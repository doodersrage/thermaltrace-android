package dev.thermaltrace.android.data.insights

import dev.thermaltrace.android.data.model.ChartPointDto
import dev.thermaltrace.android.data.model.HouseInsightDto
import kotlin.math.abs
import kotlin.math.ln

data class HeatingInsight(
    val label: String,
    val detail: String,
    val severity: Severity,
) {
    enum class Severity { Info, Warning }
}

private val heatingModes = setOf("HEAT", "HEATCOOL", "heat", "auxHeatOnly")
private val coolingModes = setOf("COOL", "cool")

private fun isThermostatHeating(mode: String?): Boolean =
    mode != null && heatingModes.contains(mode)

private fun isThermostatCooling(mode: String?): Boolean =
    mode != null && coolingModes.contains(mode)

/** Dew point °F from air temperature and relative humidity (Magnus-Tetens). */
fun dewPointF(tempF: Double, rhPct: Double): Double? {
    if (!tempF.isFinite() || !rhPct.isFinite() || rhPct <= 0.0 || rhPct > 100.0) return null
    val tC = (tempF - 32.0) * (5.0 / 9.0)
    val a = 17.62
    val b = 243.12
    val gamma = ln(rhPct / 100.0) + (a * tC) / (b + tC)
    val tdC = (b * gamma) / (a - gamma)
    if (!tdC.isFinite()) return null
    return tdC * (9.0 / 5.0) + 32.0
}

fun estimateHeatingLossRate(
    indoorPoints: List<ChartPointDto>,
    outdoorTempF: Double?,
): Double? {
    val temps = indoorPoints.filter { it.tempf.isFinite() }.takeLast(12)
    if (temps.size < 2) return null
    val first = temps.first()
    val last = temps.last()
    val hours =
        (parseMillis(last.timestamp) - parseMillis(first.timestamp)) / (60.0 * 60.0 * 1000.0)
    if (hours <= 0) return null
    val rate = (last.tempf - first.tempf) / hours
    if (!rate.isFinite()) return null
    if (outdoorTempF != null && outdoorTempF < 32.0 && rate < 0) return rate
    return rate
}

fun buildHeatingInsights(
    indoorPoints: List<ChartPointDto>,
    outdoorTempF: Double?,
    freezeThresholdF: Double,
    doorOpenMinutes: Double? = null,
    house: HouseInsightDto? = null,
): List<HeatingInsight> {
    val insights = mutableListOf<HeatingInsight>()
    val latest = indoorPoints.lastOrNull { it.tempf.isFinite() } ?: return insights

    val rate = estimateHeatingLossRate(indoorPoints, outdoorTempF)
    if (rate != null && rate <= -2.0) {
        insights += HeatingInsight(
            label = "Rapid temperature drop",
            detail = "Space is falling about ${"%.1f".format(abs(rate))}°F/h. Check doors, heaters, and insulation.",
            severity = HeatingInsight.Severity.Warning,
        )
    }

    if (
        doorOpenMinutes != null &&
        doorOpenMinutes >= 10 &&
        rate != null &&
        rate < 0
    ) {
        insights += HeatingInsight(
            label = "Door may be driving heat loss",
            detail = "Door open ~${doorOpenMinutes.toInt()} min while temperature is falling.",
            severity = HeatingInsight.Severity.Warning,
        )
    }

    if (latest.tempf <= freezeThresholdF + 5) {
        insights += HeatingInsight(
            label = "Near freeze threshold",
            detail = "Current ${"%.1f".format(latest.tempf)}°F — threshold is ${"%.0f".format(freezeThresholdF)}°F.",
            severity = if (latest.tempf <= freezeThresholdF) {
                HeatingInsight.Severity.Warning
            } else {
                HeatingInsight.Severity.Info
            },
        )
    }

    if (outdoorTempF != null && outdoorTempF <= 20 && latest.tempf > freezeThresholdF) {
        val margin = latest.tempf - freezeThresholdF
        insights += HeatingInsight(
            label = "Cold snap outside",
            detail = "Outdoor ${"%.0f".format(outdoorTempF)}°F — ${"%.0f".format(margin)}°F margin above your freeze alert.",
            severity = if (margin <= 8) HeatingInsight.Severity.Warning else HeatingInsight.Severity.Info,
        )
    }

    val dewPoint = dewPointF(latest.tempf, latest.humidity)
    if (dewPoint != null) {
        val margin = latest.tempf - dewPoint
        if (margin <= 5) {
            insights += HeatingInsight(
                label = "Condensation risk",
                detail = "Dew point ~${"%.0f".format(dewPoint)}°F — air is within ${"%.0f".format(margin.coerceAtLeast(0.0))}°F of saturating. Cold slabs and tools can sweat even if the probe is warmer.",
                severity = if (margin <= 2) HeatingInsight.Severity.Warning else HeatingInsight.Severity.Info,
            )
        }
    }

    val houseTempF = house?.ambientTempF?.takeIf { it.isFinite() }
    val hasThermostat = house?.source == "thermostat"
    val hvacMode = house?.hvacMode

    if (houseTempF != null) {
        val delta = houseTempF - latest.tempf
        if (delta >= 12) {
            val houseLabel = if (hasThermostat) "House thermostat" else "Indoor reference"
            insights += HeatingInsight(
                label = "Garage–house gap",
                detail = "$houseLabel ${"%.0f".format(houseTempF)}°F vs probe ${"%.1f".format(latest.tempf)}°F (${"%.0f".format(delta)}°F warmer inside). Freeze alerts still apply to the unconditioned probe.",
                severity = if (latest.tempf <= freezeThresholdF + 5) {
                    HeatingInsight.Severity.Warning
                } else {
                    HeatingInsight.Severity.Info
                },
            )
        }

        if (latest.tempf <= freezeThresholdF && houseTempF > freezeThresholdF + 10) {
            insights += HeatingInsight(
                label = "Warm house, cold probe",
                detail = "Probe is at or below ${"%.0f".format(freezeThresholdF)}°F while the house reads ${"%.0f".format(houseTempF)}°F — expected for an unheated garage or shop.",
                severity = HeatingInsight.Severity.Info,
            )
        }

        if (hasThermostat && isThermostatHeating(hvacMode) && latest.tempf < houseTempF - 15) {
            insights += HeatingInsight(
                label = "HVAC heating",
                detail = "Furnace is on (house ${"%.0f".format(houseTempF)}°F) but the monitored space is unconditioned — it will stay colder than living areas.",
                severity = HeatingInsight.Severity.Info,
            )
        }

        if (hasThermostat && isThermostatCooling(hvacMode) && latest.tempf > houseTempF + 10) {
            insights += HeatingInsight(
                label = "HVAC cooling",
                detail = "AC is running (house ${"%.0f".format(houseTempF)}°F). A hot garage or attic probe can still spike on sunny days.",
                severity = HeatingInsight.Severity.Info,
            )
        }
    }

    return insights
}

private fun parseMillis(iso: String): Long =
    runCatching { java.time.Instant.parse(iso).toEpochMilli() }.getOrDefault(0L)

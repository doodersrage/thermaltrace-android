package dev.thermaltrace.android.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HouseholdResponse(
    val householdId: String? = null,
    val members: List<HouseholdMemberDto> = emptyList(),
    val households: List<UserHouseholdDto> = emptyList(),
    val invites: List<HouseholdInviteDto> = emptyList(),
    val error: String? = null,
)

@Serializable
data class HouseholdMemberDto(
    val id: String? = null,
    @SerialName("household_id") val householdId: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val role: String = "member",
    @SerialName("created_at") val createdAt: String? = null,
    val email: String? = null,
)

@Serializable
data class UserHouseholdDto(
    @SerialName("household_id") val householdId: String,
    val role: String = "member",
    val name: String = "",
)

@Serializable
data class HouseholdInviteDto(
    val id: String,
    @SerialName("household_id") val householdId: String? = null,
    val email: String = "",
    val token: String? = null,
    val role: String = "viewer",
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

/** Aggregated from live readings sensors (export lacks device ids). */
data class DeviceSummary(
    val id: String,
    val name: String,
    val space: String?,
    val sensorCount: Int,
    val kinds: List<String>,
)

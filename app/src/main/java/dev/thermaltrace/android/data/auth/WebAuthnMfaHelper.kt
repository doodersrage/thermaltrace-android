package dev.thermaltrace.android.data.auth

import androidx.activity.ComponentActivity
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

class WebAuthnMfaHelper(
    private val activity: ComponentActivity,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val credentialManager = CredentialManager.create(activity)

    suspend fun getAssertion(requestJson: String): JsonObject = withContext(Dispatchers.Main) {
        val option = GetPublicKeyCredentialOption(requestJson = requestJson)
        val request = GetCredentialRequest(credentialOptions = listOf(option))
        try {
            val response: GetCredentialResponse = credentialManager.getCredential(
                context = activity,
                request = request,
            )
            val credential = response.credential
            if (credential !is PublicKeyCredential) {
                error("Unexpected credential type: ${credential::class.simpleName}")
            }
            json.parseToJsonElement(credential.authenticationResponseJson).jsonObject
        } catch (err: GetCredentialException) {
            throw IllegalStateException(err.message ?: "Security key verification failed", err)
        }
    }

    suspend fun createCredential(requestJson: String): JsonObject = withContext(Dispatchers.Main) {
        val request = CreatePublicKeyCredentialRequest(requestJson = requestJson)
        try {
            val response: CreateCredentialResponse = credentialManager.createCredential(
                context = activity,
                request = request,
            )
            if (response !is CreatePublicKeyCredentialResponse) {
                error("Unexpected registration response")
            }
            json.parseToJsonElement(response.registrationResponseJson).jsonObject
        } catch (err: CreateCredentialException) {
            throw IllegalStateException(err.message ?: "Security key registration failed", err)
        }
    }

    companion object {
        fun publicKeyRequestJson(publicKey: JsonObject): String =
            json.encodeToString(
                JsonObject.serializer(),
                buildJsonObject {
                    put("publicKey", publicKey)
                },
            )

        private val json = Json { ignoreUnknownKeys = true }
    }
}

package com.mmk.kmpauth.google

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.js.Promise
import kotlin.random.Random

public class GoogleAuthUiProviderImpl(private val credentials: GoogleAuthCredentials) : GoogleAuthUiProvider {
    private val authUrl = "https://accounts.google.com/o/oauth2/v2/auth"

    override suspend fun signIn(filterByAuthorizedAccounts: Boolean): GoogleUser? {
        val scope = "email profile" + if (credentials.scopes.isNotEmpty()) " ${credentials.scopes.joinToString(" ")}" else ""
        val redirectUri = window.location.origin // Use the app's current origin
        val state = generateRandomString()
        val nonce = generateRandomString()

        val googleAuthUrl = buildString {
            append(authUrl)
            append("?client_id=${credentials.serverId}")
            append("&redirect_uri=${encodeURIComponent(redirectUri)}")
            append("&response_type=id_token")
            append("&scope=${encodeURIComponent(scope)}")
            append("&nonce=${encodeURIComponent(nonce)}")
            append("&state=${encodeURIComponent(state)}")
        }

        openUrlInBrowser(googleAuthUrl)

        val idToken = waitForOAuthRedirect(state)
        if (idToken == null) {
            println("GoogleAuthUiProvider: idToken is null")
            return null
        }

        val jwt = decodeJwt(idToken)
        val name = jwt["name"] as? String
        val picture = jwt["picture"] as? String
        val receivedNonce = jwt["nonce"] as? String

        if (receivedNonce != nonce) {
            println("GoogleAuthUiProvider: Invalid nonce state.")
            return null
        }

        return GoogleUser(
            idToken = idToken,
            accessToken = null,
            displayName = name.orEmpty(),
            profilePicUrl = picture
        )
    }

    private suspend fun waitForOAuthRedirect(expectedState: String): String? {
        return suspendCancellableCoroutine { continuation ->
            window.addEventListener("hashchange", {
                val fragment = window.location.hash.substring(1) // Remove '#'
                val params = URLSearchParams(fragment)
                val idToken = params.get("id_token")
                val state = params.get("state")

                if (state == expectedState) {
                    continuation.resumeWith(Result.success(idToken))
                } else {
                    println("Invalid state in OAuth response.")
                    continuation.resumeWith(Result.success(null))
                }
            }) // Listen for hash change
        }
    }

    private fun openUrlInBrowser(url: String) {
        window.open(url, "_self")
    }

    private fun generateRandomString(length: Int = 32): String {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
        return (1..length)
            .map { Random.nextInt(0, allowedChars.length) }
            .map(allowedChars::get)
            .joinToString("")
    }


    public fun decodeJwt(payload: String): Map<String, Any?> {
        val decodedPayload = atob(payload) // Decode base64
        return Json.decodeFromString(decodedPayload) // Parse JSON
    }
}
@JsModule("url") // This isn't needed for direct browser access, URLSearchParams is built-in.
public external class URLSearchParams public constructor(init: String) {
    public fun get(name: String): String?
    public fun has(name: String): Boolean
    public fun forEach(callback: (value: String, key: String) -> Unit)
}
@JsName("atob")
public external fun atob(encoded: String): String

// Define a more specific return type for JSON.parse
public data class JwtPayload(
    val name: String?,
    val picture: String?,
    val nonce: String?
)
public external fun encodeURIComponent(value: String): String

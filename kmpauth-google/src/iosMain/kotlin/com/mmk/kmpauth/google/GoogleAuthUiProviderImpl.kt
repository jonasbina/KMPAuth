package com.mmk.kmpauth.google

import cocoapods.GoogleSignIn.GIDSignIn
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIApplication
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class GoogleAuthUiProviderImpl(val credentials: GoogleAuthCredentials) : GoogleAuthUiProvider {
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun signIn(filterByAuthorizedAccounts: Boolean): GoogleUser? = suspendCoroutine { continutation ->

        val rootViewController =
            UIApplication.sharedApplication.keyWindow?.rootViewController

        if (rootViewController == null) continutation.resume(null)
        else {
            GIDSignIn.sharedInstance.addScopes(credentials.scopes, rootViewController) { _, error ->
                if (error != null) {
                    println("Error adding scopes: $error")
                    continuation.resume(null)
                } else {
                    GIDSignIn.sharedInstance.signInWithPresentingViewController(rootViewController) { gidSignInResult, nsError ->
                        nsError?.let { println("Error while signing in: $nsError") }

                        val user = gidSignInResult?.user
                        val idToken = user?.idToken?.tokenString
                        val accessToken = user?.accessToken?.tokenString
                        val profile = gidSignInResult?.user?.profile
                        if (idToken != null && accessToken != null) {
                            val googleUser = GoogleUser(
                                idToken = idToken,
                                accessToken = accessToken,
                                displayName = profile?.name ?: "",
                                profilePicUrl = profile?.imageURLWithDimension(320u)?.absoluteString
                            )
                            continuation.resume(googleUser)
                        } else continuation.resume(null)
                    }
                }
            }
        }
    }


}
package com.mmk.kmpauth.google

import Scope

/**
 * Google Auth Credentials holder class.
 * @param serverId - This should be Web Client Id that you created in Google OAuth page
 */
public data class GoogleAuthCredentials(val serverId: String, val scopes:List<Scope>)

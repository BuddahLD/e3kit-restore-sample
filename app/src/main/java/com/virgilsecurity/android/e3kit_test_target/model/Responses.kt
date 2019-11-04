package com.virgilsecurity.android.e3kit_test_target.model

/**
 * Responses
 */

data class Message(val sender: String, val receiver: String, val body: String)

data class ReceiveMessageResponse(val message: Message)

data class AuthenticateResponse(val authToken: String)

data class VirgilTokenResponse(val virgilToken: String)

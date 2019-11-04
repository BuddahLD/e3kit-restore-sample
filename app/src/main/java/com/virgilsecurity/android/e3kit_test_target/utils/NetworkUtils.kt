package com.virgilsecurity.android.e3kit_test_target.utils

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.google.gson.Gson
import com.virgilsecurity.android.e3kit_test_target.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * NetworkUtils
 */
object NetworkUtils {

    fun authenticate(identity: String, coroutineScope: CoroutineScope): Deferred<String> =
        coroutineScope.async {
            val baseUrl = "http://10.0.2.2:3000/authenticate"
            val fullUrl = URL(baseUrl)

            val urlConnection = fullUrl.openConnection() as HttpURLConnection
            urlConnection.doOutput = true
            urlConnection.doInput = true
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            urlConnection.setRequestProperty("Accept", "application/json")
            urlConnection.requestMethod = "POST"

            val cred = JSONObject()

            cred.put("identity", identity)

            val wr = urlConnection.outputStream
            wr.write(cred.toString().toByteArray(charset("UTF-8")))
            wr.close()

            val httpResult = urlConnection.responseCode
            if (httpResult == HttpURLConnection.HTTP_OK) {
                val response =
                    InputStreamReader(urlConnection.inputStream, Charsets.UTF_8).buffered()
                        .use { it.readText() }

                return@async Gson().fromJson(response, AuthenticateResponse::class.java).authToken
            } else {
                throw Throwable("Some connection error")
            }
        }

    fun getVirgilJwt(authToken: String, identity: String): String =
        "http://10.0.2.2:3000/virgil-jwt?identity=$identity"
            .httpGet()
            .timeout(999999)
            .timeoutRead(999999)
//                .header("Authorization", "Bearer $authToken")
            .responseString()
            .third
            .run {
                if (this.component2() == null) {
                    val response = Gson().fromJson(
                        this.component1()!!,
                        VirgilTokenResponse::class.java
                    )

                    response.virgilToken
                } else {
                    throw IllegalStateException(this.component2()!!.exception)
                }
            }

    fun sendMessage(
        authToken: String,
        sender: String,
        recipient: String,
        body: String,
        coroutineScope: CoroutineScope
    ) =
        coroutineScope.async {
            val request = SendMessageRequest(sender, recipient, body)
            val postBody = Gson().toJson(request)

            "http://10.0.2.2:3000/sendMessage"
                .httpPost()
                .timeout(999999)
                .timeoutRead(999999)
//                .header("Authorization", "Bearer $authToken")
                .jsonBody(postBody)
                .responseString()
                .third
                .component2()
                .run {
                    if (this != null) throw IllegalStateException(this.exception)
                }
        }

    fun receiveMessage(
        authToken: String,
        sender: String,
        coroutineScope: CoroutineScope
    ): Deferred<Message> =
        coroutineScope.async {
            "http://10.0.2.2:3000/receiveMessage?sendedFor=$sender"
                .httpGet()
                .timeout(999999)
                .timeoutRead(999999)
//                .header("Authorization", "Bearer $authToken")
                .responseString()
                .third
                .run {
                    if (this.component2() == null) {
                        val response = Gson().fromJson(
                            this.component1()!!,
                            ReceiveMessageResponse::class.java
                        )

                        response.message
                    } else {
                        throw IllegalStateException(this.component2()!!.exception)
                    }
                }
        }
}

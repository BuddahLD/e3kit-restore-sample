package com.virgilsecurity.android.e3kit_test_target.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.virgilsecurity.android.e3kit_test_target.R
import com.virgilsecurity.android.e3kit_test_target.utils.EThreeHolder
import com.virgilsecurity.android.e3kit_test_target.utils.NetworkUtils
import com.virgilsecurity.android.e3kit_test_target.utils.PreferencesUtils
import kotlinx.coroutines.*

/**
 * ChatActivity
 */
class ChatActivity : AppCompatActivity() {

    private val parentJob = Job()
    private val ioCoroutineScope = CoroutineScope(Dispatchers.IO + parentJob)
    private val mainCoroutineScope = CoroutineScope(Dispatchers.Main + parentJob)
    private val sharedPreferences = PreferencesUtils.instance(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        findViewById<TextView>(R.id.tvCurrentIdentity).text =
            sharedPreferences.getNotNull(PreferencesUtils.IDENTITY)

        initButtons()
    }

    private fun initButtons() {
        findViewById<Button>(R.id.btnSend).setOnClickListener {
            val recipient = findViewById<EditText>(R.id.etReceiver).text.toString()
            val body = findViewById<EditText>(R.id.etBody).text.toString()
            if (recipient.isBlank() or body.isBlank()) {
                Toast.makeText(
                    this,
                    "receiver or message is blank",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            try {
                mainCoroutineScope.launch {
                    val card = withContext(ioCoroutineScope.coroutineContext) {
                        EThreeHolder.getEthreeNotNull().findUser(recipient).get()
                    }

                    val encryptedBody = EThreeHolder.getEthreeNotNull().encrypt(body, card)

                    NetworkUtils.sendMessage(
                        sharedPreferences.getNotNull(PreferencesUtils.AUTH_TOKEN),
                        sharedPreferences.getNotNull(PreferencesUtils.IDENTITY),
                        recipient,
                        encryptedBody,
                        ioCoroutineScope
                    ).await()
                }
            } catch (throwable: Throwable) {
                Toast.makeText(
                    this,
                    throwable.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        findViewById<Button>(R.id.btnReceive).setOnClickListener {
            try {
                val sender = findViewById<EditText>(R.id.etSender).text.toString()
                if (sender.isBlank()) {
                    Toast.makeText(
                        this,
                        "Sender should not be empty",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                mainCoroutineScope.launch {
                    val message = NetworkUtils.receiveMessage(
                        sharedPreferences.getNotNull(PreferencesUtils.AUTH_TOKEN),
                        sender,
                        ioCoroutineScope
                    ).await()

                    val card = withContext(ioCoroutineScope.coroutineContext) {
                        EThreeHolder.getEthreeNotNull().findUser(message.sender).get()
                    }

                    val decryptedBody = EThreeHolder.getEthreeNotNull().decrypt(message.body, card)

                    findViewById<TextView>(R.id.tvMessageReceived).text = decryptedBody
                }
            } catch (throwable: Throwable) {
                Toast.makeText(
                    this,
                    throwable.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        findViewById<Button>(R.id.btnSignOut).setOnClickListener {
            mainCoroutineScope.launch {
                withContext(ioCoroutineScope.coroutineContext) {
                    sharedPreferences.set(PreferencesUtils.AUTH_TOKEN, null)
                    sharedPreferences.set(PreferencesUtils.IDENTITY, null)
                    EThreeHolder.getEthreeNotNull().cleanup()
                    EThreeHolder.setEthree(null)
                }

                val intent = Intent(this@ChatActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}

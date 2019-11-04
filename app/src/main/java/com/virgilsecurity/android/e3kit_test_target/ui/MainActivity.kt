package com.virgilsecurity.android.e3kit_test_target.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.virgilsecurity.android.common.callback.OnGetTokenCallback
import com.virgilsecurity.android.e3kit_test_target.R
import com.virgilsecurity.android.e3kit_test_target.utils.EThreeHolder
import com.virgilsecurity.android.e3kit_test_target.utils.NetworkUtils
import com.virgilsecurity.android.e3kit_test_target.utils.PreferencesUtils
import com.virgilsecurity.android.ethree.interaction.EThree
import com.virgilsecurity.common.callback.OnCompleteListener
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private val parentJob = Job()
    private val ioCoroutineScope = CoroutineScope(Dispatchers.IO + parentJob)
    private val mainCoroutineScope = CoroutineScope(Dispatchers.Main + parentJob)
    private val sharedPreferences = PreferencesUtils.instance(this)

    private lateinit var pbLoading: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pbLoading = findViewById(R.id.pbLoading)

        if (sharedPreferences.get(PreferencesUtils.IDENTITY) != null) {
            initEThree()

            if (EThreeHolder.getEthreeNotNull().hasLocalPrivateKey()) {
                val intent = Intent(this@MainActivity, ChatActivity::class.java)
                startActivity(intent)
                finish()
            }
        } else {
            initEnter()
        }
    }

    private fun initEnter() {
        val btnEnter = findViewById<Button>(R.id.btnEnter)
        btnEnter.setOnClickListener {
            pbLoading.visibility = View.VISIBLE

            val identity = findViewById<EditText>(R.id.etIdentity).text.toString()
            if (identity.isBlank()) {
                Toast.makeText(
                    this,
                    "Identity should not be empty",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }

            sharedPreferences.set(PreferencesUtils.IDENTITY, identity)

            try {
                mainCoroutineScope.launch {
                    val authToken =
                        NetworkUtils.authenticate(
                            sharedPreferences.getNotNull(PreferencesUtils.IDENTITY),
                            ioCoroutineScope
                        ).await()
                    sharedPreferences.set(PreferencesUtils.AUTH_TOKEN, authToken)

                    initEThree()

                    try {
                        withContext(ioCoroutineScope.coroutineContext) {
                            EThreeHolder.getEthreeNotNull()
                                .restorePrivateKey(DONT_DO_LIKE_THIS_EVER).execute()
                        }

                        val intent = Intent(this@MainActivity, ChatActivity::class.java)
                        startActivity(intent)
                        finish()
                    } catch (throwable: Throwable) {
                        Thread.sleep(2000) // Avoid throttling from Virgil
                        registerEThree()
                    }
                }
            } catch (throwable: Throwable) {
                pbLoading.visibility = View.GONE

                Toast.makeText(
                    this,
                    throwable.message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun initEThree() {
        val ethree = EThree(
            sharedPreferences.getNotNull(PreferencesUtils.IDENTITY),
            object : OnGetTokenCallback {
                override fun onGetToken(): String {
                    return NetworkUtils.getVirgilJwt(
                        sharedPreferences.getNotNull(PreferencesUtils.AUTH_TOKEN),
                        sharedPreferences.getNotNull(PreferencesUtils.IDENTITY)
                    )
                }
            },
            this
        )

        EThreeHolder.setEthree(ethree)
    }

    private fun registerEThree() {
        mainCoroutineScope.launch {
            EThreeHolder.getEthreeNotNull().register().addCallback(object : OnCompleteListener {
                override fun onSuccess() {
                    EThreeHolder.getEthreeNotNull().backupPrivateKey(DONT_DO_LIKE_THIS_EVER)
                        .execute()

                    val intent = Intent(this@MainActivity, ChatActivity::class.java)
                    startActivity(intent)
                    finish()
                }

                override fun onError(throwable: Throwable) {
                    mainCoroutineScope.launch {
                        pbLoading.visibility = View.GONE

                        Toast.makeText(
                            this@MainActivity,
                            throwable.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }, ioCoroutineScope)
        }
    }

    companion object {
        private const val DONT_DO_LIKE_THIS_EVER = "hard_pass"
    }
}

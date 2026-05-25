package com.github.tvbox.osc.ui.compose.activities

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.github.tvbox.osc.server.ControlManager
import com.github.tvbox.osc.ui.compose.screens.PushScreen
import com.github.tvbox.osc.ui.compose.theme.TVBoxTheme

class PushComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val address = ControlManager.get().getAddress(false) ?: ""

        setContent {
            TVBoxTheme {
                PushScreen(
                    address = address,
                    onPushLocalClick = {
                        try {
                            val manager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            if (manager.hasPrimaryClip() && manager.primaryClip != null && manager.primaryClip!!.itemCount > 0) {
                                val addedText = manager.primaryClip!!.getItemAt(0).text.toString().trim()
                                val intent = Intent(this, DetailComposeActivity::class.java).apply {
                                    putExtra("id", addedText)
                                    putExtra("sourceKey", "push_agent")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                }
                                startActivity(intent)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )
            }
        }
    }
}

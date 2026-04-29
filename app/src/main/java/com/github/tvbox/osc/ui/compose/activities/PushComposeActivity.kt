package com.github.tvbox.osc.ui.compose.activities

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.github.tvbox.osc.server.ControlManager
import com.github.tvbox.osc.ui.activity.PlayActivity
import com.github.tvbox.osc.ui.compose.theme.TVBoxTheme
import com.github.tvbox.osc.ui.tv.QRCodeGen

class PushComposeActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val address = ControlManager.get().getAddress(false)
        val qrBitmap = QRCodeGen.generateBitmap(address, 300, 300, 4)

        setContent {
            TVBoxTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Push Content",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Box(
                        modifier = Modifier
                            .size(320.dp)
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        qrBitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Scan the QR code or visit this address in your browser:\n$address",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            val manager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            if (manager?.hasPrimaryClip() == true && (manager.primaryClip?.itemCount ?: 0) > 0) {
                                val addedText = manager.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
                                if (!addedText.isNullOrEmpty()) {
                                    val intent = Intent(this@PushComposeActivity, PlayActivity::class.java).apply {
                                        putExtra("id", addedText)
                                        putExtra("sourceKey", "push_agent")
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    }
                                    startActivity(intent)
                                }
                            }
                        }
                    ) {
                        Text("Play from Clipboard")
                    }
                }
            }
        }
    }
}

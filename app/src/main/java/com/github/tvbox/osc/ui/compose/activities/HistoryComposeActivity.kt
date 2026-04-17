package com.github.tvbox.osc.ui.compose.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.github.tvbox.osc.cache.RoomDataManger
import com.github.tvbox.osc.ui.compose.screens.HistoryScreen
import com.github.tvbox.osc.ui.compose.theme.TVBoxTheme
import android.content.Intent
import com.github.tvbox.osc.bean.VodInfo

class HistoryComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TVBoxTheme {
                var historyItems by remember { mutableStateOf(emptyList<VodInfo>()) }

                LaunchedEffect(Unit) {
                    historyItems = RoomDataManger.getAllVodRecord(100) ?: emptyList()
                }

                HistoryScreen(
                    historyItems = historyItems,
                    onItemClick = { vodInfo ->
                        val intent = Intent(this, DetailComposeActivity::class.java).apply {
                            putExtra("id", vodInfo.id)
                            putExtra("sourceKey", vodInfo.sourceKey)
                        }
                        startActivity(intent)
                    },
                    onClearAll = {
                        RoomDataManger.deleteVodRecordAll()
                        historyItems = emptyList()
                    }
                )
            }
        }
    }
}

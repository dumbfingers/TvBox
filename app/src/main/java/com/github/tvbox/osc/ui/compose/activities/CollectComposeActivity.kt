package com.github.tvbox.osc.ui.compose.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.github.tvbox.osc.cache.RoomDataManger
import com.github.tvbox.osc.ui.compose.screens.CollectScreen
import com.github.tvbox.osc.ui.compose.theme.TVBoxTheme
import android.content.Intent
import com.github.tvbox.osc.cache.VodCollect

class CollectComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TVBoxTheme {
                var collectItems by remember { mutableStateOf(emptyList<VodCollect>()) }

                LaunchedEffect(Unit) {
                    collectItems = RoomDataManger.getAllVodCollect() ?: emptyList()
                }

                CollectScreen(
                    collectItems = collectItems,
                    onItemClick = { vodCollect ->
                        val intent = Intent(this, DetailComposeActivity::class.java).apply {
                            putExtra("id", vodCollect.vodId)
                            putExtra("sourceKey", vodCollect.sourceKey)
                        }
                        startActivity(intent)
                    },
                    onClearAll = {
                        RoomDataManger.deleteVodCollectAll()
                        collectItems = emptyList()
                    }
                )
            }
        }
    }
}

package com.github.tvbox.osc.ui.compose.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.github.tvbox.osc.player.MyVideoView
import com.github.tvbox.osc.player.controller.VodController

@Composable
fun PlayScreen(
    videoView: MyVideoView,
    controller: VodController
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                FrameLayout(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    (videoView.parent as? ViewGroup)?.removeView(videoView)
                    addView(videoView)
                }
            },
            update = { _ ->
            }
        )
    }
}

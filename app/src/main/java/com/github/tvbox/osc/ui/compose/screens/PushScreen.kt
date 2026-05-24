package com.github.tvbox.osc.ui.compose.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.github.tvbox.osc.ui.tv.QRCodeGen

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PushScreen(
    address: String,
    onPushLocalClick: () -> Unit
) {
    val density = LocalDensity.current

    // Convert 300dp to pixels for QR code generation
    val qrSizePx = with(density) { 300.dp.roundToPx() }

    val qrBitmap = remember(address) {
        QRCodeGen.generateBitmap(address, qrSizePx, qrSizePx, 4)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        qrBitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "QR Code",
                modifier = Modifier.size(300.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "手机/电脑扫描上方二维码或者直接浏览器访问地址\n$address",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = onPushLocalClick) {
            Text(text = "推送剪贴板内容")
        }
    }
}

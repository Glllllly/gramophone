package com.example.myapplication

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class QuoteSettingsActivity : ComponentActivity() {
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                persistReadPermission(uri)
                QuoteWidgetPrefs.setImageUri(this, uri)
                updateWidgets()
            }
        }

    private val pickTextLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                persistReadPermission(uri)
                QuoteWidgetPrefs.setQuotesUri(this, uri)
                updateWidgets()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuoteSettingsScreen(
                onOpenWidgetHelp = { openWidgetHelp() },
                onPickImage = { pickImageLauncher.launch(arrayOf("image/*")) },
                onPickText = { pickTextLauncher.launch(arrayOf("text/plain", "text/*")) },
                onReset = {
                    QuoteWidgetPrefs.clearAll(this)
                    updateWidgets()
                }
            )
        }
    }

    private fun persistReadPermission(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        try {
            contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: SecurityException) {
            // Some providers may not allow persistable permissions; ignore.
        }
    }

    private fun updateWidgets() {
        val manager = AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(ComponentName(this, QuoteWidgetProvider::class.java))
        val intent = Intent(this, QuoteWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        sendBroadcast(intent)
    }

    private fun openWidgetHelp() {
        Toast.makeText(
            this,
            "请长按桌面空白处 → 小组件 → 选择本应用",
            Toast.LENGTH_LONG
        ).show()
        val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (_: Exception) {
                // ignore
            }
        }
    }
}

@Composable
private fun QuoteSettingsScreen(
    onOpenWidgetHelp: () -> Unit,
    onPickImage: () -> Unit,
    onPickText: () -> Unit,
    onReset: () -> Unit
) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFFFF7E8)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 36.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = "小组件设置",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF1F1F1F)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onOpenWidgetHelp,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFB934C))
                ) {
                    Text(text = "一键添加小组件")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onPickImage,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFB934C))
                ) {
                    Text(text = "更换背景图片")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onPickText,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFB934C))
                ) {
                    Text(text = "更换语料库(txt)")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onReset,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFB934C))
                ) {
                    Text(text = "恢复默认")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "选择后会自动刷新小组件。",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B6B6B)
                )
            }
        }
    }
}

package com.example.myapplication

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.view.View
import android.widget.RemoteViews
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.random.Random

class QuoteWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateWidgets(context, appWidgetManager, appWidgetIds, quote = "")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_SHOW_QUOTE -> {
                val quote = pickRandomQuote(context)
                updateAllWidgets(context, quote)
                scheduleClear(context)
            }
            ACTION_CLEAR_QUOTE -> {
                updateAllWidgets(context, quote = "")
            }
        }
    }

    private fun updateAllWidgets(context: Context, quote: String) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, QuoteWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(component)
        updateWidgets(context, appWidgetManager, appWidgetIds, quote)
    }

    private fun updateWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        quote: String
    ) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_quote)
            applyImage(context, views)
            views.setTextViewText(R.id.widget_quote_text, quote)
            views.setViewVisibility(
                R.id.widget_quote_text,
                if (quote.isBlank()) View.GONE else View.VISIBLE
            )
            views.setOnClickPendingIntent(
                R.id.widget_image,
                getShowQuotePendingIntent(context)
            )
            views.setOnClickPendingIntent(
                R.id.widget_more,
                getOpenSettingsPendingIntent(context)
            )
            if (quote.isBlank()) {
                views.setOnClickPendingIntent(
                    R.id.widget_quote_text,
                    getShowQuotePendingIntent(context)
                )
            } else {
                views.setOnClickPendingIntent(
                    R.id.widget_quote_text,
                    getOpenQuotePendingIntent(context, quote)
                )
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun pickRandomQuote(context: Context): String {
        val quotes = mutableListOf<String>()
        val uri = QuoteWidgetPrefs.getQuotesUri(context)
        val inputStream = if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }

        if (inputStream != null) {
            inputStream.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).useLines { lines ->
                    lines.map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .forEach { quotes.add(it) }
                }
            }
        } else {
            context.assets.open("quotes.txt").use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).useLines { lines ->
                    lines.map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .forEach { quotes.add(it) }
                }
            }
        }

        return if (quotes.isEmpty()) {
            "今天也要好好吃饭。"
        } else {
            quotes[Random.nextInt(quotes.size)]
        }
    }

    private fun scheduleClear(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = getClearQuotePendingIntent(context)
        alarmManager.cancel(pendingIntent)
        val triggerAt = System.currentTimeMillis() + CLEAR_DELAY_MS
        // Use inexact windowed alarm to avoid exact-alarm permission on Android 12+
        alarmManager.setWindow(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            CLEAR_WINDOW_MS,
            pendingIntent
        )
    }

    private fun getShowQuotePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, QuoteWidgetProvider::class.java).apply {
            action = ACTION_SHOW_QUOTE
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_SHOW,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getClearQuotePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, QuoteWidgetProvider::class.java).apply {
            action = ACTION_CLEAR_QUOTE
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_CLEAR,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getOpenQuotePendingIntent(context: Context, quote: String): PendingIntent {
        val intent = Intent(context, QuoteDetailActivity::class.java).apply {
            putExtra(QuoteDetailActivity.EXTRA_QUOTE, quote)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getOpenSettingsPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, QuoteSettingsActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_CODE_SETTINGS,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun applyImage(context: Context, views: RemoteViews) {
        val uri = QuoteWidgetPrefs.getImageUri(context)
        if (uri == null) {
            views.setImageViewResource(R.id.widget_image, R.drawable.pic)
            return
        }

        val bitmap = try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(input, null, bounds)
                val maxSize = 512
                val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxSize)

                context.contentResolver.openInputStream(uri)?.use { input2 ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                    BitmapFactory.decodeStream(input2, null, opts)
                }
            }
        } catch (_: Exception) {
            null
        }

        if (bitmap != null) {
            views.setImageViewBitmap(R.id.widget_image, bitmap)
        } else {
            views.setImageViewResource(R.id.widget_image, R.drawable.pic)
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxSize: Int): Int {
        var inSampleSize = 1
        var w = width
        var h = height
        while (w > maxSize || h > maxSize) {
            w /= 2
            h /= 2
            inSampleSize *= 2
        }
        return inSampleSize.coerceAtLeast(1)
    }

    companion object {
        private const val ACTION_SHOW_QUOTE = "com.example.myapplication.action.SHOW_QUOTE"
        private const val ACTION_CLEAR_QUOTE = "com.example.myapplication.action.CLEAR_QUOTE"
        private const val REQUEST_CODE_SHOW = 1001
        private const val REQUEST_CODE_CLEAR = 1002
        private const val REQUEST_CODE_OPEN = 1003
        private const val REQUEST_CODE_SETTINGS = 1004
        private const val CLEAR_DELAY_MS = 3000L
        private const val CLEAR_WINDOW_MS = 2000L
    }
}

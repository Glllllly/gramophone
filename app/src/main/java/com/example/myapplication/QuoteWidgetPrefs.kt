package com.example.myapplication

import android.content.Context
import android.net.Uri

object QuoteWidgetPrefs {
    private const val PREFS_NAME = "quote_widget_prefs"
    private const val KEY_IMAGE_URI = "image_uri"
    private const val KEY_QUOTES_URI = "quotes_uri"

    fun setImageUri(context: Context, uri: Uri?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_IMAGE_URI, uri?.toString())
            .apply()
    }

    fun getImageUri(context: Context): Uri? {
        val value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_IMAGE_URI, null)
        return value?.let { Uri.parse(it) }
    }

    fun setQuotesUri(context: Context, uri: Uri?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_QUOTES_URI, uri?.toString())
            .apply()
    }

    fun getQuotesUri(context: Context): Uri? {
        val value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_QUOTES_URI, null)
        return value?.let { Uri.parse(it) }
    }

    fun clearAll(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}

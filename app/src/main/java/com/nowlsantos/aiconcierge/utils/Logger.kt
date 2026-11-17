package com.nowlsantos.aiconcierge.utils

import android.util.Log

object Logger {
    private const val DEFAULT_TAG = "AppLog"

    private fun getTag(): String {
        val stackTrace = Throwable().stackTrace
        val element = stackTrace.firstOrNull { 
            !it.className.contains("Logger") && 
            !it.className.contains("android.util.Log") 
        } ?: return DEFAULT_TAG
        return element.className.substringAfterLast('.').substringBefore('$')
    }

    fun d(message: String, tag: String? = null) {
        Log.d(tag ?: getTag(), message)
    }

    fun i(message: String, tag: String? = null) {
        Log.i(tag ?: getTag(), message)
    }

    fun w(message: String, tag: String? = null) {
        Log.w(tag ?: getTag(), message)
    }

    fun e(message: String, throwable: Throwable? = null, tag: String? = null) {
        Log.e(tag ?: getTag(), message, throwable)
    }
}
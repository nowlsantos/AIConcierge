package com.nowlsantos.aiconcierge.utils

import com.nowlsantos.aiconcierge.R

val drawableMap = mapOf(
    "background_main" to R.drawable.main_background,
    "language" to R.drawable.top_language_icon,
    "my_orders" to R.drawable.top_myorders_icon,
    "message" to R.drawable.top_message_icon,
    "penchat" to R.drawable.top_penchat_icon
)

fun getDrawableId(drawableName: String): Int? {
    return drawableMap[drawableName]
}
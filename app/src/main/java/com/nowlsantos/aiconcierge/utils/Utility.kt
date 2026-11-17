package com.nowlsantos.aiconcierge.utils

import java.util.Locale

fun String.capitalizeFirstLetter(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

fun String.capitalizeEachWord(): String {
    if (this.isEmpty()) return this

    return this.split(" ") // Split the sentence into individual words
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            }
        }
}
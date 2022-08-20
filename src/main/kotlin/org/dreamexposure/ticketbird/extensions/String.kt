package org.dreamexposure.ticketbird.extensions

import java.util.*

fun String.handleLocaleDebt(): Locale {
    return when (this) {
        "ENGLISH" -> Locale.ENGLISH
        "DANISH" -> Locale.forLanguageTag("da-DK")
        "PORTUGUESE" -> Locale.forLanguageTag("pt-PT")
        "RUSSIAN" -> Locale.forLanguageTag("ru-RU")
        else -> Locale.forLanguageTag(this)
    }
}

fun String.embedTitleSafe(): String = this.substring(0, (256).coerceAtMost(this.length))

fun String.embedDescriptionSafe(): String = this.substring(0, (4096).coerceAtMost(this.length))

fun String.embedFieldSafe(): String = this.substring(0, (1024).coerceAtMost(this.length))

fun String.messageContentSafe(): String = this.substring(0, (2000).coerceAtMost(this.length))

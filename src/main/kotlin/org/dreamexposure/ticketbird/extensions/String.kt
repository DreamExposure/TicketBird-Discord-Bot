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

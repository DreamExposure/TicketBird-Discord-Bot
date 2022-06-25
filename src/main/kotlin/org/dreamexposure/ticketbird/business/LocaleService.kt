package org.dreamexposure.ticketbird.business

import java.util.*

interface LocaleService {

    fun getString(locale: Locale, field: String, vararg values: String): String
}

package org.dreamexposure.ticketbird.business

import org.dreamexposure.ticketbird.logger.LOGGER
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*

@Component
class DefaultLocaleService: LocaleService {
    private val source: ResourceBundleMessageSource

    init {
        val src = ResourceBundleMessageSource()
        src.setBasename("locale/values")
        src.setFallbackToSystemLocale(false)
        src.setDefaultEncoding(StandardCharsets.UTF_8.name())
        source = src
    }

    override fun getString(locale: Locale, field: String, vararg values: String): String = try {
        source.getMessage(field, values, locale)
    } catch (e: Exception) {
        LOGGER.error("Locale get string error | $locale | $field | $values", e)
        field
    }
}

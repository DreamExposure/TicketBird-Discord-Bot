package org.dreamexposure.ticketbird.business

import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.channel.Category
import discord4j.core.`object`.entity.channel.TextChannel

interface EnvironmentService {
    suspend fun createCategory(guildId: Snowflake, type: String): Category

    suspend fun createSupportChannel(guildId: Snowflake): TextChannel


    /**
     * Returns true if everything exists correctly, otherwise false
     * @return True if everything exists correctly, otherwise false
     */
    suspend fun validateAllEntitiesExist(guildId: Snowflake): Boolean

    suspend fun recreateMissingEntities(guildId: Snowflake)
}

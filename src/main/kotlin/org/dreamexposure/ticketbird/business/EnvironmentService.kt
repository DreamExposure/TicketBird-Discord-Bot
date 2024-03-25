package org.dreamexposure.ticketbird.business

import com.fasterxml.jackson.databind.ObjectMapper
import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.PermissionOverwrite
import discord4j.core.`object`.entity.channel.Category
import discord4j.core.`object`.entity.channel.TextChannel
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.rest.http.client.ClientException
import discord4j.rest.util.Permission
import discord4j.rest.util.PermissionSet
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.logger.LOGGER
import org.dreamexposure.ticketbird.utils.GlobalVars
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.getBean
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class EnvironmentService(
    private val beanFactory: BeanFactory,
    private val settingsService: GuildSettingsService,
    private val permissionService: PermissionService,
    private val localeService: LocaleService,
    private val componentService: ComponentService,
    private val embedService: EmbedService,
    objectMapper: ObjectMapper,
) {
    private val discordClient
        get() = beanFactory.getBean<GatewayDiscordClient>()

    private final val premiumCommands: List<ApplicationCommandRequest>
    private final val devCommands: List<ApplicationCommandRequest>

    init {
        val matcher = PathMatchingResourcePatternResolver()

        // TODO: Uncomment if I add a premium command
        this.premiumCommands = emptyList()
        // Get premium commands
        //val premiumCommands = mutableListOf<ApplicationCommandRequest>()
        //for (res in matcher.getResources("commands/premium/*.json")) {
        //    val request = objectMapper.readValue<ApplicationCommandRequest>(res.inputStream)
        //    premiumCommands.add(request)
        //}
        //this.premiumCommands = premiumCommands

        // TODO: Uncomment if I add a dev command
        this.devCommands = emptyList()
        // Get dev commands
        //val devCommands = mutableListOf<ApplicationCommandRequest>()
        //for (res in matcher.getResources("commands/dev/*.json")) {
        //    val request = objectMapper.readValue<ApplicationCommandRequest>(res.inputStream)
        //    premiumCommands.add(request)
        //}
        //this.devCommands = devCommands
    }

    suspend fun createCategory(guildId: Snowflake, type: String): Category {
        LOGGER.debug("Creating category type {} for guild: {}", type, guildId)

        val settings = settingsService.getGuildSettings(guildId)
        val guild = discordClient.getGuildById(guildId).awaitSingle()

        return guild.createCategory(localeService.getString(settings.locale, "env.category.$type.name"))
            .withPermissionOverwrites(PermissionOverwrite.forMember(
                discordClient.selfId,
                permissionService.getBotPermissionOverrides(),
                PermissionSet.none()
            )).awaitSingle()
    }

    suspend fun createSupportChannel(guildId: Snowflake): TextChannel {
        LOGGER.debug("Creating support channel for guild {}", guildId)

        val settings = settingsService.getGuildSettings(guildId)
        val guild = discordClient.getGuildById(guildId).awaitSingle()

        return guild.createTextChannel(localeService.getString(settings.locale, "env.channel.support.name"))
            .withPermissionOverwrites(
                PermissionOverwrite.forMember(
                    discordClient.selfId,
                    permissionService.getBotPermissionOverrides(),
                    PermissionSet.none()
                ),
                PermissionOverwrite.forRole(
                    guild.id,
                    permissionService.getSupportChannelMemberOverrides(),
                    PermissionSet.of(Permission.SEND_MESSAGES) // Do not allow sending messages in support channel, use interactions
                )
            ).withTopic(localeService.getString(settings.locale, "env.channel.support.topic"))
            .awaitSingle()
    }

    suspend fun validateAllEntitiesExist(guildId: Snowflake): Boolean {
        LOGGER.debug("Validating entities for {}...", guildId)

        val settings = settingsService.getGuildSettings(guildId)
        val guild = discordClient.getGuildById(guildId).awaitSingle()

        var properSetup = settings.hasRequiredIdsSet() && !settings.requiresRepair

        // Check awaiting response ticket category
        if (settings.awaitingCategory != null) {
            guild.getChannelById(settings.awaitingCategory!!)
                .doOnError(ClientException.isStatusCode(404)) { settings.awaitingCategory = null }
                .doOnError(ClientException.isStatusCode(404, 403)) { settings.requiresRepair = true; properSetup = false }
                .onErrorResume { Mono.empty() }
                .awaitSingleOrNull()
        }

        // Check responded ticket category
        if (settings.respondedCategory != null) {
            guild.getChannelById(settings.respondedCategory!!)
                .doOnError(ClientException.isStatusCode(404)) { settings.respondedCategory = null }
                .doOnError(ClientException.isStatusCode(404, 403)) { settings.requiresRepair = true; properSetup = false }
                .onErrorResume { Mono.empty() }
                .awaitSingleOrNull()
        }

        // Check held ticket category
        if (settings.holdCategory != null) {
            guild.getChannelById(settings.holdCategory!!)
                .doOnError(ClientException.isStatusCode(404)) { settings.holdCategory = null }
                .doOnError(ClientException.isStatusCode(404, 403)) { settings.requiresRepair = true; properSetup = false }
                .onErrorResume { Mono.empty() }
                .awaitSingleOrNull()
        }

        // Check closed ticket category
        if (settings.closeCategory != null) {
            guild.getChannelById(settings.closeCategory!!)
                .doOnError(ClientException.isStatusCode(404)) { settings.closeCategory = null }
                .doOnError(ClientException.isStatusCode(404, 403)) { settings.requiresRepair = true; properSetup = false }
                .onErrorResume { Mono.empty() }
                .awaitSingleOrNull()
        }

        // Check support channel
        if (settings.supportChannel != null) {
            guild.getChannelById(settings.supportChannel!!)
                .doOnError(ClientException.isStatusCode(404)) { settings.supportChannel = null }
                .doOnError(ClientException.isStatusCode(404, 403)) { settings.requiresRepair = true; properSetup = false }
                .onErrorResume { Mono.empty() }
                .awaitSingleOrNull()
        }

        // Check static message
        if (settings.supportChannel != null && settings.staticMessage != null) {
            guild.getChannelById(settings.supportChannel!!)
                .ofType(TextChannel::class.java)
                .flatMap { it.getMessageById(settings.staticMessage) }
                .doOnError(ClientException.isStatusCode(404)) { settings.staticMessage = null }
                .doOnError(ClientException.isStatusCode(404, 403)) { settings.requiresRepair = true; properSetup = false }
                .onErrorResume { Mono.empty() }
                .awaitSingleOrNull()
        }

        LOGGER.debug("Validated guild {} | Results - needs repair {}, proper setup {}", guildId, settings.requiresRepair, properSetup)

        settingsService.upsertGuildSettings(settings)
        return properSetup
    }

    suspend fun recreateMissingEntities(guildId: Snowflake) {
        LOGGER.debug("Recreating missing entities for guild {}", guildId)

        val settings = settingsService.getGuildSettings(guildId)

        if (settings.awaitingCategory == null) settings.awaitingCategory = createCategory(guildId, "awaiting").id
        if (settings.respondedCategory == null) settings.respondedCategory = createCategory(guildId, "responded").id
        if (settings.holdCategory == null) settings.holdCategory = createCategory(guildId, "hold").id
        if (settings.closeCategory == null) settings.closeCategory = createCategory(guildId, "closed").id

        if (settings.supportChannel == null) {
            val supportChannel = createSupportChannel(guildId)
            settings.supportChannel = supportChannel.id

            // Create new static message since the old one is lost now
            val embed = embedService.getSupportRequestMessageEmbed(settings) ?: throw IllegalStateException("Failed to get embed during recreate")
            supportChannel.createMessage(embed)
                .withComponents(*componentService.getStaticMessageComponents(settings))
                .doOnNext { settings.staticMessage = it.id }
                .awaitSingle()
        }

        // Just create new static message
        if (settings.staticMessage == null) {
            // Support channel exists, but not static message, just recreate
            val supportChannel = discordClient.getChannelById(settings.supportChannel!!)
                .ofType(TextChannel::class.java)
                .awaitSingle()

            val embed = embedService.getSupportRequestMessageEmbed(settings) ?: throw IllegalStateException("Failed to get embed during recreate")
            supportChannel.createMessage(embed)
                .withComponents(*componentService.getStaticMessageComponents(settings))
                .doOnNext { settings.staticMessage = it.id }
                .awaitSingle()
        }

        if (settings.hasRequiredIdsSet()) settings.requiresRepair = false
        settingsService.upsertGuildSettings(settings)
    }

    suspend fun validateChannelForLogging(guildId: Snowflake, channelId: Snowflake): Boolean {
        LOGGER.debug("Validating channel for logging for guild {} | channelId: {}", guildId, channelId)

        return discordClient.getChannelById(channelId)
            .ofType(TextChannel::class.java)
            .onErrorResume(ClientException.isStatusCode(404, 403)) { Mono.empty() }
            .awaitSingleOrNull() != null
    }

    suspend fun registerGuildCommands(guildId: Snowflake) {
        val settings = settingsService.getGuildSettings(guildId)
        val appService = discordClient.rest().applicationService
        val appId = discordClient.selfId.asLong()

        val commands = mutableListOf<ApplicationCommandRequest>()
        if (settings.patronGuild) commands.addAll(premiumCommands)
        if (settings.devGuild) commands.addAll(devCommands)

        if (commands.isNotEmpty()) {
            appService.bulkOverwriteGuildApplicationCommand(appId, guildId.asLong(), commands)
                .doOnNext { LOGGER.debug("Bulk guild overwrite read: {} | {}", it.name(), guildId) }
                .doOnError { LOGGER.error(GlobalVars.DEFAULT, "Bulk guild overwrite failed | $guildId", it) }
                .then()
                .awaitSingleOrNull()
        }
    }
}

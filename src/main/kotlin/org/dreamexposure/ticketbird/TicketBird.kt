package org.dreamexposure.ticketbird

import org.dreamexposure.ticketbird.conf.BotSettings
import org.dreamexposure.ticketbird.logger.LOGGER
import org.dreamexposure.ticketbird.message.MessageManager
import org.dreamexposure.ticketbird.utils.GlobalVars
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration
import org.springframework.stereotype.Component
import java.io.FileReader
import java.util.*

@Component
@SpringBootApplication(exclude = [SessionAutoConfiguration::class])
class TicketBird {
    companion object {
        @JvmStatic
        fun getShardIndex(): Int {
            /*
            This fucking sucks. So k8s doesn't expose the pod ordinal for a pod in a stateful set
            https://github.com/kubernetes/kubernetes/pull/68719

            This has been an open issue and PR for over 3 years now, and has gone stale as of March 3rd 2021.

            So, in order to get the pod ordinal since its not directly exposed, we have to get the hostname, and parse
            the ordinal out of that.

            To make sure we don't use this when running anywhere but inside of k8s, we are mapping the hostname to an env
            variable SHARD_POD_NAME and if that is present, parsing it for the pod ordinal to tell the bot its shard index.

            This will be removed when/if they add this feature directly and SHARD_INDEX will be an env variable...
            */

            //Check if we are running in k8s or not...
            val shardPodName = System.getenv("SHARD_POD_NAME")
            return if (shardPodName != null) {
                //In k8s, parse this shit

                //Pod name would look like `ticketbird-dev-N` where N is the ordinal and therefore shard index.
                val parts = shardPodName.split("-").toTypedArray()
                parts[parts.size - 1].toInt() //Get last part in name, that should be ordinal
            } else {
                //Fall back to config value
                BotSettings.SHARD_INDEX.get().toInt()
            }
        }

        @JvmStatic
        fun getShardCount(): Int {
            val shardCount = System.getenv("SHARD_COUNT")
            return shardCount?.toInt() ?: //Fall back to config
            BotSettings.SHARD_COUNT.get().toInt()
        }

        @JvmStatic
        fun main(args: Array<String>) {
            //Get settings
            val p = Properties()
            p.load(FileReader("application.properties"))
            BotSettings.init(p)

            //Start spring
            try {
                val app = SpringApplication(TicketBird::class.java)
                app.setAdditionalProfiles(BotSettings.PROFILE.get())
                app.run(*args)
            } catch (e: Exception) {
                e.printStackTrace()
                LOGGER.error(GlobalVars.DEFAULT, "Spring error!", e)
            }

            //Load language files.
            MessageManager.reloadLangs()
        }
    }
}

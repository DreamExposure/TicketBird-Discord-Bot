package org.dreamexposure.ticketbird

import org.dreamexposure.ticketbird.config.Config
import org.dreamexposure.ticketbird.logger.LOGGER
import org.dreamexposure.ticketbird.utils.GlobalVars.DEFAULT
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.stereotype.Component
import java.lang.management.ManagementFactory
import java.time.Duration

@Component
@SpringBootApplication(exclude = [SessionAutoConfiguration::class])
class TicketBird {
    companion object {
        fun getShardIndex(): Int {
            /*
            This sucks. So k8s doesn't expose the pod ordinal for a pod in a stateful set
            https://github.com/kubernetes/kubernetes/pull/68719

            This has been an open issue and PR for over 3 years now, and has gone stale as of March 3rd 2021.

            So, in order to get the pod ordinal since it's not directly exposed, we have to get the hostname, and parse
            the ordinal out of that.

            To make sure we don't use this when running anywhere but inside k8s, we are mapping the hostname to an env
            variable SHARD_POD_NAME and if that is present, parsing it for the pod ordinal to tell the bot its shard index.

            This will be removed when/if they add this feature directly and SHARD_INDEX will be an env variable...
            */

            //Check if we are running in k8s or not...
            val shardPodName = System.getenv("SHARD_POD_NAME")
            return if (shardPodName != null) {
                //In k8s, parse this

                //Pod name would look like `ticketbird-dev-N` where N is the ordinal and therefore shard index.
                val parts = shardPodName.split("-").toTypedArray()
                parts[parts.size - 1].toInt() //Get last part in name, that should be ordinal
            } else {
                //Fall back to config value
                Config.SHARD_INDEX.getInt()
            }
        }

        fun getShardCount(): Int {
            val shardCount = System.getenv("SHARD_COUNT")
            return shardCount?.toInt() ?: //Fall back to config
            Config.SHARD_COUNT.getInt()
        }

        fun getUptime(): Duration {
            val mxBean = ManagementFactory.getRuntimeMXBean()

            val rawDuration = System.currentTimeMillis() - mxBean.startTime
            return Duration.ofMillis(rawDuration)
        }

        @JvmStatic
        fun main(args: Array<String>) {
            Config.init()

            //Start spring
            try {
                SpringApplicationBuilder(TicketBird::class.java).run(*args)
            } catch (e: Exception) {
                e.printStackTrace()
                LOGGER.error(DEFAULT, "Spring error!", e)
            }
        }
    }
}

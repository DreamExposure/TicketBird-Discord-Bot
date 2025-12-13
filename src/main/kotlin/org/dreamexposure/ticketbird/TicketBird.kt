package org.dreamexposure.ticketbird

import org.dreamexposure.ticketbird.config.Config
import org.dreamexposure.ticketbird.logger.LOGGER
import org.dreamexposure.ticketbird.utils.GlobalVars
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
        @JvmStatic
        fun main(args: Array<String>) {
            Config.init()

            //Start spring
            try {
                SpringApplicationBuilder(TicketBird::class.java).run(*args)
            } catch (e: Exception) {
                e.printStackTrace()
                LOGGER.error(GlobalVars.DEFAULT, "Spring error!", e)
            }
        }

        fun getShardIndex(): Int {
            val k8sPodIndex = System.getenv("KUBERNETES_POD_INDEX")
            return k8sPodIndex?.toInt() ?: // Fall back to config
            Config.SHARD_INDEX.getInt()
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
    }
}

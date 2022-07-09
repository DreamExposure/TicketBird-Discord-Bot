package org.dreamexposure.ticketbird.business

import org.dreamexposure.ticketbird.`object`.TicketCreateState
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class TicketCreateStateService: StateService<TicketCreateState> {
    private val map = ConcurrentHashMap<String, TicketCreateState>()

    override suspend fun put(key: String, item: TicketCreateState) {
        map[key] = item
    }

    override suspend fun get(key: String): TicketCreateState? = map[key]
}

package org.dreamexposure.ticketbird.business.state

interface StateService<T> {

    suspend fun put(key: String, item: T)

    suspend fun get(key: String): T?
}

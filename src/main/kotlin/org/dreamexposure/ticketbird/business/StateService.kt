package org.dreamexposure.ticketbird.business

interface StateService<T> {

    suspend fun put(key: String, item: T)

    suspend fun get(key: String): T?
}

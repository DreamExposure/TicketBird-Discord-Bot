package org.dreamexposure.ticketbird.database

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface ProjectRepository: R2dbcRepository<ProjectData, Long> {
    fun findByGuildId(guildId: Long): Flux<ProjectData>

    fun deleteAllByGuildId(guildId: Long): Mono<Void>

    fun deleteByGuildIdAndProjectName(guildId: Long, projectName: String): Mono<Void>

    @Query("""
        UPDATE projects
        SET project_prefix = :prefix,
            project_name = :name
        WHERE guild_id = :guildId
            AND id = :id
    """)
    fun updateByIdAndGuildId(
        id: Long,
        guildId: Long,
        prefix: String,
        name: String,
    ): Mono<Int>
}

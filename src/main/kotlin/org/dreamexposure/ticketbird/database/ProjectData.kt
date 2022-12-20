package org.dreamexposure.ticketbird.database

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("projects")
data class ProjectData(
    @Id
    val id: Long? = null,
    val guildId: Long,
    val projectName: String,
    val projectPrefix: String,
    val staffUsers: String?,
    val staffRoles: String?,
)

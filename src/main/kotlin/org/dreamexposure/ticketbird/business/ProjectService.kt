package org.dreamexposure.ticketbird.business

import discord4j.common.util.Snowflake
import org.dreamexposure.ticketbird.`object`.Project

interface ProjectService {

    suspend fun getProject(guildId: Snowflake, id: Long): Project?
    suspend fun getProject(guildId: Snowflake, name: String): Project?

    suspend fun getAllProjects(guildId: Snowflake): List<Project>

    suspend fun createProject(project: Project): Project

    suspend fun updateProject(project: Project)

    suspend fun deleteProject(guildId: Snowflake, name: String)

    suspend fun deleteProject(guildId: Snowflake, id: Long)

    suspend fun deleteAllProjects(guildId: Snowflake)
}

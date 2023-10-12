package org.dreamexposure.ticketbird.business

import discord4j.common.util.Snowflake
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.ProjectCache
import org.dreamexposure.ticketbird.database.ProjectData
import org.dreamexposure.ticketbird.database.ProjectRepository
import org.dreamexposure.ticketbird.logger.LOGGER
import org.dreamexposure.ticketbird.`object`.Project
import org.springframework.stereotype.Component

@Component
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val projectCache: ProjectCache,
) {

    suspend fun getProject(guildId: Snowflake, id: Long): Project? {
        return getAllProjects(guildId).firstOrNull {it.id == id }
    }

    suspend fun getAllProjects(guildId: Snowflake): List<Project> {
        var projects = projectCache.get(key = guildId)?.toList()
        if (projects != null) return projects

        projects = projectRepository.findByGuildId(guildId.asLong())
            .map(::Project)
            .collectList()
            .awaitSingle()

        projectCache.put(key = guildId, value = projects.toTypedArray())
        return projects
    }

    suspend fun createProject(project: Project): Project {
        LOGGER.debug("Create new project | guildId {}", project.guildId)
        val newProject =  projectRepository.save(ProjectData(
            guildId = project.guildId.asLong(),
            projectName = project.name,
            projectPrefix = project.prefix,
            staffUsers = project.staffUsers.map(Snowflake::asLong).joinToString(","),
            staffRoles = project.staffRoles.map(Snowflake::asLong).joinToString(","),
            pingOverride = project.pingOverride.value,
        )).map(::Project).awaitSingle()

        val cached = projectCache.get(key = project.guildId)
        if (cached != null) projectCache.put(key = project.guildId, value = cached + newProject)

        return project
    }

    suspend fun updateProject(project: Project) {
        LOGGER.debug("Update project | guildId {} | projectId {}", project.guildId, project.id)
        projectRepository.updateByIdAndGuildId(
            id = project.id,
            guildId = project.guildId.asLong(),
            name = project.name,
            prefix = project.prefix,
            staffUsers = project.staffUsers.map(Snowflake::asLong).joinToString(","),
            staffRoles = project.staffRoles.map(Snowflake::asLong).joinToString(","),
            pingOverride = project.pingOverride.value,
        ).awaitSingleOrNull()

        val cached = projectCache.get(key = project.guildId)
        if (cached != null) {
            val newList = cached.toMutableList()
            newList.removeIf { it.id == project.id }
            projectCache.put(key = project.guildId, value = (newList + project).toTypedArray())
        }
    }

    suspend fun deleteProject(guildId: Snowflake, id: Long) {
        LOGGER.debug("Delete project | guildId {} | projectId {}", guildId, id)

        projectRepository.deleteById(id).awaitSingleOrNull()

        val cached = projectCache.get(key = guildId)
        if (cached != null) {
            val newList = cached.toMutableList()
            newList.removeIf { it.id == id }
            projectCache.put(key = guildId, value = newList.toTypedArray())
        }
    }

    suspend fun deleteAllProjects(guildId: Snowflake) {
        LOGGER.debug("Delete all projects | guildId {}", guildId)

        projectRepository.deleteAllByGuildId(guildId.asLong()).awaitSingleOrNull()
        projectCache.evict(key = guildId)
    }
}

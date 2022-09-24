package org.dreamexposure.ticketbird.business

import discord4j.common.util.Snowflake
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.business.cache.CacheRepository
import org.dreamexposure.ticketbird.database.ProjectData
import org.dreamexposure.ticketbird.database.ProjectRepository
import org.dreamexposure.ticketbird.`object`.Project
import org.springframework.stereotype.Component

@Component
class DefaultProjectService(
    private val projectRepository: ProjectRepository,
    private val projectCache: CacheRepository<Long, Array<Project>>
) : ProjectService {
    override suspend fun getProject(guildId: Snowflake, name: String): Project? {
        return getAllProjects(guildId).firstOrNull { it.name == name }
    }

    override suspend fun getAllProjects(guildId: Snowflake): List<Project> {
        var projects = projectCache.get(guildId.asLong())?.toList()
        if (projects != null) return projects

        projects = projectRepository.findByGuildId(guildId.asLong())
            .map(::Project)
            .collectList()
            .awaitSingle()

        projectCache.put(guildId.asLong(), projects.toTypedArray())
        return projects
    }

    override suspend fun createProject(project: Project): Project {
        val newProject =  projectRepository.save(ProjectData(
            guildId = project.guildId.asLong(),
            projectName = project.name,
            projectPrefix = project.prefix,
        )).map(::Project).awaitSingle()

        val cached = projectCache.get(project.guildId.asLong())
        if (cached != null) projectCache.put(project.guildId.asLong(), cached + newProject)

        return project
    }

    override suspend fun updateProject(project: Project) {
        projectRepository.updateByIdAndGuildId(
            id = project.id,
            guildId = project.guildId.asLong(),
            name = project.name,
            prefix = project.prefix,
        ).awaitSingleOrNull()

        val cached = projectCache.get(project.guildId.asLong())
        if (cached != null) {
            val newList = cached.toMutableList()
            newList.removeIf { it.id == project.id }
            projectCache.put(project.guildId.asLong(), (newList + project).toTypedArray())
        }
    }

    override suspend fun deleteProject(guildId: Snowflake, name: String) {
        projectRepository.deleteByGuildIdAndProjectName(guildId.asLong(), name).awaitSingleOrNull()

        val cached = projectCache.get(guildId.asLong())
        if (cached != null) {
            val newList = cached.toMutableList()
            newList.removeIf { it.name == name }
            projectCache.put(guildId.asLong(), newList.toTypedArray())
        }
    }

    override suspend fun deleteProject(guildId: Snowflake, id: Long) {
        projectRepository.deleteById(id).awaitSingleOrNull()

        val cached = projectCache.get(guildId.asLong())
        if (cached != null) {
            val newList = cached.toMutableList()
            newList.removeIf { it.id == id }
            projectCache.put(guildId.asLong(), newList.toTypedArray())
        }
    }

    override suspend fun deleteAllProjects(guildId: Snowflake) {
        projectRepository.deleteAllByGuildId(guildId.asLong()).awaitSingleOrNull()
        projectCache.evict(guildId.asLong())
    }
}

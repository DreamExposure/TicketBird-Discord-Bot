package org.dreamexposure.ticketbird.business

import discord4j.common.util.Snowflake
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.dreamexposure.ticketbird.database.ProjectData
import org.dreamexposure.ticketbird.database.ProjectRepository
import org.dreamexposure.ticketbird.`object`.Project
import org.springframework.stereotype.Component

@Component
class DefaultProjectService(private val projectRepository: ProjectRepository): ProjectService {
    override suspend fun getProject(guildId: Snowflake, name: String): Project? {
        return projectRepository.findByGuildIdAndProjectName(guildId.asLong(), name)
            .map(::Project)
            .awaitSingleOrNull()
    }

    override suspend fun getAllProjects(guildId: Snowflake): List<Project> {
        return projectRepository.findByGuildId(guildId.asLong())
            .map(::Project)
            .collectList()
            .awaitSingle()
    }

    override suspend fun createProject(project: Project): Project {
        return projectRepository.save(ProjectData(
            guildId = project.guildId.asLong(),
            projectName = project.name,
            projectPrefix = project.prefix,
        )).map(::Project).awaitSingle()
    }

    override suspend fun updateProject(project: Project) {
        projectRepository.updateByIdAndGuildId(
            id = project.id,
            guildId = project.guildId.asLong(),
            name = project.name,
            prefix = project.prefix,
        ).awaitSingleOrNull()
    }

    override suspend fun deleteProject(guildId: Snowflake, name: String) {
        projectRepository.deleteByGuildIdAndProjectName(guildId.asLong(), name).awaitSingleOrNull()
    }

    override suspend fun deleteProject(id: Long) {
        projectRepository.deleteById(id).awaitSingleOrNull()
    }

    override suspend fun deleteAllProjects(guildId: Snowflake) {
        projectRepository.deleteAllByGuildId(guildId.asLong()).awaitSingleOrNull()
    }
}

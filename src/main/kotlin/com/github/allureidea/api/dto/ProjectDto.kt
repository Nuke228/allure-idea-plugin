package com.github.allureidea.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProjectDto(
    val id: Long,
    val name: String
) {
    override fun toString(): String = "$name (id=$id)"
}

@Serializable
data class ProjectListResponse(
    val content: List<ProjectDto>
)

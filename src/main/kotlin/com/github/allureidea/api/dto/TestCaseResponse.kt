package com.github.allureidea.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class TestCaseResponse(
    val id: Long,
    val name: String,
    val projectId: Long
)

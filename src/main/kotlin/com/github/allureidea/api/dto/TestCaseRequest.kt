package com.github.allureidea.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class TestCaseLabel(
    val name: String,
    val value: String
)

@Serializable
data class TestCaseRequest(
    val projectId: Long,
    val name: String,
    val description: String? = null,
    val labels: List<TestCaseLabel> = emptyList()
)

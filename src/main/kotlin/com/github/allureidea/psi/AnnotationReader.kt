package com.github.allureidea.psi

import com.intellij.psi.PsiMethod

object AnnotationReader {

    private val TEST_ANNOTATIONS_FQN = setOf(
        "org.junit.Test",
        "org.junit.jupiter.api.Test",
        "org.testng.annotations.Test"
    )

    private val TEST_ANNOTATIONS_SHORT = setOf("Test")

    private const val ALLURE_ID_FQN = "io.qameta.allure.AllureId"
    private const val ALLURE_ID_SHORT = "AllureId"

    private val DISPLAY_NAME_FQN = setOf(
        "org.junit.jupiter.api.DisplayName",
        "org.testng.annotations.Test" // TestNG uses @Test(description=...)
    )
    private const val DISPLAY_NAME_SHORT = "DisplayName"

    fun isTestMethod(method: PsiMethod): Boolean =
        method.annotations.any { ann ->
            val fqn = ann.qualifiedName
            if (fqn != null) fqn in TEST_ANNOTATIONS_FQN
            else ann.nameReferenceElement?.referenceName in TEST_ANNOTATIONS_SHORT
        }

    fun hasAllureId(method: PsiMethod): Boolean =
        method.annotations.any { ann ->
            val fqn = ann.qualifiedName
            if (fqn != null) fqn == ALLURE_ID_FQN
            else ann.nameReferenceElement?.referenceName == ALLURE_ID_SHORT
        }

    fun getDisplayName(method: PsiMethod): String? {
        for (ann in method.annotations) {
            val fqn = ann.qualifiedName
            val isDisplayName = if (fqn != null) fqn == "org.junit.jupiter.api.DisplayName"
                else ann.nameReferenceElement?.referenceName == DISPLAY_NAME_SHORT
            if (isDisplayName) {
                val value = ann.findAttributeValue("value")
                val text = value?.text?.removeSurrounding("\"")
                if (!text.isNullOrBlank()) return text
            }
        }
        return null
    }

    fun getTestCaseName(method: PsiMethod): String =
        getDisplayName(method) ?: method.name
}

package com.github.allureidea.service

import com.github.allureidea.api.AllureApiClient
import com.github.allureidea.api.AllureAuthManager
import com.github.allureidea.api.dto.TestCaseRequest
import com.github.allureidea.psi.AllureIdAnnotationWriter
import com.github.allureidea.psi.AnnotationReader
import com.github.allureidea.settings.AllureSettingsState
import com.github.allureidea.settings.AllureTokenStorage
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod
import com.intellij.psi.SmartPointerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class AllurePluginService(
    private val project: Project,
    private val scope: CoroutineScope
) {

    fun createTestCaseFromMethod(method: PsiMethod) {
        val pointer = SmartPointerManager.createPointer(method)

        scope.launch {
            try {
                val settings = AllureSettingsState.getInstance()
                val url = settings.allureUrl
                val projectId = settings.selectedProjectId
                val token = AllureTokenStorage.getToken()

                if (url.isBlank() || projectId == 0L || token.isNullOrBlank()) {
                    notify("Please configure Allure TestOps in Settings > Tools > Allure TestOps", NotificationType.WARNING)
                    return@launch
                }

                val testCaseName = readAction {
                    val method = pointer.element
                        ?: throw IllegalStateException("Method reference is no longer valid")
                    AnnotationReader.getTestCaseName(method)
                }

                val response = withContext(Dispatchers.IO) {
                    val authManager = AllureAuthManager(url)
                    val jwt = authManager.getValidJwt(token)
                    val client = AllureApiClient(url)
                    client.createTestCase(
                        jwt = jwt,
                        request = TestCaseRequest(
                            projectId = projectId,
                            name = testCaseName
                        )
                    )
                }

                ApplicationManager.getApplication().invokeLater({
                    try {
                        val m = pointer.element
                        if (m != null) {
                            AllureIdAnnotationWriter.addAllureIdAnnotation(project, m, response.id)
                            notify("Test case #${response.id} created in Allure TestOps", NotificationType.INFORMATION)
                        } else {
                            notify("Test case #${response.id} created, but could not add @AllureId — method reference lost", NotificationType.WARNING)
                        }
                    } catch (e: Throwable) {
                        notify("Test case #${response.id} created, but failed to add @AllureId: ${e.message}", NotificationType.WARNING)
                    }
                }, ModalityState.defaultModalityState())
            } catch (e: Exception) {
                notify("Failed to create test case: ${e.message}", NotificationType.ERROR)
            }
        }
    }

    private fun notify(content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Allure TestOps Notifications")
            .createNotification(content, type)
            .notify(project)
    }
}

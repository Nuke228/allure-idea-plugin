package com.github.allureidea.action

import com.github.allureidea.psi.AnnotationReader
import com.github.allureidea.service.AllurePluginService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElement

class CreateAllureTestAction : AnAction() {

    private val log = Logger.getInstance(CreateAllureTestAction::class.java)

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        try {
            val psiMethod = findPsiMethodAtCaret(e)
            if (psiMethod == null) {
                e.presentation.isEnabledAndVisible = false
                return
            }

            val visible = ReadAction.compute<Boolean, Throwable> {
                val isTest = AnnotationReader.isTestMethod(psiMethod)
                val hasId = AnnotationReader.hasAllureId(psiMethod)
                log.info("Allure update: method='${psiMethod.name}', isTest=$isTest, hasAllureId=$hasId")
                isTest && !hasId
            }
            e.presentation.isEnabledAndVisible = visible
        } catch (t: Throwable) {
            log.error("Allure: error in update()", t)
            e.presentation.isEnabledAndVisible = false
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val psiMethod = findPsiMethodAtCaret(e) ?: return

        project.getService(AllurePluginService::class.java)
            .createTestCaseFromMethod(psiMethod)
    }

    private fun findPsiMethodAtCaret(e: AnActionEvent): PsiMethod? {
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return null
        val editor = e.getData(CommonDataKeys.EDITOR)

        if (editor != null) {
            val offset = editor.caretModel.offset
            val element = psiFile.findElementAt(offset) ?: return null

            // Use UAST to find UMethod — works for both Java and Kotlin
            val uElement = element.toUElement()
            val uMethod = uElement?.getParentOfType<UMethod>()
            if (uMethod != null) {
                val javaPsi = uMethod.javaPsi
                if (javaPsi is PsiMethod) return javaPsi
            }
        }

        // Fallback: PSI_ELEMENT
        val psiElement = e.getData(CommonDataKeys.PSI_ELEMENT)
        if (psiElement is PsiMethod) return psiElement
        val uElement = psiElement?.toUElement()
        if (uElement is UMethod) {
            val javaPsi = uElement.javaPsi
            if (javaPsi is PsiMethod) return javaPsi
        }
        return null
    }
}

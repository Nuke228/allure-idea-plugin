package com.github.allureidea.psi

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.codeStyle.JavaCodeStyleManager

object AllureIdAnnotationWriter {

    private const val ALLURE_ID_FQN = "io.qameta.allure.AllureId"
    private const val ALLURE_ID_SHORT = "AllureId"

    fun addAllureIdAnnotation(project: Project, method: PsiMethod, testCaseId: Long) {
        val sourcePsi = method.navigationElement
        val containingFile = sourcePsi.containingFile ?: return

        WriteCommandAction.runWriteCommandAction(project, "Add @AllureId annotation", null, {
            if (isKotlinFile(sourcePsi)) {
                addAnnotationToKotlin(project, sourcePsi, testCaseId)
            } else {
                addAnnotationToJava(project, method, testCaseId)
            }
        }, containingFile)
    }

    private fun isKotlinFile(element: PsiElement): Boolean {
        val file = element.containingFile ?: return false
        return file.name.endsWith(".kt") || file.name.endsWith(".kts")
    }

    private fun addAnnotationToJava(project: Project, method: PsiMethod, testCaseId: Long) {
        val factory = JavaPsiFacade.getElementFactory(project)
        val annotationText = "@$ALLURE_ID_FQN(\"$testCaseId\")"
        val annotation = factory.createAnnotationFromText(annotationText, method)

        val modifierList = method.modifierList
        modifierList.addBefore(annotation, modifierList.firstChild)

        JavaCodeStyleManager.getInstance(project).shortenClassReferences(modifierList)
    }

    private fun addAnnotationToKotlin(project: Project, sourcePsi: PsiElement, testCaseId: Long) {
        val containingFile = sourcePsi.containingFile ?: return
        val document = PsiDocumentManager.getInstance(project).getDocument(containingFile) ?: return

        val fileText = document.text

        // Add import if not present
        if (!fileText.contains("import $ALLURE_ID_FQN")) {
            val importLine = "import $ALLURE_ID_FQN"
            // Find position to insert import: after last import or after package
            val lastImportEnd = findLastImportEnd(fileText)
            if (lastImportEnd >= 0) {
                document.insertString(lastImportEnd, "\n$importLine")
            } else {
                val packageEnd = findPackageEnd(fileText)
                if (packageEnd >= 0) {
                    document.insertString(packageEnd, "\n$importLine")
                } else {
                    document.insertString(0, "$importLine\n")
                }
            }
        }

        // Re-read offsets after potential import insertion
        PsiDocumentManager.getInstance(project).commitDocument(document)
        val updatedSourcePsi = containingFile.findElementAt(sourcePsi.textRange.startOffset)
            ?.let { findFunctionParent(it) } ?: sourcePsi

        val startOffset = updatedSourcePsi.textRange.startOffset
        val lineNumber = document.getLineNumber(startOffset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)

        // Detect indentation
        val lineText = document.getText(TextRange(lineStartOffset, startOffset))
        val indent = lineText.takeWhile { it == ' ' || it == '\t' }

        val annotationLine = "${indent}@$ALLURE_ID_SHORT(\"$testCaseId\")\n"
        document.insertString(lineStartOffset, annotationLine)

        PsiDocumentManager.getInstance(project).commitDocument(document)
    }

    private fun findFunctionParent(element: PsiElement): PsiElement? {
        var current: PsiElement? = element
        while (current != null) {
            if (current.javaClass.simpleName.contains("KtNamedFunction")) {
                return current
            }
            current = current.parent
        }
        return null
    }

    private fun findLastImportEnd(text: String): Int {
        val regex = Regex("^import .+$", RegexOption.MULTILINE)
        val matches = regex.findAll(text).toList()
        if (matches.isEmpty()) return -1
        // Position right after last matched char (at the \n or end of string)
        return matches.last().range.last + 1
    }

    private fun findPackageEnd(text: String): Int {
        val regex = Regex("^package .+$", RegexOption.MULTILINE)
        val match = regex.find(text) ?: return -1
        return match.range.last + 1
    }
}

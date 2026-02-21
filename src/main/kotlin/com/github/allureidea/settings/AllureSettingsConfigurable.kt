package com.github.allureidea.settings

import com.github.allureidea.api.AllureApiClient
import com.github.allureidea.api.AllureAuthManager
import com.github.allureidea.api.dto.ProjectDto
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.dsl.builder.*
import java.awt.Component
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListCellRenderer
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPasswordField
import javax.swing.SwingUtilities

class AllureSettingsConfigurable : Configurable {

    private val log = Logger.getInstance(AllureSettingsConfigurable::class.java)

    private var urlField: javax.swing.JTextField? = null
    private var tokenField: JPasswordField? = null
    private var projectCombo: ComboBox<ProjectDto>? = null
    private var statusLabel: javax.swing.JLabel? = null

    override fun getDisplayName(): String = "Allure TestOps"

    override fun createComponent(): JComponent {
        val settings = AllureSettingsState.getInstance()
        val tf = JPasswordField(40)
        tokenField = tf
        val combo = ComboBox<ProjectDto>()
        combo.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
            ): Component {
                val text = (value as? ProjectDto)?.let { "${it.name} (id=${it.id})" } ?: ""
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus)
            }
        }
        projectCombo = combo
        val status = javax.swing.JLabel(" ")
        statusLabel = status

        val p = panel {
            row("Allure TestOps URL:") {
                urlField = textField()
                    .columns(COLUMNS_LARGE)
                    .comment("e.g. https://allure.example.com")
                    .component
            }
            row("API Token:") {
                cell(tf).columns(COLUMNS_LARGE)
                    .comment("Personal API token from Allure TestOps")
            }
            row {
                button("Test Connection & Load Projects") { loadProjects() }
            }
            row {
                cell(status)
            }
            row("Project:") {
                cell(combo).columns(COLUMNS_LARGE)
            }
        }

        urlField?.text = settings.allureUrl
        val savedToken = AllureTokenStorage.getToken()
        if (!savedToken.isNullOrEmpty()) {
            tf.text = savedToken
        }
        if (settings.selectedProjectId > 0) {
            val current = ProjectDto(settings.selectedProjectId, settings.selectedProjectName)
            combo.model = DefaultComboBoxModel(arrayOf(current))
            combo.selectedItem = current
        }

        return p
    }

    override fun isModified(): Boolean {
        val settings = AllureSettingsState.getInstance()
        val urlChanged = urlField?.text?.trimEnd('/') != settings.allureUrl
        val tokenChanged = String(tokenField?.password ?: charArrayOf()) != (AllureTokenStorage.getToken() ?: "")
        val projectChanged = (projectCombo?.selectedItem as? ProjectDto)?.id != settings.selectedProjectId
        return urlChanged || tokenChanged || projectChanged
    }

    override fun apply() {
        val settings = AllureSettingsState.getInstance()
        settings.allureUrl = urlField?.text?.trimEnd('/') ?: ""
        AllureTokenStorage.setToken(String(tokenField?.password ?: charArrayOf()))
        val selected = projectCombo?.selectedItem as? ProjectDto
        if (selected != null) {
            settings.selectedProjectId = selected.id
            settings.selectedProjectName = selected.name
        }
    }

    override fun reset() {
        val settings = AllureSettingsState.getInstance()
        urlField?.text = settings.allureUrl
        val token = AllureTokenStorage.getToken()
        if (!token.isNullOrEmpty()) {
            tokenField?.text = token
        }
        if (settings.selectedProjectId > 0) {
            val current = ProjectDto(settings.selectedProjectId, settings.selectedProjectName)
            projectCombo?.model = DefaultComboBoxModel(arrayOf(current))
            projectCombo?.selectedItem = current
        }
    }

    private fun loadProjects() {
        val url = urlField?.text?.trimEnd('/') ?: return
        val token = String(tokenField?.password ?: charArrayOf())
        if (url.isBlank() || token.isBlank()) {
            statusLabel?.text = "URL and Token must not be empty"
            return
        }
        statusLabel?.text = "Connecting..."

        Thread {
            try {
                log.info("Allure: exchanging token at $url")
                val authManager = AllureAuthManager(url)
                val jwt = authManager.exchangeToken(token)
                log.info("Allure: token exchanged, loading projects")

                val client = AllureApiClient(url)
                val projects = client.getProjects(jwt)
                log.info("Allure: loaded ${projects.size} projects")

                SwingUtilities.invokeLater {
                    val model = DefaultComboBoxModel(projects.toTypedArray())
                    projectCombo?.model = model
                    val settings = AllureSettingsState.getInstance()
                    if (settings.selectedProjectId > 0) {
                        val match = projects.find { it.id == settings.selectedProjectId }
                        if (match != null) projectCombo?.selectedItem = match
                    }
                    statusLabel?.text = "Loaded ${projects.size} projects"
                }
            } catch (t: Throwable) {
                log.error("Allure: connection failed", t)
                SwingUtilities.invokeLater {
                    statusLabel?.text = "Error: ${t.message ?: t.javaClass.simpleName}"
                }
            }
        }.apply {
            name = "allure-test-connection"
            isDaemon = true
            start()
        }
    }
}

package com.github.allureidea.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(name = "AllureTestOpsSettings", storages = [Storage("allureTestOps.xml")])
class AllureSettingsState : PersistentStateComponent<AllureSettingsState.State> {

    data class State(
        var allureUrl: String = "",
        var selectedProjectId: Long = 0,
        var selectedProjectName: String = ""
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    var allureUrl: String
        get() = state.allureUrl
        set(value) { state.allureUrl = value.trimEnd('/') }

    var selectedProjectId: Long
        get() = state.selectedProjectId
        set(value) { state.selectedProjectId = value }

    var selectedProjectName: String
        get() = state.selectedProjectName
        set(value) { state.selectedProjectName = value }

    companion object {
        fun getInstance(): AllureSettingsState =
            ApplicationManager.getApplication().getService(AllureSettingsState::class.java)
    }
}

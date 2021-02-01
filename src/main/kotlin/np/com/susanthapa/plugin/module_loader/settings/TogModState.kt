package np.com.susanthapa.plugin.module_loader.settings

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager

@State(
    name = "np.com.susanthapa.plugin.module_loader.settings.TogModState",
    storages = [Storage("TogMod.xml")]
)
class TogModState : PersistentStateComponent<TogModState.State> {

    data class State(
        val isGradleSyncEnabled: Boolean = false,
        val isSettingsFileEnabled: Boolean = false,
        val excludedModulesList: List<String> = listOf()
    )

    private var state = State()

    companion object {
        fun getInstance(project: Project): TogModState {
            return ServiceManager.getService(project, TogModState::class.java)
        }
    }

    override fun getState(): State {
        return state
    }

    override fun loadState(state: State) {
        this.state = state
    }
}
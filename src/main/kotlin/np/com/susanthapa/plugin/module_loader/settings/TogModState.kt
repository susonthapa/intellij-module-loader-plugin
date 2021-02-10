package np.com.susanthapa.plugin.module_loader.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@State(
    name = "np.com.susanthapa.plugin.module_loader.settings.TogModState",
    storages = [Storage("TogModSettings.xml")]
)
class TogModState : PersistentStateComponent<TogModState.State> {

    data class State(
        var isGradleSyncEnabled: Boolean = false,
        var isSettingsFileEnabled: Boolean = false,
        var isCleanBuildEnabled: Boolean = false,
        var excludedModulesList: List<String> = listOf()
    )

    private var state = State()

    companion object {
        fun getInstance(project: Project): TogModState {
            return project.getService(TogModState::class.java)
        }
    }

    override fun getState(): State {
        return state
    }

    override fun loadState(state: State) {
        this.state = state
    }
}
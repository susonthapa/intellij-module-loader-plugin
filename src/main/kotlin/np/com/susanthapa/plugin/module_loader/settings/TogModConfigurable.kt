package np.com.susanthapa.plugin.module_loader.settings

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import np.com.susanthapa.plugin.module_loader.TogModule
import np.com.susanthapa.plugin.module_loader.Utils
import javax.swing.JComponent

class TogModConfigurable constructor(
    private val project: Project
) : Configurable {

    private var _togModComponent: TogModComponent? = null
    private val togModComponent
        get() = _togModComponent!!

    override fun createComponent(): JComponent {
        _togModComponent = TogModComponent()
        return togModComponent.getPanel()
    }

    override fun isModified(): Boolean {
        val settings = TogModState.getInstance(project).state
        val isGradleSyncModified = togModComponent.getGradleSyncCheckbox().isSelected != settings.isGradleSyncEnabled
        val isSettingsFileModified = togModComponent.getSettingsCheckbox().isSelected != settings.isSettingsFileEnabled
        var isExclusionListModified = false
        for (i in 0 until togModComponent.getExcludedTable().model.rowCount) {
            val togMod = TogModule(
                togModComponent.getExcludedTable().model.getValueAt(i, 0) as String,
                togModComponent.getExcludedTable().model.getValueAt(i, 1) as Boolean
            )
            val isModuleExcluded = settings.excludedModulesList.find { it == togMod.name } != null
            // if the modules is already excluded then check it's status otherwise just check if the
            // module is checked in the list
            isExclusionListModified = if (isModuleExcluded) {
                togMod.isEnabled != isModuleExcluded
            } else {
                togMod.isEnabled
            }

            if (isExclusionListModified) {
                break
            }
        }

        return isGradleSyncModified || isSettingsFileModified || isExclusionListModified
    }

    override fun apply() {
        val settings = TogModState.getInstance(project)
        val exclusionList = mutableListOf<String>()
        for (i in 0 until togModComponent.getExcludedTable().model.rowCount) {
            val togMod = TogModule(
                togModComponent.getExcludedTable().model.getValueAt(i, 0) as String,
                togModComponent.getExcludedTable().model.getValueAt(i, 1) as Boolean
            )
            if (togMod.isEnabled) {
                exclusionList.add(togMod.name)
            }
        }
        val newState = settings.state.copy(
            isGradleSyncEnabled = togModComponent.getGradleSyncCheckbox().isSelected,
            isSettingsFileEnabled = togModComponent.getSettingsCheckbox().isSelected,
            excludedModulesList = exclusionList
        )
        settings.loadState(newState)
    }

    override fun reset() {
        val settings = TogModState.getInstance(project).state
        togModComponent.setGradleSyncStatus(settings.isGradleSyncEnabled)
        togModComponent.setSettingsStatus(settings.isSettingsFileEnabled)
        // convert exclusion list to TodMod list
        val togModules = Utils.getModuleNamesFromXml(project).map {
            val isModuleExcluded = settings.excludedModulesList.contains(it)
            TogModule(it, isModuleExcluded)
        }
        togModComponent.setAllModules(togModules)
    }

    override fun getDisplayName(): String {
        return "TogMod"
    }

    override fun disposeUIResources() {
        _togModComponent = null
    }
}
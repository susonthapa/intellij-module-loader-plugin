package np.com.susanthapa.plugin.module_loader

import com.android.tools.idea.gradle.project.build.GradleProjectBuilder
import com.android.tools.idea.gradle.project.sync.GradleSyncInvoker
import com.google.wireless.android.sdk.stats.GradleSyncStats
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import np.com.susanthapa.plugin.module_loader.settings.TogModState
import org.jetbrains.plugins.gradle.settings.GradleSettings
import java.io.File
import java.util.logging.Logger

class ModuleLoaderAction : AnAction() {

    private val logger = PluginLogger()

    init {
        logger.setLevel(PluginLogger.DebugLevel.PRODUCTION)
    }

    override fun update(e: AnActionEvent) {
        if (GradleSettings.getInstance(e.project!!).linkedProjectsSettings.isEmpty()) {
            logger.warn("this is not a gradle project, disabling the action")
            e.presentation.isVisible = false
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (e.project == null) {
            NotificationManager.notifyError(null, "No project found!")
            return
        }
        val project = e.project!!
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (virtualFiles == null) {
            logger.info("virtual files from action null")
            NotificationManager.notifyWarn(project, "No file associated with selection!")
            return
        }

        // obtain the selected modules fully qualified name
        val allModulesPath = Utils.getModulesPathFromXml(project)
        logger.debug("resolved modules path from modules.xml: \n $allModulesPath")
        val selectedModules = virtualFiles.mapNotNull {
            val path = it.path
            val modulePath = path.replace(project.basePath!!, "")
                .replace(File.separator, ".")
            ("${project.name}$modulePath")
        }.filter { name ->
            val isModulePresent = allModulesPath.find { it.contains(name) } != null
            if (!isModulePresent) {
                NotificationManager.notifyWarn(project, "$name not found in loaded modules!")
            }
            isModulePresent
        }
        if (selectedModules.isEmpty()) {
            NotificationManager.notifyWarn(project, "No modules to work with!")
            return
        }

        toggleModules(project, selectedModules, allModulesPath)
    }

    private fun toggleModules(project: Project, selectedModules: List<String>, allModulesPath: List<String>) {
        val resolvedModules = resolveNestedModules(selectedModules, allModulesPath)
        val appModules = resolvedModules.mapNotNull {
            val module = ModuleManager.getInstance(project).findModuleByName(it)
            val isModuleLoaded = module != null
            if (isModuleLoaded) {
                AppModule(module!!.name, it, isModuleLoaded)
            } else {
                val path = allModulesPath.find { modulePath ->
                    modulePath.contains(it)
                }
                if (path == null) {
                    null
                } else {
                    AppModule(path, it, isModuleLoaded)
                }
            }
        }

        val loadingModules = appModules.filter { !it.isLoaded }
        val unloadingModules = appModules.filter { it.isLoaded }

        if (loadingModules.isNotEmpty()) {
            // load modules
            logger.debug("loading modules: $loadingModules")
            loadModules(project, loadingModules)
        }

        if (unloadingModules.isNotEmpty()) {
            // unload modules
            logger.debug("unloading modules: $unloadingModules")
            unLoadModules(project, unloadingModules)
        }

        processModuleAction(project, appModules, { modulePath, appModule ->
            if (appModule.isLoaded) {
                "//$modulePath"
            } else {
                modulePath.substring(2)
            }
        }, {
            if (loadingModules.isNotEmpty()) {
                NotificationManager.notifyInformation(project, "Loaded ${loadingModules.size} modules")
            }
            if (unloadingModules.isNotEmpty()) {
                NotificationManager.notifyInformation(project, "Unloading ${unloadingModules.size} modules")
            }
        })
    }

    private fun resolveNestedModules(selectedModules: List<String>, allModulesPath: List<String>): List<String> {
        val resolvedModules = mutableListOf<String>()
        selectedModules.forEach { selectedModule ->
            // resolve nested modules if present
            val matchedModules = allModulesPath.filter {
                it.contains(selectedModule)
            }.toMutableList()
            if (matchedModules.size == 1) {
                logger.debug("no nested modules found: $selectedModule")
                resolvedModules.add(selectedModule)
            } else {
                logger.debug("found nested modules, resolving $selectedModule")
                val processedModules = Utils.getModulesNameFromPath(matchedModules).toMutableList()
                processedModules.remove(selectedModule)
                resolvedModules.addAll(processedModules)
                logger.debug("resolved modules: $processedModules")
            }
        }

        return resolvedModules
    }

    private fun unLoadModules(project: Project, modules: List<AppModule>) {
        // unload the module from the project
        val unloadedModuleNames = modules.map {
            it.identifier
        }.toMutableList()
        logger.debug("requested modules to unloaded: $unloadedModuleNames")
        val previouslyUnloadedModules = ModuleManager.getInstance(project).unloadedModuleDescriptions.map {
            it.name
        }.filter { !unloadedModuleNames.contains(it) }
        logger.debug("previously unloaded modules: $previouslyUnloadedModules")
        unloadedModuleNames.addAll(previouslyUnloadedModules)
        logger.debug("unloading modules: $unloadedModuleNames")
        ModuleManager.getInstance(project).setUnloadedModules(unloadedModuleNames)
    }

    private fun loadModules(project: Project, modules: List<AppModule>) {
        if (modules.isEmpty()) {
            NotificationManager.notifyError(project, "Failed to load modules, module path is invalid")
            return
        }
        modules.forEach {
            val path = VirtualFileManager.getInstance()
                .getFileSystem("file")
                .refreshAndFindFileByPath(it.identifier)
                ?.toNioPath()
            if (path != null) {
                runWriteAction {
                    ModuleManager.getInstance(project).loadModule(path)
                    logger.debug("loading module: $it")
                }
            } else {
                logger.warn("failed to resolve path for module: $it")
            }
        }
        if (TogModState.getInstance(project).state.isCleanBuildEnabled) {
            cleanBuild(project)
        }
    }

    private fun getSettingsFilePath(project: Project): String {
        return "${project.basePath}${File.separator}settings.gradle"
    }

    private fun processModuleAction(
        project: Project,
        modules: List<AppModule>,
        mapper: (String, AppModule) -> String,
        onComplete: () -> Unit
    ) {
        val settings = TogModState.getInstance(project).state
        if (settings.isSettingsFileEnabled) {
            val settingFile = VirtualFileManager.getInstance()
                .getFileSystem("file")
                .refreshAndFindFileByPath(getSettingsFilePath(project))
            if (settingFile == null) {
                NotificationManager.notifyError(project, "Failed to locate settings.gradle file!")
                return
            }
            logger.debug("requested modules to toggle: $modules")
            // sanitize the modules name
            val sanitizedModules = modules
                .filter {
                    !settings.excludedModulesList.contains(it.identifier)
                }.map {
                    val name = if (it.name.contains(project.name)) {
                        it.name.replace(project.name, "")
                            .replace(".", ":")
                    } else {
                        it.name.replace(".", ":")
                    }
                    it.copy(identifier = name)
                }
            logger.debug("modules after apply exclusion: $sanitizedModules")
            if (sanitizedModules.isEmpty()) {
                onComplete()
                if (settings.isGradleSyncEnabled) {
                    triggerGradleSync(project)
                }
                logger.debug("modules after exclusion list is empty, aborting settings.gradle file update!")
                return
            }
            val updatedContent = settingFile.inputStream.bufferedReader().readLines()
                .map { module ->
                    // find the referenced AppModule
                    val appModule = sanitizedModules.find { module.contains(it.identifier) }
                    if (appModule != null) {
                        mapper(module, appModule)
                    } else {
                        module
                    }
                }

            runWriteAction {
                val writer = settingFile.getOutputStream(this).bufferedWriter()
                updatedContent.forEach {
                    writer.write(it)
                    writer.newLine()
                }
                writer.flush()
                writer.close()
                // only trigger gradle sync after all files changes are synced to prevent any "Sync Now" tooltip
                VirtualFileManager.getInstance().asyncRefresh {
                    onComplete()
                    if (settings.isGradleSyncEnabled) {
                        triggerGradleSync(project)
                    }
                }
            }
        } else {
            onComplete()
            if (settings.isGradleSyncEnabled) {
                triggerGradleSync(project)
            }
        }
    }

    private fun triggerGradleSync(project: Project) {
        logger.debug("initiating gradle sync")
        GradleSyncInvoker.getInstance().requestProjectSync(project, GradleSyncStats.Trigger.TRIGGER_PROJECT_MODIFIED)
    }

    private fun cleanBuild(project: Project) {
        logger.debug("initialing clean build")
        GradleProjectBuilder.getInstance(project).clean()
    }
}
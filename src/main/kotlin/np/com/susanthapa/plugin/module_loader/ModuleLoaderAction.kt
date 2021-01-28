package np.com.susanthapa.plugin.module_loader

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFileManager
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import org.jetbrains.plugins.gradle.util.GradleConstants
import javax.xml.parsers.DocumentBuilderFactory

class ModuleLoaderAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        if (e.project == null) {
            NotificationManager.notifyError(null, "No project found!")
            return
        }
        val project = e.project!!
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (virtualFiles == null) {
            NotificationManager.notifyWarn(project, "No file associated with selection!")
            return
        }

        // obtain the selected modules fully qualified name
        val allModulesPath = getModulesPathFromXml(project)
        val selectedModules = virtualFiles.mapNotNull {
            val path = it.path
            val modulePath = path.replace(project.basePath!!, "")
                .replace(File.separator, ".")
            "${project.name}$modulePath"
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
        val modules = selectedModules.mapNotNull {
            ModuleManager.getInstance(project)
                .findModuleByName(it)
        }
        if (modules.isEmpty()) {
            val unloadedModulesPath = allModulesPath.filter { modulePath ->
                selectedModules.find {
                    modulePath.contains(it)
                } != null
            }
            loadModules(project, selectedModules, unloadedModulesPath)
        } else {
            if (modules.size != selectedModules.size) {
                NotificationManager.notifyWarn(project, "Combination of both loaded and unloaded modules not supported!")
                return
            }
            unLoadModules(project, modules)
        }
    }

    private fun unLoadModules(project: Project, modules: List<Module>) {
        // unload the module from the project
        println("unloading modules")
        val unloadedModuleNames = modules.map {
            it.name
        }
        ModuleManager.getInstance(project).setUnloadedModules(unloadedModuleNames)
        val modulesNames = modules.map { it.name }
        processModuleAction(project, modulesNames, { module, sanitizedNames ->
            if (module.startsWith("//")) {
                module
            } else {
                // check if this module needs to be unloaded
                val isModulePresent = sanitizedNames.find { module.contains(it) } != null
                if (isModulePresent) {
                    "//$module"
                } else {
                    module
                }
            }
        }, {
            NotificationManager.notifyInformation(project, "Unloaded ${unloadedModuleNames.size} modules")
        })
    }

    private fun processModuleAction(
        project: Project,
        modules: List<String>,
        mapper: (String, List<String>) -> String,
        onComplete: () -> Unit
    ) {
        val settingFile = VirtualFileManager.getInstance()
            .refreshAndFindFileByNioPath(File("${project.basePath}${File.separator}settings.gradle").toPath())
        if (settingFile == null) {
            NotificationManager.notifyError(project, "Failed to locate settings.gradle file!")
            return
        }
        // sanitize the modules name
        val sanitizedNames = modules.map {
            val name = if (it.contains(project.name)) {
                it.replace(project.name, "")
                    .replace(".", ":")
            } else {
                it.replace(".", ":")
            }
            name
        }
        val updatedContent = settingFile.inputStream.bufferedReader().readLines()
            .map { module -> mapper(module, sanitizedNames) }

        runWriteAction {
            val writer = settingFile.getOutputStream(this).bufferedWriter()
            updatedContent.forEach {
                writer.write(it)
                writer.newLine()
            }
            writer.close()
        }
        onComplete()
        triggerGradleSync(project)
    }

    private fun loadModules(project: Project, selectedModules: List<String>, modulesPath: List<String>) {
        if (modulesPath.isEmpty()) {
            NotificationManager.notifyError(project, "Failed to load modules, module path is invalid")
            return
        }
        modulesPath.forEach {
            runWriteAction {
                ModuleManager.getInstance(project).loadModule(File(it).toPath())
            }
        }
        val loadedModules = ModuleManager.getInstance(project).modules
            .filter {
                selectedModules.contains(it.name)
            }
        cleanBuild(loadedModules)
        processModuleAction(project, selectedModules, { module, sanitizedNames ->
            if (module.startsWith("//")) {
                // check if this module needs to be loaded
                val isModuleUnloaded = sanitizedNames.find { module.contains(it) } != null
                if (isModuleUnloaded) {
                    module.substring(2)
                } else {
                    module
                }
            } else {
                module
            }
        }, {
            NotificationManager.notifyInformation(project, "Loaded ${modulesPath.size} modules")
        })
    }

    private fun triggerGradleSync(project: Project) {
        ExternalSystemUtil.refreshProject(project, GradleConstants.SYSTEM_ID, project.basePath!!, false, ProgressExecutionMode.IN_BACKGROUND_ASYNC)
    }

    private fun cleanBuild(modules: List<Module>) {
        modules.forEach { module ->
            ModuleRootManager.getInstance(module)
                .excludeRoots
                .forEach {
                    // remove if this is the build directory
                    if (it.path.endsWith("build")) {
                        if (File(it.path).deleteRecursively()) {
                            println("removed directory: ${it.path}")
                        } else {
                            println("failed to remove directory: ${it.path}")
                        }
                    }
                }
        }
    }

    private fun getModulesPathFromXml(project: Project): List<String> {
        val modulesPath = mutableListOf<String>()
        val moduleXml = File("${project.basePath}/.idea/modules.xml")
        val doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(moduleXml)

        doc.documentElement.normalize()
        val modules = doc.getElementsByTagName("module")
        for (i in 0 until modules.length) {
            val node = modules.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                val element = node as Element
                val filePath = element.getAttribute("filepath")
                    .replace("\$PROJECT_DIR\$", project.basePath!!)
                println("filePath: $filePath")
                modulesPath.add(filePath)
            }
        }

        return modulesPath
    }
}
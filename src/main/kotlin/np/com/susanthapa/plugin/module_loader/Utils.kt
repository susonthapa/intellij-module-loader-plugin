package np.com.susanthapa.plugin.module_loader

import com.intellij.openapi.project.Project
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

object Utils {

    fun getModulesPathFromXml(project: Project): List<String> {
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
                modulesPath.add(filePath)
            }
        }

        return modulesPath
    }

    fun getModulesNameFromPath(modulesPath: List<String>): List<String> {
        return modulesPath.map {
            it.substring(it.lastIndexOf(File.separator) + 1, (it.length - 4))
        }
    }

    fun getModuleNamesFromXml(project: Project): List<String> {
        val modulesPath = getModulesPathFromXml(project)
        return getModulesNameFromPath(modulesPath)
    }
}
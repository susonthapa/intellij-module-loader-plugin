package np.com.susanthapa.plugin.module_loader.settings

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.components.BorderLayoutPanel
import np.com.susanthapa.plugin.module_loader.TogModule
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ItemEvent
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

class TogModComponent {

    private val mainPanel = JBPanel<BorderLayoutPanel>()
    private val gradleSyncCheckbox: JBCheckBox
    private val settingsCheckbox: JBCheckBox
    private val cleanBuildCheckbox: JBCheckBox
    private val excludedTable: JBTable
    private val tableModel: DefaultTableModel

    init {
        mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)

        settingsCheckbox = JBCheckBox("Comment / Uncomment module from settings.gradle")
        gradleSyncCheckbox = JBCheckBox("Trigger gradle sync after module toggle")
        cleanBuildCheckbox = JBCheckBox("Clean project after loading modules")
        val exclusionLabel = JBLabel("Exclude any modules that you don't want to comment / uncomment in settings.gradle file");
        tableModel = object : DefaultTableModel() {
            override fun isCellEditable(row: Int, column: Int): Boolean {
                if (column ==1) {
                    return true
                }
                return false
            }

            override fun getColumnClass(column: Int): Class<*> {
                return getValueAt(0, column).javaClass
            }
        }
        tableModel.setColumnIdentifiers(arrayOf("Module Name", "Excluded"))
        excludedTable = JBTable(tableModel)
        // align table header
        excludedTable.tableHeader.defaultRenderer = object : TableCellRenderer {
            private val renderer = excludedTable.tableHeader.defaultRenderer

            override fun getTableCellRendererComponent(
                table: JTable?,
                value: Any?,
                isSelected: Boolean,
                hasFocus: Boolean,
                row: Int,
                col: Int
            ): Component {
                val delegate = renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col)
                if (delegate !is JLabel) {
                    return delegate
                }

                val cmp: JLabel = delegate
                cmp.horizontalAlignment = JLabel.CENTER

                return cmp
            }
        }
        excludedTable.isEnabled = false
        val scrollPane = JBScrollPane(excludedTable)
        // setup alignment
        gradleSyncCheckbox.alignmentX = Component.LEFT_ALIGNMENT
        settingsCheckbox.alignmentX = Component.LEFT_ALIGNMENT
        cleanBuildCheckbox.alignmentX = Component.LEFT_ALIGNMENT
        exclusionLabel.alignmentX = Component.LEFT_ALIGNMENT
        scrollPane.alignmentX = Component.LEFT_ALIGNMENT

        // attach listeners
        settingsCheckbox.addItemListener {
            excludedTable.isEnabled = it.stateChange == ItemEvent.SELECTED
        }

        // add the components
        mainPanel.add(gradleSyncCheckbox)
        mainPanel.add(Box.createRigidArea(Dimension(0, 4)))
        mainPanel.add(settingsCheckbox)
        mainPanel.add(Box.createRigidArea(Dimension(0, 4)))
        mainPanel.add(cleanBuildCheckbox)
        mainPanel.add(Box.createRigidArea(Dimension(0, 16)))
        mainPanel.add(exclusionLabel)
        mainPanel.add(Box.createRigidArea(Dimension(0, 8)))
        mainPanel.add(scrollPane)
    }

    fun getGradleSyncCheckbox(): JBCheckBox {
        return gradleSyncCheckbox
    }

    fun getSettingsCheckbox(): JBCheckBox {
        return settingsCheckbox
    }

    fun getCleanBuildCheckbox(): JBCheckBox {
        return cleanBuildCheckbox
    }

    fun getExcludedTable(): JBTable {
        return excludedTable
    }

    fun setGradleSyncStatus(isEnabled: Boolean) {
        gradleSyncCheckbox.isSelected = isEnabled
    }

    fun setSettingsStatus(isEnabled: Boolean) {
        settingsCheckbox.isSelected = isEnabled
    }

    fun setCleanBuildStatus(isEnabled: Boolean) {
        cleanBuildCheckbox.isSelected = isEnabled
    }

    fun setAllModules(modules: List<TogModule>) {
        modules.map { togModule ->
            arrayOf(togModule.name, togModule.isEnabled)
        }.forEach {
            tableModel.addRow(it)
        }
        tableModel.fireTableDataChanged()
    }

    fun getPanel(): JPanel {
        return mainPanel
    }
}
package np.com.susanthapa.plugin.module_loader.settings

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import np.com.susanthapa.plugin.module_loader.TogModule
import java.awt.Component
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

class TogModComponent {

    private val mainPanel: JPanel = JPanel()
    private val gradleSyncCheckbox: JBCheckBox
    private val settingsCheckbox: JBCheckBox
    private val excludedTable: JBTable
    private val tableModel: DefaultTableModel

    init {
        mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)

        settingsCheckbox = JBCheckBox("Comment / Uncomment module from settings.gradle")
        settingsCheckbox.border = EmptyBorder(0, 0, 0, 0)
        settingsCheckbox.horizontalAlignment = JLabel.LEFT
        gradleSyncCheckbox = JBCheckBox("Trigger gradle sync after module toggle")
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
        val scrollPane = JBScrollPane(excludedTable)
        scrollPane.border = EmptyBorder(24, 0, 0, 0)
        mainPanel.add(settingsCheckbox)
        mainPanel.add(gradleSyncCheckbox)
        mainPanel.add(scrollPane)
    }

    fun getGradleSyncCheckbox(): JBCheckBox {
        return gradleSyncCheckbox
    }

    fun getSettingsCheckbox(): JBCheckBox {
        return settingsCheckbox
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

    inner class HeaderRenderer constructor(table: JTable) : TableCellRenderer {
        private val renderer = table.tableHeader.defaultRenderer as DefaultTableCellRenderer

        init {
            renderer.horizontalAlignment = JLabel.CENTER
        }

        override fun getTableCellRendererComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            col: Int
        ): Component {
            return  renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col)
        }

    }
}
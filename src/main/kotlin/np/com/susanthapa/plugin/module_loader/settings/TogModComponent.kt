package np.com.susanthapa.plugin.module_loader.settings

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import np.com.susanthapa.plugin.module_loader.TogModule
import java.awt.Component
import java.awt.Dimension
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
        gradleSyncCheckbox = JBCheckBox("Trigger gradle sync after module toggle")
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
        val scrollPane = JBScrollPane(excludedTable)
        // setup alignment
        settingsCheckbox.alignmentX = Component.LEFT_ALIGNMENT
        gradleSyncCheckbox.alignmentX = Component.LEFT_ALIGNMENT
        exclusionLabel.alignmentX = Component.LEFT_ALIGNMENT
        scrollPane.alignmentX = Component.LEFT_ALIGNMENT

        // add the components
        mainPanel.add(settingsCheckbox)
        mainPanel.add(Box.createRigidArea(Dimension(0, 4)))
        mainPanel.add(gradleSyncCheckbox)
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
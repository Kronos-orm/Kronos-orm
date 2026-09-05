package com.kotlinorm.plugin.idea

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.CollectionListModel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.kotlinorm.plugin.idea.codegen.BuiltInKPojoName
import com.kotlinorm.plugin.idea.codegen.KronosDatabaseToolsMetadataProvider
import com.kotlinorm.plugin.idea.codegen.KronosIdeaCodegenRenderer
import com.kotlinorm.plugin.idea.codegen.KronosIdeaDataSource
import com.kotlinorm.plugin.idea.codegen.KronosIdeaGeneratedFile
import com.kotlinorm.plugin.idea.codegen.KronosIdeaGenerationRequest
import com.kotlinorm.plugin.idea.codegen.KronosIdeaTable
import com.kotlinorm.plugin.idea.codegen.KronosIdeaTemplate
import com.kotlinorm.plugin.idea.codegen.KronosTemplateManager
import com.kotlinorm.plugin.idea.setting.PluginSettings
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.ItemEvent
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.JTabbedPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.ListSelectionModel

class MainPanel(private val project: Project) {
    private val metadataProvider = KronosDatabaseToolsMetadataProvider(project)
    private val renderer = KronosIdeaCodegenRenderer()
    private val templateManager = KronosTemplateManager(project)
    private val settings = PluginSettings.getInstance()

    private val mainPanel = JPanel(BorderLayout())
    private val dataSourceCombo = JComboBox<DataSourceItem>()
    private val tableList = JBList<TableItem>()
    private val packageField = JTextField(defaultPackageName())
    private val outputField = JTextField(defaultOutputDirectory())
    private val templateCombo = JComboBox<TemplateItem>()
    private val overwriteBox = JCheckBox("Overwrite existing files")
    private val previewArea = JTextArea()
    private val generatorStatus = JLabel("Load data sources to begin.")
    private val templateList = JBList<TemplateItem>()
    private val templatePreviewArea = JTextArea()
    private val templateSourceLabel = JLabel()
    private val templatePathLabel = JLabel()
    private val templateStatus = JLabel("Project templates are stored in .kronos/templates.")

    private var dataSources: List<KronosIdeaDataSource> = emptyList()
    private var templates: List<KronosIdeaTemplate> = emptyList()

    init {
        setupUI()
        refreshTemplates()
        refreshDataSources()
    }

    val component: JComponent
        get() = mainPanel

    private fun setupUI() {
        val tabs = JTabbedPane()
        tabs.addTab("Code Generator", buildGeneratorTab())
        tabs.addTab("Templates", buildTemplatesTab())
        mainPanel.add(tabs, BorderLayout.CENTER)
    }

    private fun buildGeneratorTab(): JComponent {
        tableList.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
        tableList.visibleRowCount = 12

        dataSourceCombo.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) {
                loadTablesForSelectedDataSource()
            }
        }

        val left = JPanel(BorderLayout(0, 8)).apply {
            border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
            add(JPanel(BorderLayout(8, 0)).apply {
                add(dataSourceCombo, BorderLayout.CENTER)
                add(JButton("Refresh").apply { addActionListener { refreshDataSources() } }, BorderLayout.EAST)
            }, BorderLayout.NORTH)
            add(JBScrollPane(tableList), BorderLayout.CENTER)
        }

        previewArea.isEditable = false
        previewArea.lineWrap = false

        val controls = JPanel(GridBagLayout()).apply {
            border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
            addRow("Package", packageField, 0)
            addRow("Output", outputField, 1)
            addRow("Template", templateCombo, 2)
            add(overwriteBox, GridBagConstraints().apply {
                gridx = 1
                gridy = 3
                weightx = 1.0
                fill = GridBagConstraints.HORIZONTAL
            })
            add(JPanel().apply {
                add(JButton("Preview").apply { addActionListener { previewSelectedTables() } })
                add(JButton("Generate").apply { addActionListener { generateSelectedTables() } })
            }, GridBagConstraints().apply {
                gridx = 1
                gridy = 4
                anchor = GridBagConstraints.WEST
            })
        }

        val right = JPanel(BorderLayout(0, 8)).apply {
            add(controls, BorderLayout.NORTH)
            add(JBScrollPane(previewArea), BorderLayout.CENTER)
            add(generatorStatus, BorderLayout.SOUTH)
        }

        return JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right).apply {
            resizeWeight = 0.35
            preferredSize = Dimension(860, 560)
        }
    }

    private fun buildTemplatesTab(): JComponent {
        templateList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        templateList.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) {
                val selected = templateList.selectedValue ?: return@addListSelectionListener
                selectTemplate(selected)
            }
        }

        templatePreviewArea.isEditable = false
        templatePreviewArea.lineWrap = false

        val left = JPanel(BorderLayout(0, 8)).apply {
            border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
            add(JPanel(BorderLayout(8, 0)).apply {
                add(JLabel("Available templates"), BorderLayout.WEST)
                add(JPanel().apply {
                    add(JButton("Refresh").apply { addActionListener { refreshTemplates() } })
                }, BorderLayout.EAST)
            }, BorderLayout.NORTH)
            add(JBScrollPane(templateList), BorderLayout.CENTER)
            add(templateStatus, BorderLayout.SOUTH)
        }

        val details = JPanel(BorderLayout(0, 8)).apply {
            border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
            add(JPanel(GridBagLayout()).apply {
                addRow("Source", templateSourceLabel, 0)
                addRow("Path", templatePathLabel, 1)
                add(JPanel().apply {
                    add(JButton("Copy to Project").apply { addActionListener { copyBuiltInTemplate() } })
                    add(JButton("Open Project Template").apply { addActionListener { openProjectTemplate() } })
                }, GridBagConstraints().apply {
                    gridx = 1
                    gridy = 2
                    anchor = GridBagConstraints.WEST
                })
            }, BorderLayout.NORTH)
            add(JBScrollPane(templatePreviewArea), BorderLayout.CENTER)
        }

        return JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, details).apply {
            resizeWeight = 0.32
            preferredSize = Dimension(860, 560)
        }
    }

    private fun JPanel.addRow(label: String, component: JComponent, row: Int) {
        add(JLabel(label), GridBagConstraints().apply {
            gridx = 0
            gridy = row
            anchor = GridBagConstraints.WEST
            insets.right = 8
        })
        add(component, GridBagConstraints().apply {
            gridx = 1
            gridy = row
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
        })
    }

    private fun refreshDataSources() {
        val result = metadataProvider.loadDataSources()
        result.onSuccess { loaded ->
            dataSources = loaded
            dataSourceCombo.removeAllItems()
            loaded.forEach { dataSource -> dataSourceCombo.addItem(DataSourceItem(dataSource)) }
            if (loaded.isEmpty()) {
                tableList.model = CollectionListModel(emptyList<TableItem>())
                generatorStatus.text = "No IDEA Database data sources are configured."
            } else {
                dataSourceCombo.selectedIndex = 0
                loadTablesForSelectedDataSource()
            }
        }.onFailure { error ->
            dataSources = emptyList()
            dataSourceCombo.removeAllItems()
            tableList.model = CollectionListModel(emptyList<TableItem>())
            generatorStatus.text = "DatabaseTools metadata could not be loaded: ${error.message}"
        }
    }

    private fun loadTablesForSelectedDataSource() {
        val dataSource = (dataSourceCombo.selectedItem as? DataSourceItem)?.dataSource
        val items = dataSource?.tables.orEmpty().map(::TableItem)
        tableList.model = CollectionListModel(items)
        generatorStatus.text = when {
            dataSource == null -> "Select a data source."
            items.isEmpty() -> "No introspected tables were found for ${dataSource.name}."
            else -> "Select one or more tables."
        }
    }

    private fun refreshTemplates() {
        templates = templateManager.templates()
        val items = templates.map(::TemplateItem)
        templateCombo.removeAllItems()
        items.forEach { templateCombo.addItem(it) }
        templateList.model = CollectionListModel(items)
        val selected = items.firstOrNull { it.template.name == settings.activeTemplateName } ?: items.firstOrNull()
        if (selected != null) {
            templateList.setSelectedValue(selected, true)
            selectTemplate(selected)
        } else {
            templatePreviewArea.text = ""
            templateSourceLabel.text = ""
            templatePathLabel.text = ""
        }
    }

    private fun previewSelectedTables() {
        val request = buildRequest() ?: return
        runCatching { renderer.preview(request) }
            .onSuccess { files ->
                previewArea.text = files.joinToString("\n\n// -----\n\n") { it.content }
                generatorStatus.text = "Previewed ${files.size} file(s)."
            }
            .onFailure { generatorStatus.text = it.message ?: "Preview failed." }
    }

    private fun generateSelectedTables() {
        val request = buildRequest() ?: return
        runCatching {
            val files = renderer.preview(request)
            writeGeneratedFiles(request, files)
            files
        }.onSuccess { files ->
            previewArea.text = files.joinToString("\n\n// -----\n\n") { it.content }
            generatorStatus.text = "Generated ${files.size} file(s) into ${request.outputDirectory}."
        }.onFailure {
            generatorStatus.text = it.message ?: "Generation failed."
        }
    }

    private fun buildRequest(): KronosIdeaGenerationRequest? {
        val tables = tableList.selectedValuesList.map { it.table }
        if (tables.isEmpty()) {
            generatorStatus.text = "Select at least one table."
            return null
        }
        val template = (templateCombo.selectedItem as? TemplateItem)?.template?.name ?: BuiltInKPojoName
        val templateFile = (templateCombo.selectedItem as? TemplateItem)?.template?.projectFile
        val templateContent = templateFile?.let { VfsUtil.loadText(it) }
        settings.activeTemplateName = template
        return KronosIdeaGenerationRequest(
            tables = tables,
            packageName = packageField.text.trim(),
            outputDirectory = outputField.text.trim(),
            overwrite = overwriteBox.isSelected,
            templateName = template,
            templateContent = templateContent,
        )
    }

    private fun writeGeneratedFiles(request: KronosIdeaGenerationRequest, files: List<KronosIdeaGeneratedFile>) {
        require(request.outputDirectory.isNotBlank()) { "Output directory is required." }
        WriteCommandAction.writeCommandAction(project).run<RuntimeException> {
            val root = VfsUtil.createDirectories(request.outputDirectory)
            files.forEach { file ->
                val existing = root.findChild(file.relativePath)
                if (existing != null && !request.overwrite) {
                    error("${file.relativePath} already exists. Enable overwrite to replace it.")
                }
                val target = existing ?: root.findOrCreateChildData(this, file.relativePath)
                VfsUtil.saveText(target, file.content)
            }
            LocalFileSystem.getInstance().refreshAndFindFileByPath(request.outputDirectory)
        }
    }

    private fun copyBuiltInTemplate() {
        runCatching { templateManager.copyBuiltInKPojoToProject() }
            .onSuccess {
                templateStatus.text = "Project template: ${it.path}"
                refreshTemplates()
            }
            .onFailure { templateStatus.text = it.message ?: "Template copy failed." }
    }

    private fun openProjectTemplate() {
        val selected = templateList.selectedValue?.template
        val file = selected?.projectFile ?: runCatching { templateManager.copyBuiltInKPojoToProject() }
            .onFailure { templateStatus.text = it.message ?: "Template open failed." }
            .getOrNull()
            ?: return
        FileEditorManager.getInstance(project).openFile(file, true)
        templateStatus.text = "Opened ${file.path}."
        refreshTemplates()
    }

    private fun selectTemplate(item: TemplateItem) {
        settings.activeTemplateName = item.template.name
        templateCombo.selectedItem = item
        templateSourceLabel.text = item.template.source
        templatePathLabel.text = item.template.path
        templatePreviewArea.text = runCatching { templateManager.content(item.template) }
            .getOrElse { "Could not load template content: ${it.message}" }
        templatePreviewArea.caretPosition = 0
    }

    private fun defaultPackageName(): String = "com.kotlinorm.orm.table"

    private fun defaultOutputDirectory(): String {
        val basePath = project.basePath ?: return ""
        return "$basePath/src/main/kotlin/com/kotlinorm/orm/table"
    }

    private data class DataSourceItem(val dataSource: KronosIdeaDataSource) {
        override fun toString(): String = "${dataSource.name} (${dataSource.tables.size})"
    }

    private data class TableItem(val table: KronosIdeaTable) {
        override fun toString(): String = "${table.displayName} (${table.columns.size})"
    }

    private data class TemplateItem(val template: KronosIdeaTemplate) {
        override fun toString(): String = "${template.name} (${template.source})"
    }
}

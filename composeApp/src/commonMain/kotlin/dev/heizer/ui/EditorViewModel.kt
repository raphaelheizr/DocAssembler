package dev.heizer.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.heizer.core.document.DocumentNodeDefinitionRegistry
import dev.heizer.core.document.DocumentNodeDefinition
import dev.heizer.core.document.Document
import dev.heizer.core.document.DocumentNode
import dev.heizer.core.document.renderer.docx.DocxRenderer
import dev.heizer.core.file.FileRepository
import dev.heizer.core.serializer.JsonSerializer
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

class EditorViewModel {
    private val repository = FileRepository(JsonSerializer.create<DocumentNodeDefinitionRegistry>())
    private var registry: DocumentNodeDefinitionRegistry? = null

    var document by mutableStateOf(Document.create("Novo Documento"))
    
    var definitions by mutableStateOf<List<DocumentNodeDefinition>>(emptyList())

    var errorMessage by mutableStateOf<String?>(null)
    var successMessage by mutableStateOf<String?>(null)

    var selectedNodeId by mutableStateOf<Long?>(null)

    var customTemplateEnabled by mutableStateOf(false)
    var customTemplatePath by mutableStateOf("")

    var isSettingsOpen by mutableStateOf(false)

    init {
        try {
            val loadedRegistry = DocumentNodeDefinitionRegistry.Factory.load(repository)
            registry = loadedRegistry
            definitions = loadedRegistry.definitions
            customTemplateEnabled = loadedRegistry.customTemplateEnabled
            customTemplatePath = loadedRegistry.customTemplatePath
        } catch (e: Exception) {
            errorMessage = "Erro ao carregar definições: ${e.message}"
            // Inicializar um registro vazio caso falhe a carga (ex: arquivo não existe)
            registry = DocumentNodeDefinitionRegistry(emptyList())
        }
    }

    fun selectNode(id: Long) {
        selectedNodeId = id
    }

    fun pickCustomTemplateFile() {
        val chooser = JFileChooser()
        val filter = FileNameExtensionFilter("Documentos Word (.docx)", "docx")
        chooser.fileFilter = filter
        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            pickCustomTemplateFileInternal(chooser.selectedFile.absolutePath)
        }
    }

    internal fun pickCustomTemplateFileInternal(path: String) {
        customTemplatePath = path
        saveSettings()
    }

    fun toggleCustomTemplate(enabled: Boolean) {
        customTemplateEnabled = enabled
        saveSettings()
    }

    fun saveSettings() {
        registry?.let {
            val newRegistry = DocumentNodeDefinitionRegistry(
                definitions = it.definitions,
                customTemplateEnabled = customTemplateEnabled,
                customTemplatePath = customTemplatePath
            )
            try {
                newRegistry.save(repository)
                registry = newRegistry
            } catch (e: Exception) {
                errorMessage = "Erro ao salvar configurações: ${e.message}"
            }
        }
    }

    fun addComponent(definition: DocumentNodeDefinition) {
        val newNodeId = (System.currentTimeMillis() + (0..1000).random())
        val newNode = DocumentNode(newNodeId, definition.templateDocPath)
        
        document = document.addNode(selectedNodeId, newNode)
    }

    var pendingDeleteNode by mutableStateOf<DocumentNode?>(null)

    fun requestDeleteNode(node: DocumentNode) {
        if (node.children.isNotEmpty()) {
            pendingDeleteNode = node
        } else {
            confirmDeleteNode(node.id)
        }
    }

    fun confirmDeleteNode(nodeId: Long) {
        document = document.removeNode(nodeId)
        if (selectedNodeId == nodeId) {
            selectedNodeId = null
        }
        pendingDeleteNode = null
    }

    fun cancelDelete() {
        pendingDeleteNode = null
    }

    fun generateDocument() {
        errorMessage = null
        successMessage = null
        val missingTemplates = document.validateTemplates()

        if (missingTemplates.isNotEmpty()) {
            errorMessage = "Os seguintes templates não foram encontrados: ${missingTemplates.joinToString(", ")}"
            return
        }

        try {
            val baseTemplate = if (customTemplateEnabled && customTemplatePath.isNotBlank()) {
                customTemplatePath
            } else {
                null
            }
            document.render(DocxRenderer(), baseTemplate)
            successMessage = "Documento gerado com sucesso!"
        } catch (e: Exception) {
            errorMessage = "Erro ao gerar documento: ${e.message}"
        }
    }
}

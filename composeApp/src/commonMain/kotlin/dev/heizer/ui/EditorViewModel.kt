package dev.heizer.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.heizer.core.document.Document
import dev.heizer.core.document.DocumentNode
import dev.heizer.core.document.DocumentNodeDefinition
import dev.heizer.core.document.DocumentNodeDefinitionRegistry
import dev.heizer.core.document.renderer.docx.DocxRenderer
import dev.heizer.core.file.FileRepository
import dev.heizer.core.serializer.JsonSerializer
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class EditorViewModel {
    private val repository = FileRepository(JsonSerializer.create<DocumentNodeDefinitionRegistry>())

    var registry: DocumentNodeDefinitionRegistry =
        runCatching {
            DocumentNodeDefinitionRegistry.Factory.load(repository)
        }.getOrElse { ex ->
            print("Erro ao carregar configurações: ${ex.message}. Usando configurações padrão")
            DocumentNodeDefinitionRegistry.Factory.empty()
                .also {
                    it.save(repository)
                }
        }

    var document by mutableStateOf(Document.create("Novo Documento"))

    // Used in App.kt for modal/dialog message show TODO: Move to UI layer; fazer um hook/callback que envie as mensagens pro UI ao invés de manter aqui o estado
    var modalErrorMessage by mutableStateOf<String?>(null)
    var modalSuccessMessage by mutableStateOf<String?>(null)

    @OptIn(ExperimentalUuidApi::class)
    var selectedNodeId by mutableStateOf<Uuid?>(null)

    var isSettingsOpen by mutableStateOf(false)

    @OptIn(ExperimentalUuidApi::class)
    fun selectNode(id: Uuid) {
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
        registry.copy(customBaseTemplatePath = path)
            .also {
                registry = it
            }
            .also {
                saveSettings()
            }
    }

    fun setCustomTemplateEnabled(enabled: Boolean) =
        registry.copy(customBaseTemplateEnabled = enabled)
            .also {
                registry = it
            }
            .also {
                saveSettings()
            }

    fun saveSettings() = registry.save(repository)

    @OptIn(ExperimentalUuidApi::class)
    fun addComponent(definition: DocumentNodeDefinition) {
        document = document
            .addNode(
                selectedNodeId,
                DocumentNode(definition.templateDocPath)
            )
    }

    var pendingDeleteNode by mutableStateOf<DocumentNode?>(null)

    @OptIn(ExperimentalUuidApi::class)
    fun requestDeleteNode(node: DocumentNode) {
        if (node.children.isNotEmpty()) {
            pendingDeleteNode = node
        } else {
            confirmDeleteNode(node.id)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun confirmDeleteNode(nodeId: Uuid) {
        document = document.removeNode(nodeId)
        selectedNodeId = null
        pendingDeleteNode = null
    }

    fun cancelDelete() {
        pendingDeleteNode = null
    }

    private val renderer: DocxRenderer = DocxRenderer()

    fun generateDocument() {
        if (document.validateTemplates().isNotEmpty()) {
            modalErrorMessage = "Erro: Os modelos personalizados não foram encontrados: ${
                document.validateTemplates().joinToString(", ")
            }"
        }
        runCatching {
            document.render(renderer, getBaseDefinitions())
            modalSuccessMessage = "Documento gerado com sucesso!"
        }.getOrElse {
            modalErrorMessage = "Erro ao gerar documento: ${it.message}"
        }
    }

    fun getBaseDefinitions() =
        if (registry.customBaseTemplateEnabled && registry.customBaseTemplatePath.isNotBlank()) registry.customBaseTemplatePath
        else null

}

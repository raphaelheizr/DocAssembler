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

class EditorViewModel {
    var document by mutableStateOf(Document.create("Novo Documento"))
    
    var definitions by mutableStateOf<List<DocumentNodeDefinition>>(emptyList())

    var errorMessage by mutableStateOf<String?>(null)
    var successMessage by mutableStateOf<String?>(null)

    init {
        try {
            val repository = FileRepository(JsonSerializer.create<DocumentNodeDefinitionRegistry>())
            val registry = DocumentNodeDefinitionRegistry.Factory.load(repository)
            definitions = registry.definitions
        } catch (e: Exception) {
            errorMessage = "Erro ao carregar definições: ${e.message}"
        }
    }

    var selectedNodeId by mutableStateOf<Long?>(null)

    fun selectNode(id: Long) {
        selectedNodeId = id
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
            document.render(DocxRenderer())
            successMessage = "Documento gerado com sucesso!"
        } catch (e: Exception) {
            errorMessage = "Erro ao gerar documento: ${e.message}"
        }
    }
}

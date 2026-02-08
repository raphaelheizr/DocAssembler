package dev.heizer.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.heizer.core.document.Document
import dev.heizer.core.document.DocumentNodeDefinition
import dev.heizer.core.document.DocumentNode
import dev.heizer.core.document.renderer.DocxRenderer
import java.io.File

class EditorViewModel {
    var document by mutableStateOf(Document.create("Novo Documento"))
    
    var definitions by mutableStateOf(
        listOf(
            DocumentNodeDefinition("1", "Título", "Componente de título", "templates/title.docx"),
            DocumentNodeDefinition("2", "Parágrafo", "Componente de parágrafo", "templates/paragraph.docx"),
            DocumentNodeDefinition("3", "Assinatura", "Componente de assinatura", "templates/signature.docx")
        )
    )

    var selectedNodeId by mutableStateOf<Long?>(null)

    var errorMessage by mutableStateOf<String?>(null)

    fun selectNode(id: Long) {
        selectedNodeId = id
    }

    fun addComponent(definition: DocumentNodeDefinition) {
        val newNodeId = (System.currentTimeMillis() + (0..1000).random())
        val newNode = DocumentNode(newNodeId, definition.templateDocPath)
        
        document = document.addNode(selectedNodeId, newNode)
    }

    fun generateDocument() {
        errorMessage = null
        val missingTemplates = document.validateTemplates()

        if (missingTemplates.isNotEmpty()) {
            errorMessage = "Os seguintes templates não foram encontrados: ${missingTemplates.joinToString(", ")}"
            return
        }

        try {
            document.render(DocxRenderer())
        } catch (e: Exception) {
            errorMessage = "Erro ao gerar documento: ${e.message}"
        }
    }
}

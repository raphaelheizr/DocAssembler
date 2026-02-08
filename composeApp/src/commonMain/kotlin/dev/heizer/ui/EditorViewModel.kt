package dev.heizer.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.heizer.core.document.Document
import dev.heizer.core.document.DocumentNodeDefinition

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

    fun selectNode(id: Long) {
        selectedNodeId = id
    }
}

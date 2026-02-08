package dev.heizer.core.document

import kotlinx.serialization.Serializable

@Serializable
data class DocumentNode(
    val id: Long,
    val templatePath: String,
    val children: List<DocumentNode> = emptyList()
)
package dev.heizer.core.document

import kotlinx.serialization.Serializable

@Serializable
data class DocumentNodeDefinition(
    val id: String,
    val name: String,
    val description: String,
    val templateDocPath: String,
) : Comparable<DocumentNodeDefinition> {
    override fun compareTo(other: DocumentNodeDefinition):
            Int = id.compareTo(other.id)
}
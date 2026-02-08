package dev.heizer.core.document

import kotlinx.serialization.Serializable

@Serializable
data class DocumentNode(
    val id: Int,
    val relativePath: String,
) : Comparable<DocumentNode> {
    override fun compareTo(other: DocumentNode): Int =
        id.compareTo(other.id)

}
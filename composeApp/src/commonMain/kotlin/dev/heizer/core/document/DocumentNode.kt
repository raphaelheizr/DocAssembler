package dev.heizer.core.document

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class DocumentNode @OptIn(ExperimentalUuidApi::class) constructor(
    val id: Uuid,
    val templatePath: String,
    val children: List<DocumentNode> = emptyList()
) {
    @OptIn(ExperimentalUuidApi::class)
    constructor(templatePath: String)
            : this(Uuid.generateV7(), templatePath, emptyList())

}
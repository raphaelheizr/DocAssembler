package dev.heizer.core.document.renderer

import dev.heizer.core.document.Document

interface DocumentRenderer {
    fun render(document: Document, path: String, baseTemplatePath: String? = null)
}
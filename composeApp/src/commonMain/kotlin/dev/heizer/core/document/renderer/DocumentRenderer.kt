package dev.heizer.core.document.renderer

import dev.heizer.core.document.Document

fun interface DocumentRenderer {
    fun render(document: Document, string: String)
}
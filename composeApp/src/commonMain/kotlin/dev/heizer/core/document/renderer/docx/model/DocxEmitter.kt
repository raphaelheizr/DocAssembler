package dev.heizer.core.document.renderer.docx.model

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFStyle
import org.apache.poi.xwpf.usermodel.Document
import org.apache.poi.util.Units

class DocxEmitter(private val styleRegistry: StyleRegistry) {
    fun emit(nodes: List<DocNode>, baseTemplatePath: String? = null): XWPFDocument {
        val doc = baseTemplatePath?.let { path ->
            java.io.File(path).takeIf { it.exists() }?.let { file ->
                java.io.FileInputStream(file).use { XWPFDocument(it) }
            }
        } ?: XWPFDocument()

        applyStyles(doc)

        nodes.forEach { emitNode(it, doc) }
        return doc
    }

    private fun applyStyles(doc: XWPFDocument) {
        val styles = doc.styles ?: doc.createStyles()
        styleRegistry.getAllStyles().forEach { ctStyle ->
            val styleId = ctStyle.styleId
            if (styles.getStyle(styleId) == null) {
                try {
                    // Create a new XWPFStyle from the CTStyle
                    val newStyle = XWPFStyle(ctStyle)
                    styles.addStyle(newStyle)
                } catch (e: Exception) {
                    // Ignore if style cannot be added
                }
            }
        }
    }

    private fun emitNode(node: DocNode, doc: XWPFDocument) {
        when (node) {
            is GenericNode -> {
                doc.document.body.newCursor().use { cursor ->
                    cursor.toEndToken()
                    node.xmlObject.newCursor().use { srcCursor ->
                        srcCursor.copyXml(cursor)
                    }
                }
            }

            is ParagraphNode -> emitParagraph(node, doc.createParagraph())
            is PlaceholderNode -> { /* Já resolvido */
            }

            else -> {}
        }
    }

    private fun emitParagraph(node: ParagraphNode, p: org.apache.poi.xwpf.usermodel.XWPFParagraph) {
        node.pPr?.let { p.ctp.pPr = it }
        node.styleId?.let { p.style = it }

        node.runs.forEach { runNode ->
            val r = p.createRun()
            runNode.rPr?.let { r.ctr.rPr = it }
            runNode.styleId?.let { r.style = it }
            r.setText(runNode.text, 0)

            runNode.images.forEach { imageNode ->
                val format = when (imageNode.extension.lowercase()) {
                    "png" -> Document.PICTURE_TYPE_PNG
                    "jpg", "jpeg" -> Document.PICTURE_TYPE_JPEG
                    "gif" -> Document.PICTURE_TYPE_GIF
                    "bmp" -> Document.PICTURE_TYPE_BMP
                    "wmf" -> Document.PICTURE_TYPE_WMF
                    else -> Document.PICTURE_TYPE_PNG
                }
                try {
                    r.addPicture(
                        imageNode.data.inputStream(),
                        format,
                        "image.${imageNode.extension}",
                        imageNode.width.toInt(),
                        imageNode.height.toInt()
                    )
                } catch (e: Exception) {
                    // Log error or handle
                }
            }
        }
    }
}

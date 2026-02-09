package dev.heizer.core.document.renderer.docx.model

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFStyle
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.apache.poi.xwpf.usermodel.XWPFTableCell
import org.apache.poi.xwpf.usermodel.Document
import org.apache.poi.util.Units

class DocxEmitter(private val styleRegistry: StyleRegistry) {
    fun emit(nodes: List<DocNode>, baseTemplatePath: String? = null): XWPFDocument {
        val doc = if (baseTemplatePath != null) {
            val file = java.io.File(baseTemplatePath)
            if (file.exists()) {
                java.io.FileInputStream(file).use { XWPFDocument(it) }
            } else {
                XWPFDocument()
            }
        } else {
            XWPFDocument()
        }
        applyStyles(doc)

        for (node in nodes) {
            emitNode(node, doc)
        }
        return doc
    }

    private fun applyStyles(doc: XWPFDocument) {
        val styles = doc.createStyles()
        for (style in styleRegistry.getAllStyles()) {
            styles.addStyle(XWPFStyle(style))
        }
    }

    private fun emitNode(node: DocNode, doc: XWPFDocument) {
        when (node) {
            is GenericNode -> {
                val cursor = doc.document.body.newCursor()
                cursor.toEndToken()
                val srcCursor = node.xmlObject.newCursor()
                srcCursor.copyXml(cursor)
                srcCursor.dispose()
                cursor.dispose()
            }

            is ParagraphNode -> {
                val p = doc.createParagraph()
                emitParagraph(node, p)
            }

            is PlaceholderNode -> {
                // Já deve ter sido resolvido
            }

            else -> {}
        }
    }

    private fun emitParagraph(node: ParagraphNode, p: org.apache.poi.xwpf.usermodel.XWPFParagraph) {
        if (node.pPr != null) {
            p.ctp.pPr = node.pPr
        }
        if (node.styleId != null) {
            p.style = node.styleId
        }
        for (runNode in node.runs) {
            val r = p.createRun()
            if (runNode.rPr != null) {
                r.ctr.rPr = runNode.rPr
            }
            if (runNode.styleId != null) {
                r.style = runNode.styleId
            }
            r.setText(runNode.text, 0)
            for (imageNode in runNode.images) {
                val format = when (imageNode.extension.lowercase()) {
                    "png" -> Document.PICTURE_TYPE_PNG
                    "jpg", "jpeg" -> Document.PICTURE_TYPE_JPEG
                    "gif" -> Document.PICTURE_TYPE_GIF
                    "bmp" -> Document.PICTURE_TYPE_BMP
                    "wmf" -> Document.PICTURE_TYPE_WMF
                    else -> Document.PICTURE_TYPE_PNG
                }
                try {
                    // Nota: Dimensões fixas para simplificar, idealmente deveriam vir do template
                    r.addPicture(
                        imageNode.data.inputStream(),
                        format,
                        "image.${imageNode.extension}",
                        Units.toEMU(300.0),
                        Units.toEMU(200.0)
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun emitNestedNode(node: DocNode, cell: XWPFTableCell) {
        when (node) {
            is ParagraphNode -> {
                val p = cell.addParagraph()
                emitParagraph(node, p)
            }
            is GenericNode -> {
                val cursor = cell.ctTc.newCursor()
                cursor.toEndToken()
                val srcCursor = node.xmlObject.newCursor()
                srcCursor.copyXml(cursor)
                srcCursor.dispose()
                cursor.dispose()
            }
            else -> {}
        }
    }
}

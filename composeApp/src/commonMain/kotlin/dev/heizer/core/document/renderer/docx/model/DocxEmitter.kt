package dev.heizer.core.document.renderer.docx.model

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFStyle

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
            is ParagraphNode -> {
                val p = doc.createParagraph()
                p.ctp.pPr = node.pPr
                if (node.styleId != null) {
                    p.style = node.styleId
                }
                for (runNode in node.runs) {
                    val r = p.createRun()
                    r.ctr.rPr = runNode.rPr
                    if (runNode.styleId != null) {
                        r.style = runNode.styleId
                    }
                    r.setText(runNode.text, 0)
                }
            }

            is PlaceholderNode -> {
                // Em teoria, placeholders já deveriam ter sido resolvidos pelo engine.
                // Se sobrou um aqui, apenas ignoramos ou avisamos.
            }

            else -> {}
        }
    }
}

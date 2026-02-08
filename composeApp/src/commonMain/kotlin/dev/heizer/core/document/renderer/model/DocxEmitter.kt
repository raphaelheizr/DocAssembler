package dev.heizer.core.document.renderer.model

import org.apache.poi.xwpf.usermodel.XWPFDocument

class DocxEmitter(private val styleRegistry: StyleRegistry) {
    fun emit(nodes: List<DocNode>): XWPFDocument {
        val doc = XWPFDocument()
        applyStyles(doc)

        for (node in nodes) {
            emitNode(node, doc)
        }
        return doc
    }

    private fun applyStyles(doc: XWPFDocument) {
        val styles = doc.createStyles()
        for (style in styleRegistry.getAllStyles()) {
            styles.addStyle(org.apache.poi.xwpf.usermodel.XWPFStyle(style))
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

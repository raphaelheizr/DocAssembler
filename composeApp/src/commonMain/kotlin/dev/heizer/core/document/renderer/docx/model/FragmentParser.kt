package dev.heizer.core.document.renderer.docx.model

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr

class FragmentParser(private val styleRegistry: StyleRegistry) {
    fun parse(doc: XWPFDocument): List<DocNode> {
        val nodes = mutableListOf<DocNode>()
        for (p in doc.paragraphs) {
            if (p.text.contains("{%}")) {
                if (p.text.trim() == "{%}") {
                    nodes.add(PlaceholderNode(p.ctp.pPr, p.runs.find { it.text().contains("{%}") }?.ctr?.rPr))
                } else {
                    nodes.add(parseInlinePlaceholder(p, doc))
                }
            } else {
                nodes.add(parseParagraph(p, doc))
            }
        }
        return nodes
    }

    private fun parseParagraph(p: XWPFParagraph, doc: XWPFDocument): ParagraphNode {
        val styleId = p.style?.let { styleRegistry.ensure(it, doc) }
        return ParagraphNode(
            styleId,
            p.ctp.pPr,
            p.runs.map { r ->
                val rStyleId = r.style?.let { styleRegistry.ensure(it, doc) }
                RunNode(r.text(), rStyleId, r.ctr.rPr)
            }
        )
    }

    private fun parseInlinePlaceholder(p: XWPFParagraph, doc: XWPFDocument): DocNode {
        // Para simplificar a interpolação inline no AST, vamos tratar o parágrafo com {%} 
        // como uma sequência de nós, onde um deles é um marcador especial de run-placeholder.
        // No entanto, para seguir o modelo sugerido, podemos manter ParagraphNode mas com um RunNode especial ou similar.
        // Mas a proposta sugere PlaceholderNode. Vamos adaptar: 
        // Se for inline, talvez precisemos de um InlinePlaceholderNode.
        
        val styleId = p.style?.let { styleRegistry.ensure(it, doc) }
        val runs = mutableListOf<RunNode>()
        var placeholderRPr: CTRPr? = null

        for (r in p.runs) {
            val text = r.text()
            if (text.contains("{%}")) {
                placeholderRPr = r.ctr.rPr
                // Dividimos o run em partes se necessário, ou apenas marcamos este run como especial.
                // Para simplificar o engine, vamos apenas colocar o texto com {%} e deixar o engine resolver.
            }
            val rStyleId = r.style?.let { styleRegistry.ensure(it, doc) }
            runs.add(RunNode(text, rStyleId, r.ctr.rPr))
        }

        return ParagraphNode(styleId, p.ctp.pPr, runs)
    }
}

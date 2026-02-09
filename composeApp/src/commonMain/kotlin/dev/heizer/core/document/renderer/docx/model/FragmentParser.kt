package dev.heizer.core.document.renderer.docx.model

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.apache.poi.xwpf.usermodel.BodyElementType
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr

class FragmentParser(private val styleRegistry: StyleRegistry) {
    fun parse(doc: XWPFDocument): List<DocNode> {
        val nodes = mutableListOf<DocNode>()
        for (element in doc.bodyElements) {
            when (element.elementType) {
                BodyElementType.PARAGRAPH -> {
                    val p = element as XWPFParagraph
                    if (p.text.contains("{%}")) {
                        if (p.text.trim() == "{%}") {
                            nodes.add(PlaceholderNode(p.ctp.pPr, p.runs.find { it.text().contains("{%}") }?.ctr?.rPr))
                        } else {
                            nodes.add(parseInlinePlaceholder(p, doc))
                        }
                    } else {
                        // Se não tem placeholder, usamos parseParagraph para garantir que estilos e imagens sejam extraídos corretamente.
                        // O uso de GenericNode aqui estava causando regressão na aplicação de estilos.
                        nodes.add(parseParagraph(p, doc))
                    }
                }
                BodyElementType.TABLE -> {
                    val table = element as XWPFTable
                    nodes.add(GenericNode(table.ctTbl))
                }
                else -> {
                    // Outros elementos (ex: SDT, etc)
                    try {
                        val method = element.javaClass.getMethod("getCtp")
                        nodes.add(GenericNode(method.invoke(element) as org.apache.xmlbeans.XmlObject))
                    } catch (e: Exception) {
                        try {
                            val method = element.javaClass.getMethod("getCTTbl")
                            nodes.add(GenericNode(method.invoke(element) as org.apache.xmlbeans.XmlObject))
                        } catch (e2: Exception) {
                            // Fallback se não conseguirmos pegar o CT object facilmente
                        }
                    }
                }
            }
        }
        return nodes
    }

    // Remover parseTable e simplificar o resto se possível, mas manter parseParagraph/parseInlinePlaceholder 
    // para quando houver interpolação.

    // Remover parseTable pois agora usamos GenericNode

    private fun parseParagraph(p: XWPFParagraph, doc: XWPFDocument): ParagraphNode {
        val styleId = p.style?.let { styleRegistry.getStyleId(it, doc) }
        val runs = p.runs.map { r ->
            val rStyleId = r.style?.let { styleRegistry.getStyleId(it, doc) }
            val pictureNodes = r.embeddedPictures.map { pic ->
                ImageNode(pic.pictureData.data, pic.pictureData.suggestFileExtension())
            }
            RunNode(r.text(), rStyleId, r.ctr.rPr, pictureNodes)
        }
        return ParagraphNode(
            styleId,
            p.ctp.pPr,
            runs
        )
    }

    private fun parseInlinePlaceholder(p: XWPFParagraph, doc: XWPFDocument): DocNode {
        // Para simplificar a interpolação inline no AST, vamos tratar o parágrafo com {%} 
        // como uma sequência de nós, onde um deles é um marcador especial de run-placeholder.
        // No entanto, para seguir o modelo sugerido, podemos manter ParagraphNode mas com um RunNode especial ou similar.
        // Mas a proposta sugere PlaceholderNode. Vamos adaptar: 
        // Se for inline, talvez precisemos de um InlinePlaceholderNode.
        
        val styleId = p.style?.let { styleRegistry.getStyleId(it, doc) }
        val runs = mutableListOf<RunNode>()
        var placeholderRPr: CTRPr? = null

        for (r in p.runs) {
            val text = r.text()
            if (text.contains("{%}")) {
                placeholderRPr = r.ctr.rPr
            }
            val rStyleId = r.style?.let { styleRegistry.getStyleId(it, doc) }
            val images = r.embeddedPictures.map { pic ->
                ImageNode(pic.pictureData.data, pic.pictureData.suggestFileExtension())
            }
            runs.add(RunNode(text, rStyleId, r.ctr.rPr, images))
        }

        return ParagraphNode(styleId, p.ctp.pPr, runs)
    }
}

package dev.heizer.core.document.renderer.docx.model

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.apache.poi.xwpf.usermodel.BodyElementType
import org.apache.poi.xwpf.usermodel.IBodyElement
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr

interface BodyElementParser {
    fun canParse(element: IBodyElement): Boolean
    fun parse(element: IBodyElement, doc: XWPFDocument, styleRegistry: StyleRegistry, parser: FragmentParser): DocNode?
}

class ParagraphElementParser : BodyElementParser {
    override fun canParse(element: IBodyElement): Boolean = element.elementType == BodyElementType.PARAGRAPH

    override fun parse(element: IBodyElement, doc: XWPFDocument, styleRegistry: StyleRegistry, parser: FragmentParser): DocNode {
        val p = element as XWPFParagraph
        return if (p.text.trim() == "{%}") {
            PlaceholderNode(p.ctp.pPr, p.runs.find { it.text().contains("{%}") }?.ctr?.rPr)
        } else {
            val styleId = p.style?.let { styleRegistry.getStyleId(it, doc) }
            val runs = p.runs.map { r ->
                val rStyleId = r.style?.let { styleRegistry.getStyleId(it, doc) }
                val images = r.embeddedPictures.map { pic ->
                    ImageNode(
                        pic.pictureData.data,
                        pic.pictureData.suggestFileExtension(),
                        pic.ctPicture.spPr.xfrm.ext.cx / 9525.0,
                        pic.ctPicture.spPr.xfrm.ext.cy / 9525.0
                    )
                }
                RunNode(r.text(), rStyleId, r.ctr.rPr, images)
            }
            ParagraphNode(styleId, p.ctp.pPr, runs)
        }
    }
}

class TableElementParser : BodyElementParser {
    override fun canParse(element: IBodyElement): Boolean = element.elementType == BodyElementType.TABLE

    override fun parse(element: IBodyElement, doc: XWPFDocument, styleRegistry: StyleRegistry, parser: FragmentParser): DocNode {
        val table = element as XWPFTable
        table.styleID?.let { styleRegistry.getStyleId(it, doc) }
        return GenericNode(table.ctTbl)
    }
}

class SdtElementParser : BodyElementParser {
    override fun canParse(element: IBodyElement): Boolean = element.elementType == BodyElementType.CONTENTCONTROL

    override fun parse(element: IBodyElement, doc: XWPFDocument, styleRegistry: StyleRegistry, parser: FragmentParser): DocNode? =
        try {
            val method = element.javaClass.getMethod("getCtSdt")
            GenericNode(method.invoke(element) as org.apache.xmlbeans.XmlObject)
        } catch (e: Exception) {
            null
        }
}

class FallbackElementParser : BodyElementParser {
    override fun canParse(element: IBodyElement): Boolean = true

    override fun parse(element: IBodyElement, doc: XWPFDocument, styleRegistry: StyleRegistry, parser: FragmentParser): DocNode? =
        runCatching {
            val method = element.javaClass.getMethod("getCtp")
            GenericNode(method.invoke(element) as org.apache.xmlbeans.XmlObject)
        }.getOrElse {
            runCatching {
                val method = element.javaClass.getMethod("getCTTbl")
                GenericNode(method.invoke(element) as org.apache.xmlbeans.XmlObject)
            }.getOrNull()
        }
}

class FragmentParser(private val styleRegistry: StyleRegistry) {
    private val parsers = listOf(
        ParagraphElementParser(),
        TableElementParser(),
        SdtElementParser(),
        FallbackElementParser()
    )

    fun parse(doc: XWPFDocument): List<DocNode> {
        return doc.bodyElements.mapNotNull { element ->
            parsers.find { it.canParse(element) }?.parse(element, doc, styleRegistry, this)
        }
    }
}

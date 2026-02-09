package dev.heizer.core.document.renderer.docx.model

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.xmlbeans.XmlObject
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*

sealed class DocNode

data class GenericNode(
    val xmlObject: XmlObject
) : DocNode()

data class ParagraphNode(
    val styleId: String?,
    val pPr: CTPPr?,
    val runs: List<RunNode>
) : DocNode()

data class RunNode(
    val text: String,
    val styleId: String?,
    val rPr: CTRPr?,
    val images: List<ImageNode> = emptyList()
) : DocNode()

data class ImageNode(
    val data: ByteArray,
    val extension: String
) : DocNode() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageNode

        if (!data.contentEquals(other.data)) return false
        if (extension != other.extension) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + extension.hashCode()
        return result
    }
}

data class PlaceholderNode(
    val originalParagraphPPr: CTPPr?,
    val originalRunRPr: CTRPr?
) : DocNode()

class StyleRegistry {
    private val styles = mutableMapOf<String, CTStyle>()

    fun register(styleId: String, styleDef: CTStyle) {
        styles.putIfAbsent(styleId, styleDef)
    }

    fun getAllStyles(): Collection<CTStyle> = styles.values

    fun getStyleId(styleId: String, sourceDoc: XWPFDocument): String {
        if (!styles.containsKey(styleId)) {
            val sourceStyles = sourceDoc.styles
            if (sourceStyles != null) {
                val style = sourceStyles.getStyle(styleId)
                if (style != null) {
                    register(styleId, style.ctStyle)
                }
            }
        }
        return styleId
    }
}

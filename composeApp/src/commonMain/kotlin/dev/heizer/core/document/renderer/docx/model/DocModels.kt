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
    val extension: String,
    val width: Long,
    val height: Long
) : DocNode() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageNode

        if (!data.contentEquals(other.data)) return false
        if (extension != other.extension) return false
        if (width != other.width) return false
        if (height != other.height) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + extension.hashCode()
        result = 31 * result + width.hashCode()
        result = 31 * result + height.hashCode()
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
            sourceDoc.styles?.let { sourceStyles ->
                val style = try {
                    sourceStyles.getStyle(styleId)
                } catch (e: Exception) {
                    null
                }
                if (style != null) {
                    register(styleId, style.ctStyle.copy() as CTStyle)
                } else {
                    // Fallback: Tenta iterar sobre os estilos se getStyle falhar por algum motivo de ID vs Name
                    try {
                        val method = sourceStyles.javaClass.methods.find { it.name == "getUsedStyleList" }
                        val usedStyles = method?.let {
                            if (it.parameterCount == 0) it.invoke(sourceStyles) as? List<*> else null
                        }

                        usedStyles?.filterIsInstance<org.apache.poi.xwpf.usermodel.XWPFStyle>()
                            ?.find { it.styleId == styleId || it.name == styleId }
                            ?.let { register(it.styleId, it.ctStyle.copy() as CTStyle) }
                    } catch (e: Exception) {
                        // Ignora se falhar por reflexão
                    }
                }
            }
        }
        return styleId
    }
}

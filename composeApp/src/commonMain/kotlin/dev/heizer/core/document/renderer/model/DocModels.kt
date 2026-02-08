package dev.heizer.core.document.renderer.model

import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle

sealed class DocNode

data class ParagraphNode(
    val styleId: String?,
    val pPr: CTPPr?,
    val runs: List<RunNode>
) : DocNode()

data class RunNode(
    val text: String,
    val styleId: String?,
    val rPr: CTRPr?
) : DocNode()

data class PlaceholderNode(
    val originalParagraphPPr: CTPPr?,
    val originalRunRPr: CTRPr?
) : DocNode()

class StyleRegistry {
    private val styles = mutableMapOf<String, CTStyle>()

    fun register(styleId: String, styleDef: CTStyle) {
        styles.putIfAbsent(styleId, styleDef)
    }

    fun getStyle(styleId: String): CTStyle? = styles[styleId]
    
    fun getAllStyles(): Collection<CTStyle> = styles.values

    fun ensure(styleId: String, sourceDoc: org.apache.poi.xwpf.usermodel.XWPFDocument): String {
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

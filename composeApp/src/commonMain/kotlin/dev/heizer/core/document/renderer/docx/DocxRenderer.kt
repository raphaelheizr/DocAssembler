package dev.heizer.core.document.renderer.docx

import dev.heizer.core.document.Document
import dev.heizer.core.document.DocumentNode
import dev.heizer.core.document.renderer.DocumentRenderer
import dev.heizer.core.document.renderer.docx.model.DocNode
import dev.heizer.core.document.renderer.docx.model.DocxEmitter
import dev.heizer.core.document.renderer.docx.model.FragmentParser
import dev.heizer.core.document.renderer.docx.model.InterpolationEngine
import dev.heizer.core.document.renderer.docx.model.ParagraphNode
import dev.heizer.core.document.renderer.docx.model.PlaceholderNode
import dev.heizer.core.document.renderer.docx.model.StyleRegistry
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class DocxRenderer : DocumentRenderer {

    override fun render(document: Document, path: String, baseTemplatePath: String?) {
        val styleRegistry = StyleRegistry()
        val parser = FragmentParser(styleRegistry)
        val engine = InterpolationEngine()
        val emitter = DocxEmitter(styleRegistry)

        val rootNodes = document.nodes.flatMap { node ->
            renderNodeToAST(node, parser, engine)
        }

        val resultDocument = emitter.emit(rootNodes, baseTemplatePath)

        val file = File(path)
        file.parentFile?.mkdirs()

        FileOutputStream(file)
            .use { out ->
                resultDocument.write(out)
            }

        resultDocument.close()
    }

    private fun renderNodeToAST(
        node: DocumentNode,
        parser: FragmentParser,
        engine: InterpolationEngine
    ): List<DocNode> {
        val templateDoc = loadTemplate(node.templatePath)
        val templateNodes = templateDoc.use { parser.parse(it) }

        if (node.children.isNotEmpty()) {
            val hasPlaceholder = templateNodes.any { 
                it is PlaceholderNode || (it is ParagraphNode && it.runs.any { r -> r.text.contains("{%}") })
            }
            if (!hasPlaceholder) {
                throw IllegalStateException("Não é possível interpolar sem a sequência {%} no template: ${node.templatePath}")
            }
        }

        if (node.children.isEmpty()) {
            // Se não tem filhos, apenas removemos o placeholder se existir
            return engine.interpolate(templateNodes, emptyMap())
        }

        val childrenAST = node.children.flatMap { child ->
            renderNodeToAST(child, parser, engine)
        }

        // Criar mapa de interpolação
        // Para simplificar, associamos todos os placeholders deste template aos filhos deste node
        val placeholders = templateNodes.filterIsInstance<PlaceholderNode>()
        val inlinePlaceholders = templateNodes.filterIsInstance<ParagraphNode>().filter { p -> p.runs.any { it.text.contains("{%}") } }
        
        val childrenMap = mutableMapOf<PlaceholderNode, List<DocNode>>()
        
        if (placeholders.isNotEmpty()) {
            placeholders.forEach { childrenMap[it] = childrenAST }
        }
        
        if (inlinePlaceholders.isNotEmpty()) {
            inlinePlaceholders.forEach { p ->
                val virtualPlaceholder = PlaceholderNode(p.pPr, p.runs.find { it.text.contains("{%}") }?.rPr)
                childrenMap[virtualPlaceholder] = childrenAST
            }
        }

        return engine.interpolate(templateNodes, childrenMap)
    }

    private fun loadTemplate(path: String): XWPFDocument {
        val templateFile = File(path)
        require(templateFile.exists()) { "Template não encontrado: $path" }
        return FileInputStream(templateFile).use { XWPFDocument(it) }
    }
}
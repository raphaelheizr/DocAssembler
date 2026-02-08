package dev.heizer.core.document.renderer

import dev.heizer.core.document.Document
import dev.heizer.core.document.DocumentNode
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFRun
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class DocxRenderer : DocumentRenderer {

    override fun render(document: Document, path: String) {
        val resultDocument = XWPFDocument()

        document.nodes.forEach { node ->
            renderNodeTo(node, resultDocument)
        }

        val file = File(path)
        file.parentFile?.mkdirs()

        FileOutputStream(file)
            .use { out ->
                resultDocument.write(out)
            }

        resultDocument.close()
    }

    private fun renderNodeTo(node: DocumentNode, targetDoc: XWPFDocument, parentStyles: ParentStyles? = null) {
        val templateDoc = loadTemplate(node.templatePath)
        templateDoc.use { doc ->
            if (node.children.isEmpty()) {
                appendDocument(targetDoc, doc, parentStyles)
            } else {
                renderWithInterpolation(node, doc, targetDoc, parentStyles)
            }
        }
    }

    data class ParentStyles(
        val paragraphProperties: org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr?,
        val runProperties: org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr?
    )

    private fun loadTemplate(path: String): XWPFDocument {
        val templateFile = File(path)
        require(templateFile.exists()) { "Template não encontrado: $path" }
        return FileInputStream(templateFile).use { XWPFDocument(it) }
    }

    private fun renderWithInterpolation(
        node: DocumentNode,
        templateDoc: XWPFDocument,
        targetDoc: XWPFDocument,
        parentStyles: ParentStyles? = null
    ) {
        val placeholderParagraph = findPlaceholderParagraph(templateDoc)
            ?: throw IllegalStateException("Não é possível interpolar sem a sequência {%} no template: ${node.templatePath}")

        val elementPos = templateDoc.getPosOfParagraph(placeholderParagraph)
        val bodyElements = templateDoc.bodyElements

        // Copiar parágrafos ANTES do placeholder
        copyParagraphsInRange(templateDoc, targetDoc, 0 until elementPos, parentStyles)

        // Tratar o parágrafo do placeholder
        processPlaceholder(node, placeholderParagraph, targetDoc, parentStyles)

        // Copiar parágrafos DEPOIS do placeholder
        copyParagraphsInRange(templateDoc, targetDoc, (elementPos + 1) until bodyElements.size, parentStyles)
    }

    private fun findPlaceholderParagraph(doc: XWPFDocument): XWPFParagraph? {
        return doc.paragraphs.find { it.text.contains("{%}") }
    }

    private fun copyParagraphsInRange(source: XWPFDocument, target: XWPFDocument, range: IntRange, parentStyles: ParentStyles? = null) {
        val bodyElements = source.bodyElements
        for (i in range) {
            val element = bodyElements.getOrNull(i)
            if (element is XWPFParagraph) {
                copyParagraph(element, target.createParagraph())
            }
        }
    }

    private fun processPlaceholder(
        node: DocumentNode,
        placeholderParagraph: XWPFParagraph,
        targetDoc: XWPFDocument,
        parentStyles: ParentStyles? = null
    ) {
        val pText = placeholderParagraph.text
        
        // Determinar os estilos que serão passados para os filhos
        // Se já temos parentStyles vindo de cima, continuamos usando-os (conforme requisito de usar estilo do documento-pai)
        // Se não temos, os estilos deste placeholder (que é o pai para os próximos) serão os novos parentStyles.
        val currentStyles = parentStyles ?: ParentStyles(
            placeholderParagraph.ctp.pPr,
            placeholderParagraph.runs.find { (it.getText(0) ?: "").contains("{%}") }?.ctr?.rPr
        )

        if (pText.trim() == "{%}") {
            if (node.children.isNotEmpty()) {
                node.children.forEach { renderNodeTo(it, targetDoc, currentStyles) }
            }
        } else {
            renderInlinePlaceholder(node, placeholderParagraph, targetDoc, currentStyles)
        }
    }

    private fun renderInlinePlaceholder(
        node: DocumentNode,
        placeholderParagraph: XWPFParagraph,
        targetDoc: XWPFDocument,
        parentStyles: ParentStyles
    ) {
        val newP = targetDoc.createParagraph()
        newP.ctp.pPr = placeholderParagraph.ctp.pPr

        var foundInRun = false
        for (run in placeholderParagraph.runs) {
            val runText = run.getText(0) ?: ""
            if (!foundInRun && runText.contains("{%}")) {
                foundInRun = true
                processRunLeavingPlaceholder(node, run, newP, targetDoc, placeholderParagraph, parentStyles)
            } else if (foundInRun) {
                // Se já processamos o run do placeholder, os runs subsequentes devem ir para o ÚLTIMO parágrafo gerado
                // (que pode ser o newP ou um novo parágrafo criado após um interpolado)
                val lastP = targetDoc.paragraphs.last()
                copyRun(run, lastP.createRun())
            } else {
                // Se o run CONTÉM {%}, ele deve ser processado pela lógica de interpolação.
                // Mas o loop atual verifica apenas se foundInRun já é true.
                // Se o run atual tiver {%} e for o primeiro, ele entra no primeiro IF.
                copyRun(run, newP.createRun())
            }
        }
    }

    private fun processRunLeavingPlaceholder(
        node: DocumentNode,
        run: XWPFRun,
        currentP: XWPFParagraph,
        targetDoc: XWPFDocument,
        placeholderParagraph: XWPFParagraph,
        parentStyles: ParentStyles
    ) {
        val runText = run.getText(0) ?: ""
        // Substituir apenas a primeira ocorrência do placeholder para evitar problemas se houver múltiplos (embora não esperado)
        val placeholder = "{%}"
        val parts = runText.split(placeholder, limit = 2)

        // Parte antes do placeholder no mesmo run
        if (parts[0].isNotEmpty()) {
            val rBefore = currentP.createRun()
            copyRunStyles(run, rBefore)
            rBefore.setText(parts[0], 0)
        }

        // Interpolar filhos
        if (node.children.isNotEmpty()) {
            node.children.forEach { renderNodeTo(it, targetDoc, parentStyles) }
        }

        // Parte depois do placeholder no mesmo run
        if (parts.size > 1 && parts[1].isNotEmpty()) {
            val rAfterP = targetDoc.createParagraph()
            rAfterP.ctp.pPr = placeholderParagraph.ctp.pPr
            val rAfter = rAfterP.createRun()
            copyRunStyles(run, rAfter)
            rAfter.setText(parts[1], 0)
        } else if (node.children.isEmpty() && parts[0].isEmpty() && currentP.runs.isEmpty()) {
            // Se não tem filhos, o prefixo é vazio, e não há outros runs no parágrafo atual,
            // então este parágrafo que criamos para o inline está vazio e deve ser removido
            // para que {%} não deixe um rastro de linha vazia.
            val pos = targetDoc.getPosOfParagraph(currentP)
            if (pos != -1) {
                targetDoc.removeBodyElement(pos)
            }
        }
    }

    private fun appendDocument(target: XWPFDocument, source: XWPFDocument, parentStyles: ParentStyles? = null) {
        for (element in source.bodyElements) {
            if (element is XWPFParagraph) {
                val newP = target.createParagraph()
                copyParagraph(element, newP)
            }
            // Outros elementos como XWPFTable poderiam ser adicionados aqui
        }
    }

    private fun copyParagraph(source: XWPFParagraph, target: XWPFParagraph) {
        target.ctp.pPr = source.ctp.pPr
        
        for (run in source.runs) {
            val targetRun = target.createRun()
            copyRun(run, targetRun)
        }
    }

    private fun copyRun(source: XWPFRun, target: XWPFRun) {
        copyRunStyles(source, target)
        val text = source.getText(0) ?: ""
        target.setText(text.replace("{%}", ""))
    }

    private fun copyRunStyles(source: XWPFRun, target: XWPFRun) {
        target.ctr.rPr = source.ctr.rPr
    }
}
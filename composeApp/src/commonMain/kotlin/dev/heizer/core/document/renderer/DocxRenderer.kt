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

    private fun renderNodeTo(node: DocumentNode, targetDoc: XWPFDocument) {
        val templateDoc = loadTemplate(node.templatePath)
        templateDoc.use { doc ->
            if (node.children.isEmpty()) {
                appendDocument(targetDoc, doc)
            } else {
                renderWithInterpolation(node, doc, targetDoc)
            }
        }
    }

    private fun loadTemplate(path: String): XWPFDocument {
        val templateFile = File(path)
        require(!templateFile.exists()) { "Template não encontrado: $path" }
        return FileInputStream(templateFile).use { XWPFDocument(it) }
    }

    private fun renderWithInterpolation(
        node: DocumentNode,
        templateDoc: XWPFDocument,
        targetDoc: XWPFDocument
    ) {
        val placeholderParagraph = findPlaceholderParagraph(templateDoc)
            ?: throw IllegalStateException("Não é possível interpolar sem a sequência {%} no template: ${node.templatePath}")

        val elementPos = templateDoc.getPosOfParagraph(placeholderParagraph)
        val bodyElements = templateDoc.bodyElements

        // Copiar parágrafos ANTES do placeholder
        copyParagraphsInRange(templateDoc, targetDoc, 0 until elementPos)

        // Tratar o parágrafo do placeholder
        processPlaceholder(node, placeholderParagraph, targetDoc)

        // Copiar parágrafos DEPOIS do placeholder
        copyParagraphsInRange(templateDoc, targetDoc, (elementPos + 1) until bodyElements.size)
    }

    private fun findPlaceholderParagraph(doc: XWPFDocument): XWPFParagraph? {
        return doc.paragraphs.find { it.text.contains("{%}") }
    }

    private fun copyParagraphsInRange(source: XWPFDocument, target: XWPFDocument, range: IntRange) {
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
        targetDoc: XWPFDocument
    ) {
        val pText = placeholderParagraph.text
        if (pText.trim() == "{%}") {
            node.children.forEach { renderNodeTo(it, targetDoc) }
        } else {
            renderInlinePlaceholder(node, placeholderParagraph, targetDoc)
        }
    }

    private fun renderInlinePlaceholder(
        node: DocumentNode,
        placeholderParagraph: XWPFParagraph,
        targetDoc: XWPFDocument
    ) {
        val newP = targetDoc.createParagraph()
        newP.ctp.pPr = placeholderParagraph.ctp.pPr

        var foundInRun = false
        for (run in placeholderParagraph.runs) {
            val runText = run.getText(0) ?: ""
            if (!foundInRun && runText.contains("{%}")) {
                foundInRun = true
                processRunLeavingPlaceholder(node, run, newP, targetDoc, placeholderParagraph)
            } else if (foundInRun) {
                val lastP = targetDoc.paragraphs.last()
                copyRun(run, lastP.createRun())
            } else {
                copyRun(run, newP.createRun())
            }
        }
    }

    private fun processRunLeavingPlaceholder(
        node: DocumentNode,
        run: XWPFRun,
        currentP: XWPFParagraph,
        targetDoc: XWPFDocument,
        placeholderParagraph: XWPFParagraph
    ) {
        val runText = run.getText(0) ?: ""
        val parts = runText.split("{%}", limit = 2)

        // Parte antes do placeholder no mesmo run
        if (parts[0].isNotEmpty()) {
            val rBefore = currentP.createRun()
            copyRunStyles(run, rBefore)
            rBefore.setText(parts[0], 0)
        }

        // Interpolar filhos
        node.children.forEach { renderNodeTo(it, targetDoc) }

        // Parte depois do placeholder no mesmo run
        if (parts[1].isNotEmpty()) {
            val rAfterP = targetDoc.createParagraph()
            rAfterP.ctp.pPr = placeholderParagraph.ctp.pPr
            val rAfter = rAfterP.createRun()
            copyRunStyles(run, rAfter)
            rAfter.setText(parts[1], 0)
        }
    }

    private fun appendDocument(target: XWPFDocument, source: XWPFDocument) {
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
        target.setText(source.getText(0))
    }

    private fun copyRunStyles(source: XWPFRun, target: XWPFRun) {
        target.ctr.rPr = source.ctr.rPr
    }
}
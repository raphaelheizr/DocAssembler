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
        
        // Remove o parágrafo vazio inicial que o XWPFDocument cria por padrão
        if (resultDocument.paragraphs.isNotEmpty()) {
            resultDocument.removeBodyElement(0)
        }

        document.nodes.forEach { node ->
            renderNodeTo(node, resultDocument)
        }

        val file = File(path)
        file.parentFile?.mkdirs()
        
        FileOutputStream(file).use { out ->
            resultDocument.write(out)
        }
        resultDocument.close()
    }

    private fun renderNodeTo(node: DocumentNode, targetDoc: XWPFDocument) {
        val templateFile = File(node.templatePath)
        if (!templateFile.exists()) {
            throw IllegalArgumentException("Template não encontrado: ${node.templatePath}")
        }

        FileInputStream(templateFile).use { fis ->
            val templateDoc = XWPFDocument(fis)
            
            if (node.children.isEmpty()) {
                appendDocument(targetDoc, templateDoc)
            } else {
                // Se houver children, precisamos interpolar
                var placeholderFound = false
                
                // Procurar pelo placeholder {%}
                val bodyElements = templateDoc.bodyElements
                for (element in bodyElements) {
                    if (element is XWPFParagraph) {
                        val text = element.text
                        if (text.contains("{%}")) {
                            placeholderFound = true
                            
                            // Copiar parágrafos ANTES do placeholder
                            val elementPos = templateDoc.getPosOfParagraph(element)
                            for (i in 0 until elementPos) {
                                val prevElement = templateDoc.bodyElements[i]
                                if (prevElement is XWPFParagraph) {
                                    copyParagraph(prevElement, targetDoc.createParagraph())
                                }
                            }

                            // Tratar o parágrafo do placeholder
                            val placeholderParagraph = element
                            val pText = placeholderParagraph.text
                            
                            if (pText.trim() == "{%}") {
                                // Se for apenas o placeholder, renderiza os filhos no lugar
                                node.children.forEach { child ->
                                    renderNodeTo(child, targetDoc)
                                }
                            } else {
                                // Se houver texto antes/depois no mesmo parágrafo
                                val newP = targetDoc.createParagraph()
                                newP.ctp.pPr = placeholderParagraph.ctp.pPr
                                
                                // Copiar runs até o {%}
                                var foundInRun = false
                                for (run in placeholderParagraph.runs) {
                                    val runText = run.getText(0) ?: ""
                                    if (!foundInRun && runText.contains("{%}")) {
                                        foundInRun = true
                                        val parts = runText.split("{%}", limit = 2)
                                        
                                        // Parte antes
                                        if (parts[0].isNotEmpty()) {
                                            val rBefore = newP.createRun()
                                            copyRunStyles(run, rBefore)
                                            rBefore.setText(parts[0], 0)
                                        }
                                        
                                        // Interpolar filhos aqui
                                        node.children.forEach { child ->
                                            renderNodeTo(child, targetDoc)
                                        }

                                        // Parte depois
                                        if (parts[1].isNotEmpty()) {
                                            val rAfterP = targetDoc.createParagraph()
                                            rAfterP.ctp.pPr = placeholderParagraph.ctp.pPr
                                            val rAfter = rAfterP.createRun()
                                            copyRunStyles(run, rAfter)
                                            rAfter.setText(parts[1], 0)
                                        }
                                    } else if (foundInRun) {
                                        // Texto após o run que continha o placeholder
                                        val lastP = targetDoc.paragraphs.last()
                                        val rNext = lastP.createRun()
                                        copyRunStyles(run, rNext)
                                        rNext.setText(runText, 0)
                                    } else {
                                        // Texto antes do run que contém o placeholder
                                        val rPrev = newP.createRun()
                                        copyRunStyles(run, rPrev)
                                        rPrev.setText(runText, 0)
                                    }
                                }
                            }

                            // Copiar parágrafos DEPOIS do placeholder
                            for (i in (elementPos + 1) until bodyElements.size) {
                                val nextElement = bodyElements[i]
                                if (nextElement is XWPFParagraph) {
                                    copyParagraph(nextElement, targetDoc.createParagraph())
                                }
                            }
                            
                            break
                        }
                    }
                }

                if (!placeholderFound) {
                    throw IllegalStateException("Não é possível interpolar sem a sequência {%} no template: ${node.templatePath}")
                }
            }
            templateDoc.close()
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
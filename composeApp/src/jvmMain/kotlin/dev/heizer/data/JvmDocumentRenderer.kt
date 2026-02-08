package dev.heizer.data

import dev.heizer.domain.*
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class JvmDocumentRenderer : DocumentRenderer {
    override fun render(document: DocumentNode, outputPath: String) {
        val outDoc = XWPFDocument()
        
        renderNode(document, outDoc)

        FileOutputStream(File(outputPath)).use { out ->
            outDoc.write(out)
        }
        outDoc.close()
    }

    private fun renderNode(node: AstNode, outDoc: XWPFDocument) {
        val docPath = node.meta.docPath
        if (docPath != null) {
            val file = File(docPath)
            if (file.exists()) {
                try {
                    FileInputStream(file).use { fis ->
                        val templateDoc = XWPFDocument(fis)
                        // Para simplificar nesta versão, vamos copiar os parágrafos do template
                        templateDoc.paragraphs.forEach { para ->
                            val newPara = outDoc.createParagraph()
                            // Copiar estilo se necessário (complexo com POI puro)
                            // Por enquanto, apenas o texto
                            val newRun = newPara.createRun()
                            newRun.setText(para.text)
                        }
                        templateDoc.close()
                    }
                } catch (e: Exception) {
                    println("Erro ao ler template $docPath: ${e.message}")
                }
            } else {
                println("Aviso: Template não encontrado em $docPath")
            }
        }

        // Recursão
        when (node) {
            is DocumentNode -> node.blocks.forEach { renderNode(it, outDoc) }
            is SectionNode -> node.blocks.forEach { renderNode(it, outDoc) }
            // ParagraphNode e outros podem ter lógica específica se não forem baseados em template
            else -> {}
        }
    }
}

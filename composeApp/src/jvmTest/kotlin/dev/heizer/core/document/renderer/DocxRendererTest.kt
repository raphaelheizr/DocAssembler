package dev.heizer.core.document.renderer

import dev.heizer.core.document.Document
import dev.heizer.core.document.DocumentNode
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileOutputStream
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class DocxRendererTest {

    @Test
    fun testRenderSimpleDocument() {
        // Criar templates temporários
        val templateDir = File("build/test-templates")
        templateDir.mkdirs()
        
        val titleTemplate = File(templateDir, "title.docx")
        XWPFDocument().use { doc ->
            val p = doc.createParagraph()
            val r = p.createRun()
            r.setText("Título do Documento")
            r.isBold = true
            FileOutputStream(titleTemplate).use { doc.write(it) }
        }

        val contentTemplate = File(templateDir, "content.docx")
        XWPFDocument().use { doc ->
            val p1 = doc.createParagraph()
            p1.createRun().setText("Início do conteúdo")
            
            val pPlaceholder = doc.createParagraph()
            pPlaceholder.createRun().setText("{%}")
            
            val p2 = doc.createParagraph()
            p2.createRun().setText("Fim do conteúdo")
            FileOutputStream(contentTemplate).use { doc.write(it) }
        }

        val itemTemplate = File(templateDir, "item.docx")
        XWPFDocument().use { doc ->
            val p = doc.createParagraph()
            p.createRun().setText("- Item da lista")
            FileOutputStream(itemTemplate).use { doc.write(it) }
        }

        // Montar estrutura do Document
        val itemNode = DocumentNode(3, itemTemplate.path)
        val contentNode = DocumentNode(2, contentTemplate.path, listOf(itemNode))
        val rootNode = DocumentNode(1, titleTemplate.path)
        
        val document = Document(
            Document.Metadata("Teste", "none"),
            listOf(rootNode, contentNode)
        )

        val renderer = DocxRenderer()
        val outputPath = "build/test-output/result.docx"
        File("build/test-output").mkdirs()
        
        renderer.render(document, outputPath)
        
        assertTrue(File(outputPath).exists(), "O arquivo de saída deve ser gerado")
        
        // Verificar conteúdo básico
        XWPFDocument(File(outputPath).inputStream()).use { doc ->
            val texts = doc.paragraphs.map { it.text }
            assertTrue(texts.contains("Título do Documento"), "Deve conter o título")
            assertTrue(texts.contains("Início do conteúdo"), "Deve conter o início do conteúdo")
            assertTrue(texts.contains("- Item da lista"), "Deve conter o item interpolado")
            assertTrue(texts.contains("Fim do conteúdo"), "Deve conter o fim do conteúdo")
            assertTrue(!texts.contains("{%}"), "O placeholder não deve estar presente")
        }
    }

    @Test
    fun testRenderErrorWithoutPlaceholder() {
        val templateDir = File("build/test-templates")
        templateDir.mkdirs()
        
        val noPlaceholderTemplate = File(templateDir, "no_placeholder.docx")
        XWPFDocument().use { doc ->
            doc.createParagraph().createRun().setText("Sem placeholder aqui")
            FileOutputStream(noPlaceholderTemplate).use { doc.write(it) }
        }

        val childNode = DocumentNode(2, "irrelevant")
        val rootNode = DocumentNode(1, noPlaceholderTemplate.path, listOf(childNode))
        
        val document = Document(
            Document.Metadata("Erro", "none"),
            listOf(rootNode)
        )

        val renderer = DocxRenderer()
        assertFailsWith<IllegalStateException> {
            renderer.render(document, "build/test-output/error.docx")
        }
    }

    @Test
    fun testStylesPreservationAndPlaceholderRemoval() {
        val templateDir = File("build/test-templates")
        templateDir.mkdirs()

        // Template pai com placeholder
        val parentTemplate = File(templateDir, "parent_styles.docx")
        XWPFDocument().use { doc ->
            val p = doc.createParagraph()
            p.createRun().setText("Pai: ")
            p.createRun().setText("{%}")
            FileOutputStream(parentTemplate).use { doc.write(it) }
        }

        // Template filho com seu próprio estilo (ex: Negrito)
        val childTemplate = File(templateDir, "child_styles.docx")
        XWPFDocument().use { doc ->
            val p = doc.createParagraph()
            val r = p.createRun()
            r.setText("Filho Negrito")
            r.isBold = true
            FileOutputStream(childTemplate).use { doc.write(it) }
        }

        // Cenário 1: Com filho
        val docWithChild = Document(
            Document.Metadata("Com Filho", "none"),
            listOf(DocumentNode(1, parentTemplate.path, listOf(DocumentNode(2, childTemplate.path))))
        )

        val renderer = DocxRenderer()
        val out1 = "build/test-output/styles_with_child.docx"
        renderer.render(docWithChild, out1)

        XWPFDocument(File(out1).inputStream()).use { doc ->
            val p = doc.paragraphs.find { it.text.contains("Filho Negrito") }
            assertTrue(p != null, "Deve conter o texto do filho")
            assertTrue(p!!.runs.any { it.isBold }, "O estilo negrito do filho deve ser preservado")
            assertTrue(!doc.paragraphs.any { it.text.contains("{%}") }, "O placeholder não deve estar presente")
        }

        // Cenário 2: Sem filho (placeholder deve sumir)
        val docWithoutChild = Document(
            Document.Metadata("Sem Filho", "none"),
            listOf(DocumentNode(1, parentTemplate.path, emptyList()))
        )
        val out2 = "build/test-output/styles_no_child.docx"
        renderer.render(docWithoutChild, out2)

        XWPFDocument(File(out2).inputStream()).use { doc ->
            val texts = doc.paragraphs.map { it.text }
            assertTrue(texts.any { it.contains("Pai: ") }, "Deve conter o texto do pai")
            assertTrue(!texts.any { it.contains("{%}") }, "O placeholder deve ter sido removido")
            // Verifica se não sobrou parágrafo vazio onde estava o {%} se ele era o único no parágrafo
            // No nosso caso o parágrafo tem "Pai: {%}", então deve sobrar "Pai: "
            assertTrue(texts.any { it.trim() == "Pai:" }, "Deve conter 'Pai:' sem o placeholder. Texto atual: ${texts}")
        }
    }
}

package dev.heizer.core.document.renderer

import dev.heizer.core.document.Document
import dev.heizer.core.document.DocumentNode
import dev.heizer.core.document.renderer.docx.DocxRenderer
import org.apache.poi.util.Units
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.Document as POIDocument
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImageDimensionTest {

    @Test
    fun testImageDimensionPreservation() {
        val templateDir = File("build/test-templates")
        templateDir.mkdirs()
        val imageTemplate = File(templateDir, "image_template.docx")

        // 1. Criar um template com uma imagem de tamanho específico
        // Usaremos uma imagem dummy de 1x1 pixel
        val dummyImage = ByteArray(100) // Simplificado, POI aceita quase qualquer coisa se o formato for indicado

        val expectedWidthEMU = Units.toEMU(150.0) // 150 points
        val expectedHeightEMU = Units.toEMU(100.0) // 100 points

        XWPFDocument().use { doc ->
            val p = doc.createParagraph()
            val r = p.createRun()
            // Adicionar imagem com tamanho específico
            r.addPicture(
                ByteArrayInputStream(dummyImage),
                POIDocument.PICTURE_TYPE_PNG,
                "dummy.png",
                expectedWidthEMU.toInt(),
                expectedHeightEMU.toInt()
            )
            FileOutputStream(imageTemplate).use { doc.write(it) }
        }

        // 2. Renderizar o documento usando esse template
        val document = Document(
            Document.Metadata("Teste Imagem", "none"),
            listOf(DocumentNode(imageTemplate.path))
        )

        val renderer = DocxRenderer()
        val outputPath = "build/test-output/image_result"
        File("build/test-output").mkdirs()
        renderer.render(document, outputPath)

        // 3. Verificar se as dimensões no documento final são as mesmas do original
        XWPFDocument(File("$outputPath.docx").inputStream()).use { doc ->
            val paragraphs = doc.paragraphs
            val run = paragraphs[0].runs[0]
            val pictures = run.embeddedPictures
            assertEquals(1, pictures.size, "Deve haver uma imagem")
            
            val pic = pictures[0]
            val actualWidth = pic.ctPicture.spPr.xfrm.ext.cx
            val actualHeight = pic.ctPicture.spPr.xfrm.ext.cy

            println("[DEBUG_LOG] Expected Width: $expectedWidthEMU, Actual Width: $actualWidth")
            println("[DEBUG_LOG] Expected Height: $expectedHeightEMU, Actual Height: $actualHeight")

            assertEquals(expectedWidthEMU.toLong(), actualWidth.toLong(), "A largura da imagem deve ser preservada")
            assertEquals(expectedHeightEMU.toLong(), actualHeight.toLong(), "A altura da imagem deve ser preservada")
        }
    }
}

package dev.heizer.core.document.renderer

import dev.heizer.core.document.Document
import dev.heizer.core.document.DocumentNode
import dev.heizer.core.document.renderer.docx.DocxRenderer
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder
import java.io.File
import java.io.FileOutputStream
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TableBorderReproductionTest {

    @Test
    fun testTableBorderPreservation() {
        val templateDir = File("build/repro-table-border")
        templateDir.mkdirs()
        val tableTemplate = File(templateDir, "table_with_borders.docx")

        XWPFDocument().use { doc ->
            val table = doc.createTable(2, 2)
            
            // Adicionar bordas explicitamente via CTTblPr
            val tblPr = table.ctTbl.tblPr ?: table.ctTbl.addNewTblPr()
            val borders = tblPr.addNewTblBorders()
            
            borders.addNewTop().setVal(STBorder.SINGLE)
            borders.top.sz = java.math.BigInteger.valueOf(4)
            borders.top.space = java.math.BigInteger.ZERO
            borders.top.color = "000000"

            borders.addNewBottom().setVal(STBorder.SINGLE)
            borders.bottom.sz = java.math.BigInteger.valueOf(4)
            borders.bottom.color = "000000"

            borders.addNewLeft().setVal(STBorder.SINGLE)
            borders.left.sz = java.math.BigInteger.valueOf(4)
            borders.left.color = "000000"

            borders.addNewRight().setVal(STBorder.SINGLE)
            borders.right.sz = java.math.BigInteger.valueOf(4)
            borders.right.color = "000000"

            borders.addNewInsideH().setVal(STBorder.SINGLE)
            borders.insideH.sz = java.math.BigInteger.valueOf(4)
            borders.insideH.color = "000000"

            borders.addNewInsideV().setVal(STBorder.SINGLE)
            borders.insideV.sz = java.math.BigInteger.valueOf(4)
            borders.insideV.color = "000000"

            table.getRow(0).getCell(0).setText("Célula 1")
            table.getRow(0).getCell(1).setText("Célula 2")
            
            FileOutputStream(tableTemplate).use { doc.write(it) }
        }

        val document = Document(
            Document.Metadata("Teste Tabela", "none"),
            listOf(DocumentNode(tableTemplate.path))
        )

        val renderer = DocxRenderer()
        val outputPath = "build/test-output/table_border_result"
        File("build/test-output").mkdirs()
        renderer.render(document, outputPath)

        XWPFDocument(File("$outputPath.docx").inputStream()).use { doc ->
            val tables = doc.tables
            assertTrue(tables.isNotEmpty(), "Deve haver uma tabela no documento final")
            val outputTable = tables[0]
            val tblPr = outputTable.ctTbl.tblPr
            assertNotNull(tblPr, "tblPr não deve ser nulo")
            val borders = tblPr.tblBorders
            assertNotNull(borders, "Bordas da tabela não devem ser nulas")
            
            println("[DEBUG_LOG] Top border: ${borders.top?.getVal()}")
            assertTrue(borders.isSetTop, "Borda superior deve estar definida")
            assertTrue(borders.top.getVal() != null, "Valor da borda superior deve estar definido")
        }
    }
}

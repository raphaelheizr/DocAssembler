package dev.heizer

import dev.heizer.core.document.component.repository.ComponentRepository
import dev.heizer.core.document.DocumentNodeDefinition
import dev.heizer.domain.DocumentNode
import dev.heizer.domain.DocumentRenderer
import dev.heizer.ui.EditorViewModel
import java.io.File
import kotlin.test.*

class ViewModelTest {

    @Test
    fun testGenerateDocument() {
        val repo = ComponentRepository("test_gen.json")
        var renderedPath: String? = null
        val mockRenderer = object : DocumentRenderer {
            override fun render(document: DocumentNode, outputPath: String) {
                renderedPath = outputPath
            }
        }
        val vm = EditorViewModel(repo, mockRenderer)
        vm.outputPath = "custom_output.docx"
        vm.generateDocument()
        
        assertEquals("custom_output.docx", renderedPath)
        File("test_gen.json").delete()
    }

    @Test
    fun testAddComponentSafely() {
        val repo = ComponentRepository("test_components.json")
        val vm = EditorViewModel(repo)
        
        val def = DocumentNodeDefinition("test", "Test", "Desc", "path/to.doc")
        
        // Adiciona na raiz
        vm.addComponent(def)
        assertEquals(1, vm.document.blocks.size)
        assertEquals("path/to.doc", vm.document.blocks[0].meta.docPath)
        
        // Seleciona a seção adicionada
        val sectionId = vm.document.blocks[0].id
        vm.selectNode(sectionId)
        
        // Tenta adicionar outro componente dentro da seção
        vm.addComponent(def)
        assertEquals(1, vm.document.blocks.size)
        val section = vm.document.blocks[0] as dev.heizer.domain.SectionNode
        assertEquals(1, section.blocks.size)
        
        File("test_components.json").delete()
    }

    @Test
    fun testRegisterComponent() {
        val repo = ComponentRepository("test_reg.json")
        val vm = EditorViewModel(repo)
        
        vm.registerNewComponent("New", "path.doc", "Paragraph")
        
        val saved = repo.load()
        assertTrue(saved.components.any { it.name == "New" && it.templateDocPath == "path.doc" })
        
        File("test_reg.json").delete()
    }
}

package dev.heizer

import dev.heizer.core.document.DocumentNodeDefinition
import dev.heizer.ui.EditorViewModel
import kotlin.test.*

class EditorViewModelTest {

    @Test
    fun testAddComponentUpdatesDocument() {
        val vm = EditorViewModel()
        val initialDoc = vm.document
        val def = DocumentNodeDefinition("1", "Test", "Desc", "templates/title.docx")
        
        vm.addComponent(def)
        
        assertNotSame(initialDoc, vm.document, "Document should be a new instance after adding a component")
        
        val paths = vm.document.nodes.map { it.templatePath }
        
        assertTrue(paths.contains("templates/title.docx"), "Document should contain the new component")
    }

    @Test
    fun testSelectionIdMatchesNodeValueId() {
        val vm = EditorViewModel()
        val def = DocumentNodeDefinition("1", "Test", "Desc", "templates/title.docx")
        vm.addComponent(def)
        
        val addedNodeId = vm.document.nodes.first().id
        
        vm.selectNode(addedNodeId)
        assertEquals(addedNodeId, vm.selectedNodeId)
    }

    @Test
    fun testAddComponentInsertAsChildOfSelected() {
        val vm = EditorViewModel()
        val def1 = DocumentNodeDefinition("1", "Title", "Desc", "templates/title.docx")
        val def2 = DocumentNodeDefinition("2", "Paragraph", "Desc", "templates/paragraph.docx")

        vm.addComponent(def1)
        val firstNode = vm.document.nodes[0]
        val firstId = firstNode.id
        
        vm.selectNode(firstId)
        vm.addComponent(def2)

        assertEquals(1, vm.document.nodes.size, "Should still have only one root node")
        assertEquals(1, vm.document.nodes[0].children.size, "First node should have one child")
        assertEquals("templates/paragraph.docx", vm.document.nodes[0].children[0].templatePath)
    }
}

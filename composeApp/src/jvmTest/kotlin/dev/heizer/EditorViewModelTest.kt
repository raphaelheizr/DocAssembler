package dev.heizer

import dev.heizer.core.document.DocumentNodeDefinition
import dev.heizer.ui.EditorViewModel
import kotlin.test.*
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
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

    @Test
    fun testRemoveNodeWithoutChildrenExecutesImmediately() {
        val vm = EditorViewModel()
        val def = DocumentNodeDefinition("1", "Test", "Desc", "templates/title.docx")
        vm.addComponent(def)
        val nodeId = vm.document.nodes.first().id
        
        vm.requestDeleteNode(vm.document.nodes.first())
        
        assertTrue(vm.document.nodes.isEmpty(), "Node should be removed immediately since it has no children")
        assertNull(vm.pendingDeleteNode, "Should not have pending delete")
    }

    @Test
    fun testRemoveNodeWithChildrenAsksForConfirmation() {
        val vm = EditorViewModel()
        val def1 = DocumentNodeDefinition("1", "Parent", "Desc", "templates/title.docx")
        val def2 = DocumentNodeDefinition("2", "Child", "Desc", "templates/paragraph.docx")
        
        vm.addComponent(def1)
        vm.selectNode(vm.document.nodes.first().id)
        vm.addComponent(def2)
        
        val parentNode = vm.document.nodes.first()
        vm.requestDeleteNode(parentNode)
        
        assertFalse(vm.document.nodes.isEmpty(), "Node should NOT be removed yet")
        assertNotNull(vm.pendingDeleteNode, "Should have pending delete")
        assertEquals(parentNode.id, vm.pendingDeleteNode?.id)
        
        vm.deleteNode(parentNode.id)
        assertTrue(vm.document.nodes.isEmpty(), "Node and children should be removed after confirmation")
        assertNull(vm.pendingDeleteNode, "Pending delete should be cleared")
    }

    @Test
    fun testSettingsPersistence() {
        val vm = EditorViewModel()
        val testPath = "/tmp/test.docx"
        
        vm.setCustomTemplateEnabled(true)
        vm.pickCustomTemplateFileInternal(testPath)
        
        // Simular recarregamento
        val vm2 = EditorViewModel()
        
        assertTrue(vm2.registry.customBaseTemplateEnabled, "Enabled state should be persisted")
        assertEquals(testPath, vm2.registry.customBaseTemplatePath, "Path should be persisted")
    }

    @Test
    fun testGenerateDocumentValidatesBaseTemplate() {
        val vm = EditorViewModel()
        vm.setCustomTemplateEnabled(true)
        vm.pickCustomTemplateFileInternal("/path/to/non/existent.docx")
        
        vm.generateDocument("out", "test.docx")
        
        assertNotNull(vm.modalErrorMessage)
        assertTrue(vm.modalErrorMessage!!.contains("non/existent.docx"))
    }

    @Test
    fun testGenerateDocumentPersistsOutputPathAndFileName() {
        val vm = EditorViewModel()
        val testOutputPath = "out"
        val testFileName = "meu_contrato.docx"
        
        // Simular a abertura do diálogo e a geração
        vm.openGenerateDialog()
        vm.generateDocument(testOutputPath, testFileName)
        
        // Verificar persistência ao recarregar criando nova instância
        val vm2 = EditorViewModel()
        assertEquals(testOutputPath, vm2.registry.outputPath, "OutputPath should be persisted across instances")
        assertEquals(testFileName, vm2.registry.outputFileName, "OutputFileName should be persisted across instances")
    }

    @Test
    fun testSelectRootNode() {
        val vm = EditorViewModel()
        val def = DocumentNodeDefinition("1", "Test", "Desc", "templates/title.docx")
        vm.addComponent(def)
        val nodeId = vm.document.nodes.first().id
        vm.selectNode(nodeId)
        
        assertEquals(nodeId, vm.selectedNodeId)
        
        vm.selectNode(null)
        assertNull(vm.selectedNodeId, "Selecting null should set selectedNodeId to null (root)")
    }
}

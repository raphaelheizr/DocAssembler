package dev.heizer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.heizer.core.document.component.repository.ComponentRepository
import dev.heizer.data.JvmDocumentRenderer
import dev.heizer.domain.*

@Composable
fun App() {
    val repository = remember { ComponentRepository() }
    val renderer = remember { JvmDocumentRenderer() }
    val viewModel = remember { EditorViewModel(repository, renderer) }
    var showRegisterDialog by remember { mutableStateOf(false) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column {
                // Header
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("DocAssembler")
                            Spacer(modifier = Modifier.width(16.dp))
                            OutlinedTextField(
                                value = viewModel.outputPath,
                                onValueChange = { viewModel.outputPath = it },
                                label = { Text("Caminho de saída") },
                                modifier = Modifier.width(300.dp),
                                singleLine = true
                            )
                        }
                    },
                    actions = {
                        Button(onClick = { viewModel.generateDocument() }) {
                            Text("Gerar Documento")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { showRegisterDialog = true }) {
                            Text("Cadastrar Componente")
                        }
                    }
                )

                Row(modifier = Modifier.fillMaxSize()) {
                    // Esquerda: Tree-view
                    Box(modifier = Modifier.weight(0.4f).fillMaxHeight().background(Color.LightGray.copy(alpha = 0.2f)).padding(8.dp)) {
                        TreeView(viewModel.document, viewModel.selectedNodeId, onSelect = { viewModel.selectNode(it) })
                    }

                    // Direita: Lista de botões
                    Column(modifier = Modifier.weight(0.6f).fillMaxHeight().padding(8.dp)) {
                        Text("Criar Componente", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn {
                            items(viewModel.registry.components) { def ->
                                Button(
                                    onClick = { viewModel.addComponent(def) },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(def.name)
                                        if (def.description.isNotEmpty()) {
                                            Text(def.description, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showRegisterDialog) {
            RegisterComponentDialog(
                onDismiss = { showRegisterDialog = false },
                onConfirm = { name, docPath, type ->
                    viewModel.registerNewComponent(name, docPath, type)
                    showRegisterDialog = false
                }
            )
        }
    }
}

@Composable
fun TreeView(node: AstNode, selectedId: String?, onSelect: (String) -> Unit) {
    Column(modifier = Modifier.padding(start = 8.dp)) {
        val isSelected = node.id == selectedId
        val label = when (node) {
            is DocumentNode -> "📄 Documento"
            is SectionNode -> "📁 Seção (${node.id.take(4)})"
            is ParagraphNode -> "¶ Parágrafo (${node.id.take(4)})"
            is PlaceholderNode -> "{} Placeholder (${node.key})"
            is TextNode -> "abc Texto (${node.text.take(10)})"
            else -> node.id
        }
        val subLabel = node.meta.docPath?.let { " [$it]" } ?: ""

        Text(
            text = label + subLabel,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(node.id) }
                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                .padding(4.dp),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.Unspecified
        )

        when (node) {
            is DocumentNode -> node.blocks.forEach { TreeView(it, selectedId, onSelect) }
            is SectionNode -> node.blocks.forEach { TreeView(it, selectedId, onSelect) }
            is ParagraphNode -> node.inlines.forEach { TreeView(it, selectedId, onSelect) }
            else -> {}
        }
    }
}

@Composable
fun RegisterComponentDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var docPath by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Section") }
    val types = listOf("Section", "Paragraph", "Placeholder", "Text")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cadastrar Novo Componente") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Componente") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = docPath,
                    onValueChange = { docPath = it },
                    label = { Text("Caminho do arquivo .doc") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Tipo de Nó:")
                types.forEach { t ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = type == t, onClick = { type = t })
                        Text(t)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank() && docPath.isNotBlank()) onConfirm(name, docPath, type) }) {
                Text("Cadastrar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(title: @Composable () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    CenterAlignedTopAppBar(title = title, actions = actions)
}
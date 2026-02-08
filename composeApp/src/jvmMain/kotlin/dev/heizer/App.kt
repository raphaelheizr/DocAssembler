package dev.heizer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.heizer.core.document.Document
import dev.heizer.core.document.DocumentNode
import dev.heizer.core.document.DocumentNodeDefinition
import dev.heizer.ui.EditorViewModel
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Composable
fun App() {
    val viewModel = remember { EditorViewModel() }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column {
                // Header
                TopAppBar(
                    title = {
                        Text("DocAssembler")
                    },
                    actions = {
                        IconButton(onClick = { viewModel.isSettingsOpen = true }) {
                            Text("⚙️")
                        }
                        Button(onClick = { viewModel.generateDocument() }) {
                            Text("Gerar Documento")
                        }
                    }
                )

                if (viewModel.isSettingsOpen) {
                    SettingsScreen(viewModel)
                }

                if (viewModel.errorMessage != null) {
                    AlertDialog(
                        onDismissRequest = { viewModel.errorMessage = null },
                        confirmButton = {
                            TextButton(onClick = { viewModel.errorMessage = null }) {
                                Text("OK")
                            }
                        },
                        title = { Text("Erro") },
                        text = { Text(viewModel.errorMessage!!) }
                    )
                }

                if (viewModel.successMessage != null) {
                    AlertDialog(
                        onDismissRequest = { viewModel.successMessage = null },
                        confirmButton = {
                            TextButton(onClick = { viewModel.successMessage = null }) {
                                Text("OK")
                            }
                        },
                        title = { Text("Sucesso") },
                        text = { Text(viewModel.successMessage!!) }
                    )
                }

                if (viewModel.pendingDeleteNode != null) {
                    AlertDialog(
                        onDismissRequest = { viewModel.cancelDelete() },
                        confirmButton = {
                            TextButton(onClick = { viewModel.confirmDeleteNode(viewModel.pendingDeleteNode!!.id) }) {
                                Text("Confirmar")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.cancelDelete() }) {
                                Text("Cancelar")
                            }
                        },
                        title = { Text("Confirmar Exclusão") },
                        text = { Text("Este nodo possui filhos. Excluir este nodo também excluirá todos os seus filhos. Deseja continuar?") }
                    )
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    // Esquerda: Árvore do Documento
                    Box(
                        modifier = Modifier.weight(0.4f).fillMaxHeight().background(Color.LightGray.copy(alpha = 0.2f))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("Estrutura do Documento", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            TreeView(
                                viewModel.document,
                                viewModel.selectedNodeId,
                                onSelect = { viewModel.selectNode(it) },
                                onDelete = { viewModel.requestDeleteNode(it) })
                        }
                    }

                    // Direita: Lista de ComponentDefinition
                    Column(modifier = Modifier.weight(0.6f).fillMaxHeight().padding(8.dp)) {
                        Text("Componentes Disponíveis", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn {
                            items(viewModel.registry.definitions) { def: DocumentNodeDefinition ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        .clickable { viewModel.addComponent(def) }
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(def.name, style = MaterialTheme.typography.titleSmall)
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
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
fun TreeView(document: Document, selectedId: Uuid?, onSelect: (Uuid) -> Unit, onDelete: (DocumentNode) -> Unit) {
    Column {
        Text(
            text = "📄 ${document.metadata.name}",
            modifier = Modifier.padding(vertical = 4.dp)
        )
        document.nodes.forEach { node ->
            NodeView(node, selectedId, onSelect, onDelete)
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
fun NodeView(node: DocumentNode, selectedId: Uuid?, onSelect: (Uuid) -> Unit, onDelete: (DocumentNode) -> Unit) {
    Column(modifier = Modifier.padding(start = 16.dp)) {
        val isSelected = node.id == selectedId

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .clickable { onSelect(node.id) },
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.5f
                )
            ),
            elevation = if (isSelected) CardDefaults.cardElevation(defaultElevation = 4.dp) else CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📄",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "ID: ${node.id} - ${node.templatePath.split("/").last()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onDelete(node) }, modifier = Modifier.size(24.dp)) {
                    Text(
                        text = "X",
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        node.children.forEach { child ->
            NodeView(child, selectedId, onSelect, onDelete)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(title: @Composable () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    CenterAlignedTopAppBar(title = title, actions = actions)
}

@Composable
fun SettingsScreen(viewModel: EditorViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.isSettingsOpen = false },
        confirmButton = {
            TextButton(onClick = { viewModel.isSettingsOpen = false }) {
                Text("Fechar")
            }
        },
        title = { Text("Configurações") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text("Templates", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = viewModel.registry.customBaseTemplateEnabled,
                        onCheckedChange = { viewModel.toggleCustomTemplate(it) }
                    )
                    Text("Usar modelo-base personalizado")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { viewModel.pickCustomTemplateFile() },
                        enabled = viewModel.registry.customBaseTemplateEnabled
                    ) {
                        Text("Selecionar Arquivo .docx")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = viewModel.registry.customBaseTemplatePath.ifBlank { "Nenhum arquivo selecionado" },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    )
}
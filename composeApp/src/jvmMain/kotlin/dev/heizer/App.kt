package dev.heizer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import dev.heizer.core.document.Document
import dev.heizer.core.document.DocumentNode
import dev.heizer.core.document.DocumentNodeDefinition
import dev.heizer.ui.EditorViewModel

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
                        Button(onClick = { viewModel.generateDocument() }) {
                            Text("Gerar Documento")
                        }
                    }
                )

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

                Row(modifier = Modifier.fillMaxSize()) {
                    // Esquerda: Árvore do Documento
                    Box(modifier = Modifier.weight(0.4f).fillMaxHeight().background(Color.LightGray.copy(alpha = 0.2f)).padding(8.dp)) {
                        Column {
                            Text("Estrutura do Documento", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            TreeView(viewModel.document, viewModel.selectedNodeId, onSelect = { viewModel.selectNode(it) })
                        }
                    }

                    // Direita: Lista de ComponentDefinition
                    Column(modifier = Modifier.weight(0.6f).fillMaxHeight().padding(8.dp)) {
                        Text("Componentes Disponíveis", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn {
                            items(viewModel.definitions) { def: DocumentNodeDefinition ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.addComponent(def) }
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

@Composable
fun TreeView(document: Document, selectedId: Long?, onSelect: (Long) -> Unit) {
    Column {
        Text(
            text = "📄 ${document.metadata.name}",
            modifier = Modifier.padding(vertical = 4.dp)
        )
        document.nodes.forEach { node ->
            NodeView(node, selectedId, onSelect)
        }
    }
}

@Composable
fun NodeView(node: DocumentNode, selectedId: Long?, onSelect: (Long) -> Unit) {
    Column(modifier = Modifier.padding(start = 16.dp)) {
        val isSelected = node.id == selectedId
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .clickable { onSelect(node.id) },
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            elevation = if (isSelected) CardDefaults.cardElevation(defaultElevation = 4.dp) else CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    text = "ID: ${node.id % 1000} - ${node.templatePath.split("/").last()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        node.children.forEach { child ->
            NodeView(child, selectedId, onSelect)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(title: @Composable () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    CenterAlignedTopAppBar(title = title, actions = actions)
}
package dev.heizer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.heizer.core.document.Document
import dev.heizer.core.document.DocumentNode
import dev.heizer.core.document.DocumentNodeDefinition
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.heizer.ui.EditorViewModel
import dev.heizer.ui.theme.DocAssemblerTheme
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Composable
fun App() {
    val viewModel = remember { EditorViewModel() }

    DocAssemblerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column {
                // Header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp
                ) {
                    TopAppBar(
                        title = {
                            Text(
                                "DOC ASSEMBLER",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Light,
                                    letterSpacing = 3.sp
                                )
                            )
                        },
                        actions = {
                            IconButton(onClick = { viewModel.isSettingsOpen = true }) {
                                Text("⚙")
                            }
                            Button(
                                onClick = { viewModel.openGenerateDialog() },
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Text("GERAR DOCUMENTO")
                            }
                        }
                    )
                }

                if (viewModel.isSettingsOpen) {
                    SettingsScreen(viewModel)
                }

                if (viewModel.modalErrorMessage != null) {
                    AlertDialog(
                        onDismissRequest = { viewModel.modalErrorMessage = null },
                        confirmButton = {
                            TextButton(onClick = { viewModel.modalErrorMessage = null }) {
                                Text("OK")
                            }
                        },
                        title = { Text("Erro") },
                        text = { Text(viewModel.modalErrorMessage!!) }
                    )
                }

                if (viewModel.modalSuccessMessage != null) {
                    AlertDialog(
                        onDismissRequest = { viewModel.modalSuccessMessage = null },
                        confirmButton = {
                            TextButton(onClick = { viewModel.modalSuccessMessage = null }) {
                                Text("OK")
                            }
                        },
                        title = { Text("Sucesso") },
                        text = { Text(viewModel.modalSuccessMessage!!) }
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

                if (viewModel.isGenerateDialogOpen) {
                    AlertDialog(
                        onDismissRequest = { viewModel.isGenerateDialogOpen = false },
                        title = { Text("Gerar Arquivo") },
                        text = {
                            Column {
                                Text("Defina o nome do arquivo e a pasta de destino:")
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = viewModel.currentOutputFileName,
                                    onValueChange = { viewModel.currentOutputFileName = it },
                                    label = { Text("Nome do Arquivo") },
                                    placeholder = { Text("ex: contrato") },
                                    supportingText = { Text("O arquivo será salvo com a extensão .docx") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Button(
                                        onClick = { viewModel.pickOutputDirectory() },
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text("ESCOLHER PASTA")
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = viewModel.currentOutputPath.ifBlank { "Nenhuma pasta selecionada" },
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { viewModel.generateDocument(viewModel.currentOutputPath, viewModel.currentOutputFileName) },
                                enabled = viewModel.currentOutputPath.isNotBlank() && viewModel.currentOutputFileName.isNotBlank()
                            ) {
                                Text("Gerar")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.isGenerateDialogOpen = false }) {
                                Text("Cancelar")
                            }
                        }
                    )
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    // Esquerda: Árvore do Documento
                    Surface(
                        modifier = Modifier.weight(0.7f).fillMaxHeight(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "ESTRUTURA",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            TreeView(
                                viewModel.document,
                                viewModel.selectedNodeId,
                                onSelect = { viewModel.selectNode(it) },
                                onDelete = { viewModel.requestDeleteNode(it) })
                        }
                    }

                    // Direita: Lista de ComponentDefinition
                    Column(modifier = Modifier.weight(0.3f).fillMaxHeight().padding(16.dp)) {
                        Text(
                            "COMPONENTES",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                        ) {
                            items(viewModel.registry.definitions) { def: DocumentNodeDefinition ->
                                Card(
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable { viewModel.addComponent(def) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    shape = MaterialTheme.shapes.medium,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            def.name.uppercase(),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (def.description.isNotEmpty()) {
                                            Text(
                                                def.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
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
fun TreeView(document: Document, selectedId: Uuid?, onSelect: (Uuid?) -> Unit, onDelete: (DocumentNode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val isRootSelected = selectedId == null
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(null) },
            color = if (isRootSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📁",
                    modifier = Modifier.padding(end = 12.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = document.metadata.name.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isRootSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isRootSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        document.nodes.forEach { node ->
            NodeView(node, selectedId, onSelect, onDelete)
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
fun NodeView(node: DocumentNode, selectedId: Uuid?, onSelect: (Uuid?) -> Unit, onDelete: (DocumentNode) -> Unit) {
    Column(modifier = Modifier.padding(start = 24.dp)) {
        val isSelected = node.id == selectedId

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .clickable { onSelect(node.id) },
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            shape = MaterialTheme.shapes.medium,
            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "—",
                    modifier = Modifier.padding(end = 12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = node.templatePath.split("/").last().uppercase(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = node.id.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = (if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.5f)
                    )
                }
                IconButton(onClick = { onDelete(node) }, modifier = Modifier.size(24.dp)) {
                    Text(
                        text = "✕",
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
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
    TopAppBar(
        title = title,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun SettingsScreen(viewModel: EditorViewModel) {
    var showEditDialog by remember { mutableStateOf<DocumentNodeDefinition?>(null) }

    AlertDialog(
        onDismissRequest = { viewModel.isSettingsOpen = false },
        modifier = Modifier.widthIn(min = 600.dp).heightIn(max = 800.dp),
        confirmButton = {
            TextButton(onClick = { viewModel.isSettingsOpen = false }) {
                Text("FECHAR")
            }
        },
        title = {
            Text(
                "CONFIGURAÇÕES",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Light,
                letterSpacing = 2.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text(
                    "MODELO BASE",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = viewModel.registry.customBaseTemplateEnabled,
                                onCheckedChange = { checked -> viewModel.setCustomTemplateEnabled(checked) }
                            )
                            Text("Usar modelo-base personalizado", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { viewModel.pickCustomTemplateFile() },
                                enabled = viewModel.registry.customBaseTemplateEnabled,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text("SELECIONAR .DOCX")
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = viewModel.registry.customBaseTemplatePath.ifBlank { "Nenhum arquivo" }.split("/").last(),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "TEMPLATES DE COMPONENTES",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = {
                        showEditDialog = DocumentNodeDefinition(
                            id = UUID.randomUUID().toString(),
                            name = "",
                            description = "",
                            templateDocPath = ""
                        )
                    }) {
                        Text("+", style = MaterialTheme.typography.headlineSmall)
                    }
                }

                Text("""Para utilizar interpolação, insira um único {%} no corpo do documento.
                    |
                    |Desta forma, o modelo selecionado será inserido no lugar do bloco.
                """.trimMargin()
                    , style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    userScrollEnabled = true
                ) {
                    items(viewModel.registry.definitions) { definition ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(definition.name.uppercase(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    if (definition.description.isNotEmpty()) {
                                        Text(definition.description, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                IconButton(onClick = { showEditDialog = definition }, modifier = Modifier.size(32.dp)) {
                                    Text("✎")
                                }
                                IconButton(onClick = { viewModel.deleteDefinition(definition.id) }, modifier = Modifier.size(32.dp)) {
                                    Text("🗑", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    )

    showEditDialog?.let { definition ->
        DefinitionEditDialog(
            definition = definition,
            onDismiss = { showEditDialog = null },
            onConfirm = { updated ->
                viewModel.addOrUpdateDefinition(updated)
                showEditDialog = null
            },
            onPickFile = { viewModel.pickDefinitionFile(it) }
        )
    }
}

@Composable
fun DefinitionEditDialog(
    definition: DocumentNodeDefinition,
    onDismiss: () -> Unit,
    onConfirm: (DocumentNodeDefinition) -> Unit,
    onPickFile: ((String) -> Unit) -> Unit
) {
    var name by remember { mutableStateOf(definition.name) }
    var description by remember { mutableStateOf(definition.description) }
    var path by remember { mutableStateOf(definition.templateDocPath) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (definition.name.isEmpty()) "NOVO COMPONENTE" else "EDITAR COMPONENTE",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Light
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("NOME") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFFF2F2F2),
                        focusedContainerColor = Color(0xFFF2F2F2),
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent
                    )
                )
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("DESCRIÇÃO") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFFF2F2F2),
                        focusedContainerColor = Color(0xFFF2F2F2),
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent
                    )
                )
                
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onPickFile { path = it } },
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text("ARQUIVO")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            path.split("/").last().ifBlank { "Nenhum arquivo" },
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        definition.copy(
                            name = name,
                            description = description,
                            templateDocPath = path
                        )
                    )
                },
                enabled = name.isNotBlank() && path.isNotBlank(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("SALVAR")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR")
            }
        }
    )
}
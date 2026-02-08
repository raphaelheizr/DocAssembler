package dev.heizer.core.document.renderer.model

class InterpolationEngine {
    fun interpolate(
        nodes: List<DocNode>,
        childrenMap: Map<PlaceholderNode, List<DocNode>>
    ): List<DocNode> {
        val result = mutableListOf<DocNode>()
        for (node in nodes) {
            when (node) {
                is PlaceholderNode -> {
                    val children = childrenMap[node] ?: emptyList()
                    result.addAll(children)
                }
                is ParagraphNode -> {
                    if (node.runs.any { it.text.contains("{%}") }) {
                        result.addAll(interpolateInline(node, childrenMap))
                    } else {
                        result.add(node)
                    }
                }
                else -> result.add(node)
            }
        }
        return result
    }

    private fun interpolateInline(
        pNode: ParagraphNode,
        childrenMap: Map<PlaceholderNode, List<DocNode>>
    ): List<DocNode> {
        val result = mutableListOf<DocNode>()
        
        // Se o parágrafo contém um placeholder inline, ele pode se transformar em múltiplos parágrafos
        // se os filhos contiverem parágrafos.
        
        val placeholderRun = pNode.runs.find { it.text.contains("{%}") } ?: return listOf(pNode)
        
        // Criamos um PlaceholderNode "virtual" para achar os filhos (ou passamos os filhos de outra forma)
        // No DocxRenderer atual, node.children são os filhos do DocumentNode.
        // Vamos assumir que childrenMap tem a entrada correta. 
        // Mas como identificar QUAL PlaceholderNode? 
        // Para simplificar, vamos assumir que há apenas um tipo de placeholder por parágrafo/nível.
        
        val placeholderKey = childrenMap.keys.find { it.originalParagraphPPr == pNode.pPr } 
            ?: childrenMap.keys.firstOrNull() // Fallback temporário
        
        val children = if (placeholderKey != null) childrenMap[placeholderKey] ?: emptyList() else emptyList()

        if (children.isEmpty()) {
            // Remove o placeholder e mantém o parágrafo
            val newRuns = pNode.runs.map { r ->
                if (r == placeholderRun) r.copy(text = r.text.replace("{%}", "")) else r
            }.filter { it.text.isNotEmpty() }
            
            if (newRuns.isEmpty()) return emptyList()
            return listOf(pNode.copy(runs = newRuns))
        }

        // Lógica complexa: prefixo + filhos + sufixo
        val parts = placeholderRun.text.split("{%}", limit = 2)
        val prefix = parts[0]
        val suffix = if (parts.size > 1) parts[1] else ""

        // 1. Parágrafo inicial (com prefixo)
        val initialRuns = mutableListOf<RunNode>()
        for (run in pNode.runs) {
            if (run == placeholderRun) {
                if (prefix.isNotEmpty()) {
                    initialRuns.add(run.copy(text = prefix))
                }
                break
            }
            initialRuns.add(run)
        }
        
        // Se houver filhos, o primeiro filho pode ser "mesclado" no primeiro parágrafo se for Texto?
        // Mas parágrafos são atômicos no OOXML. Vamos apenas emitir sequencialmente.
        
        if (initialRuns.isNotEmpty()) {
            result.add(pNode.copy(runs = initialRuns))
        }

        // 2. Filhos
        result.addAll(children)

        // 3. Parágrafo final (com sufixo)
        val finalRuns = mutableListOf<RunNode>()
        var foundPlaceholder = false
        for (run in pNode.runs) {
            if (run == placeholderRun) {
                foundPlaceholder = true
                if (suffix.isNotEmpty()) {
                    finalRuns.add(run.copy(text = suffix))
                }
                continue
            }
            if (foundPlaceholder) {
                finalRuns.add(run)
            }
        }

        if (finalRuns.isNotEmpty()) {
            result.add(pNode.copy(runs = finalRuns))
        }

        return result
    }
}

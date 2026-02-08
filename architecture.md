## 1. Contrato da AST (nível de domínio)

### Princípios

* **Imutável** (facilita validação, cache e rollback)
* **Estrutural**, não textual
* **Formato-agnóstica** (.doc / .odt / futuro)
* **Placeholder é nó de primeira classe**
* **Estilo separado de conteúdo**

---

### Interfaces-base

```kotlin
interface AstNode {
    val id: String
    val meta: Meta
}
```

```kotlin
data class Meta(
    val originTemplate: String? = null,
    val layer: Int? = null
)
```

---

### Documento raiz

```kotlin
data class DocumentNode(
    override val id: String,
    val blocks: List<BlockNode>,
    override val meta: Meta = Meta()
) : AstNode
```

---

### Blocos estruturais

```kotlin
sealed interface BlockNode : AstNode
```

#### Section

```kotlin
data class SectionNode(
    override val id: String,
    val blocks: List<BlockNode>,
    override val meta: Meta = Meta()
) : BlockNode
```

#### Paragraph

```kotlin
data class ParagraphNode(
    override val id: String,
    val inlines: List<InlineNode>,
    val style: ParagraphStyle,
    override val meta: Meta = Meta()
) : BlockNode
```

#### Table

```kotlin
data class TableNode(
    override val id: String,
    val rows: List<TableRowNode>,
    val style: TableStyle,
    override val meta: Meta = Meta()
) : BlockNode
```

---

### Inline content

```kotlin
sealed interface InlineNode : AstNode
```

#### Text

```kotlin
data class TextNode(
    override val id: String,
    val text: String,
    val style: TextStyle,
    override val meta: Meta = Meta()
) : InlineNode
```

#### Placeholder

```kotlin
data class PlaceholderNode(
    override val id: String,
    val key: String,               // ex: HEADER, BODY
    val required: Boolean = true,
    override val meta: Meta = Meta()
) : InlineNode
```

---

### Tabela

```kotlin
data class TableRowNode(
    override val id: String,
    val cells: List<TableCellNode>,
    override val meta: Meta = Meta()
) : AstNode
```

```kotlin
data class TableCellNode(
    override val id: String,
    val blocks: List<BlockNode>,
    override val meta: Meta = Meta()
) : AstNode
```

---

### Estilos (Value Objects)

```kotlin
data class TextStyle(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val fontSize: Int? = null
)
```

```kotlin
data class ParagraphStyle(
    val alignment: Alignment,
    val spacing: Int? = null
)
```

```kotlin
data class TableStyle(
    val bordered: Boolean = true
)
```

---

## 2. Contratos de Transformação (AST → AST)

### Parser (infra → domínio)

```kotlin
interface TemplateParser {
    fun parse(bytes: ByteArray): DocumentNode
}
```

---

### Interpolação

```kotlin
interface AstInterpolator {
    fun interpolate(
        document: DocumentNode,
        fragments: Map<String, DocumentNode>,
        policy: InterpolationPolicy
    ): DocumentNode
}
```

```kotlin
data class InterpolationPolicy(
    val maxDepth: Int,
    val layerOrder: List<Int>,
    val allowOverride: Boolean
)
```

---

### Exportação

```kotlin
interface DocumentRenderer {
    fun render(document: DocumentNode): ByteArray
}
```

---

## 3. Implementação Kotlin Multiplatform (Desktop)

### Módulos KMP

```
:core-domain        (commonMain)
:core-application   (commonMain)
:parser-doc         (jvmMain)
:parser-odt         (jvmMain)
:renderer-doc       (jvmMain)
:desktop-ui         (jvmMain)
```

---

## 4. Implementação do Interpolador (core-domain)

```kotlin
class DefaultAstInterpolator : AstInterpolator {

    override fun interpolate(
        document: DocumentNode,
        fragments: Map<String, DocumentNode>,
        policy: InterpolationPolicy
    ): DocumentNode {
        return resolve(document, fragments, policy, depth = 0)
    }

    private fun resolve(
        doc: DocumentNode,
        fragments: Map<String, DocumentNode>,
        policy: InterpolationPolicy,
        depth: Int
    ): DocumentNode {
        require(depth <= policy.maxDepth) { "Interpolation depth exceeded" }

        val newBlocks = doc.blocks.flatMap { block ->
            when (block) {
                is ParagraphNode -> resolveParagraph(block, fragments, policy, depth)
                else -> listOf(block)
            }
        }

        return doc.copy(blocks = newBlocks)
    }

    private fun resolveParagraph(
        p: ParagraphNode,
        fragments: Map<String, DocumentNode>,
        policy: InterpolationPolicy,
        depth: Int
    ): List<BlockNode> {

        val result = mutableListOf<BlockNode>()
        val buffer = mutableListOf<InlineNode>()

        for (inline in p.inlines) {
            when (inline) {
                is PlaceholderNode -> {
                    val fragment = fragments[inline.key]
                        ?: error("Missing fragment ${inline.key}")

                    result += ParagraphNode(
                        id = "p-${inline.key}",
                        inlines = emptyList(),
                        style = p.style
                    )

                    result += resolve(fragment, fragments, policy, depth + 1).blocks
                }
                else -> buffer += inline
            }
        }

        if (buffer.isNotEmpty()) {
            result += p.copy(inlines = buffer)
        }

        return result
    }
}
```

---

## 5. Parser `.doc` (jvmMain)

* Apache POI
* Conversão direta para AST
* Cada parágrafo → `ParagraphNode`
* Runs → `TextNode`
* `{%X%}` → `PlaceholderNode`

```kotlin
class DocxTemplateParser : TemplateParser {
    override fun parse(bytes: ByteArray): DocumentNode {
        // Apache POI → AST
        TODO()
    }
}
```

---

## 6. UI Desktop (Compose Desktop)

UI só trabalha com:

* `DocumentNode`
* `AssemblyPlan`
* `InterpolationPolicy`

Sem dependência de `.doc`, `.odt` ou parser.

---

## 7. Garantias dessa AST

* Interpolação **determinística**
* Precedência controlável
* Layout preservado
* Fácil serialização (debug, cache)
* Testável sem filesystem
* Compatível com WASM / backend futuro

---

## Próximo passo lógico

* Formalizar **AssemblyPlan DSL**
* Definir **estratégia de merge de estilos**
* Especificar **placeholder block-level vs inline-level**

Se quiser, sigo exatamente por qualquer um desses.

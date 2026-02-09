# DocAssembler

O **DocAssembler** é uma ferramenta desktop desenvolvida em Kotlin para montagem automatizada de documentos `.docx`. Ele permite criar documentos complexos a partir de fragmentos e definições pré-configuradas, facilitando a padronização e a agilidade na criação de arquivos de texto estruturados.

## Visão Geral

O projeto utiliza **Compose Multiplatform** para a interface gráfica e **Apache POI** para a manipulação de documentos Word. A arquitetura é baseada em uma Árvore de Sintaxe Abstrata (AST), o que torna o sistema flexível para futuras expansões e diferentes formatos de saída.

Principais funcionalidades:
- Gerenciamento de definições de componentes de documentos.
- Interface visual para estruturação do documento.
- Renderização final para o formato `.docx`.
- Suporte a placeholders e interpolação de dados.

## Composição e Interpolação

A composição de documentos no DocAssembler funciona através da união de fragmentos definidos em uma Árvore de Sintaxe Abstrata (AST). Existem duas formas principais de organizar o conteúdo:

1.  **Blocos Estruturais**: Fragmentos que representam seções inteiras, parágrafos ou tabelas.
2.  **Interpolação Inline**: Permite inserir conteúdo dinâmico dentro de um parágrafo existente.

### Uso do Placeholder `{%}`

Para realizar a interpolação inline, utiliza-se a sequência de caracteres `{%}` dentro de um parágrafo em um template `.docx`. 

- **Como funciona**: O motor de renderização identifica a sequência `{%}`, divide o parágrafo original em "antes" e "depois" do marcador, e insere os fragmentos filhos exatamente nessa posição.
- **Regra Importante**: **Só pode haver uma sequência `{%}` por parágrafo.** O sistema está projetado para processar apenas a primeira ocorrência encontrada; sequências adicionais no mesmo parágrafo serão tratadas como texto comum ou ignoradas na lógica de substituição.

Exemplo de uso em um template:
> "Este documento foi gerado por {%} em conformidade com as normas."

Se o fragmento inserido for "DocAssembler", o resultado final será:
> "Este documento foi gerado por DocAssembler em conformidade com as normas."

## Como Executar

Para rodar o programa em ambiente de desenvolvimento, utilize o Gradle:

### macOS / Linux
```shell
./gradlew :composeApp:run
```

### Windows
```shell
.\gradlew.bat :composeApp:run
```

## Distribuição

O projeto utiliza o plugin do Compose Desktop para gerar executáveis nativos e instaladores.

### Criar um executável standalone (Distribuível)
Este comando cria uma pasta com o executável e todas as dependências necessárias (JRE inclusa), permitindo rodar o app sem que o usuário precise ter o Java instalado.

```shell
./gradlew :composeApp:createDistributable
```
O resultado estará em `composeApp/build/compose/binaries/main/app`.

### Criar um instalador para o OS atual
Para gerar um instalador nativo (`.deb` no Linux, `.msi` no Windows, `.dmg` no macOS), execute:

```shell
./gradlew :composeApp:packageDistributionForCurrentOS
```
Os instaladores gerados estarão disponíveis em `composeApp/build/compose/binaries/main/`.

---
Desenvolvido com [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html).
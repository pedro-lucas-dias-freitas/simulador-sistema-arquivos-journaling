# Simulador de Sistema de Arquivos com Journaling

**Autor:** Pedro Lucas Dias Freitas
**Instituição:** Universidade de Fortaleza(UNIFOR)  
**Disciplina:** Projeto de sistemas operacionais  
**Link do Repositório:** [https://github.com/pedro-lucas-dias-freitas/simulador-sistema-arquivos-journaling.git](https://github.com/pedro-lucas-dias-freitas/simulador-sistema-arquivos-journaling.git)

---

## Metodologia
O simulador foi desenvolvido na linguagem de programação Java, utilizando o paradigma de Orientação a Objetos para abstrair os componentes de um sistema de armazenamento. O programa opera no formato de Shell interativo, recebendo chamadas de métodos com seus devidos parâmetros via linha de comando em tempo de execução. 

A arquitetura do software é dividida em camadas: a captura da entrada do usuário, o roteamento lógico e a execução pontual da tarefa correspondente a um comando de um Sistema Operacional (SO). O simulador executa cada funcionalidade em memória de forma isolada e exibe o resultado ou mensagens de erro diretamente na tela.

---

## Parte 1: Introdução ao Sistema de Arquivos com Journaling

### Descrição do Sistema de Arquivos
Um sistema de arquivos é a estrutura lógica e o conjunto de algoritmos que um Sistema Operacional utiliza para organizar, armazenar, nomear e recuperar dados em dispositivos de armazenamento. Sem ele, os dados seriam uma sequência contínua de bits ilegível. Sua importância reside em prover uma abstração simples, em arquivos e pastas, para o usuário, garantindo o controle de acesso, eficiência de leitura/escrita e a persistência dos dados.

### Journaling
O *Journaling* é um mecanismo projetado para garantir a consistência e integridade dos dados contra falhas catastróficas, como quedas abruptas de energia ou travamentos do sistema. Antes que as alterações estruturais sejam consolidadas nos blocos de dados principais, a intenção da operação é registrada em uma área reservada: o log ou *journal*. Se o sistema falhar no meio de uma escrita, o SO consulta o log durante a inicialização para refazer (*redo*) ou desfazer (*undo*) a operação pendente.

Os principais tipos de journaling incluem:
* **Write-Ahead Logging(WAL):** técnica em que as modificações são escritas de forma sequencial em um log persistente antes de serem aplicadas ao estado real do sistema em memória ou disco. É a abordagem técnica simulada neste projeto.
* **Log-Structured File Systems:** sistemas onde todo o próprio sistema de arquivos é estruturado como um log contínuo. As novas escritas e metadados são sempre anexados ao final do log, otimizando escritas sequenciais.
* **Metadata Journaling:** registra apenas as alterações nos metadados, por exemplo estruturas de diretórios e permissões, oferecendo um equilíbrio entre desempenho e segurança.
* **Full Journaling(Data Journaling):** registra tanto os metadados quanto os dados reais dos arquivos no log antes da escrita definitiva. Garante máxima integridade para o conteúdo dos arquivos, porém duplica o volume de I/O em disco.
* **Ordered Journaling:** garante que os dados do arquivo sejam descarregados no disco rígido antes que os metadados associados sejam gravados no journal. Previne que arquivos apontem para dados corrompidos ou antigos após uma falha.
* **Asynchronous Journaling:** as transações são enviadas ao log de forma assíncrona em blocos periódicos. Otimiza drasticamente o desempenho de escrita, assumindo o risco de perda das transações mais recentes em caso de crash imediato.

---

## Parte 2: Arquitetura do Simulador

### Estrutura de Dados
O simulador organiza o sistema de arquivos lógico como uma árvore hierárquica mantida inteiramente na memória RAM através da Java Virtual Machine(JVM). As estruturas fundamentais mapeadas são:
* **FSNode (Classe Abstrata):** atua como o nó base da árvore, contendo os atributos comuns `name` (nome do item) e `parent` (ponteiro para o diretório pai). Define o método abstrato `cloneNode()` para viabilizar operações de cópia via polimorfismo.
* **FSDirectory (Classe Concreta):** herda de `FSNode`. Representa os diretórios e utiliza a estrutura de dados `HashMap<String, FSNode>` para gerenciar seus nós filhos. Isso permite buscar, inserir e remover arquivos ou subdiretórios com complexidade de tempo constante $O(1)$.
* **FSFile (Classe Concreta):** herda de `FSNode`. Representa as folhas da árvore, ou seja, os arquivos simulados.

### Journaling
O mecanismo de journaling foi arquitetado seguindo o padrão *Write-Ahead Logging*(WAL). Ele intercepta a entrada bruta do terminal imediatamente após o envio do usuário. Antes de o simulador validar os parâmetros ou modificar a árvore de nós na memória, a string de comando acompanhada de um carimbo de data/hora (*timestamp*) é gravada fisicamente no disco rígido do sistema hospedeiro em um arquivo chamado `journal.log`. Isso simula com precisão a barreira de persistência que protege sistemas operacionais reais contra corrupção.

---

## Parte 3: Implementação em Java

A implementação do simulador está contida em um arquivo unificado estruturado nas seguintes classes essenciais:

* **Classe `FileSystemSimulator`:** contém o método `main`, que gerencia o loop de execução do terminal simulado. Implementa o método `executeCommand(String[] tokens)`, agindo como o interpretador do Shell que valida a quantidade de argumentos e delega a responsabilidade para as funções internas através de um bloco de controle `switch-case`.
* **Classes `FSFile` e `FSDirectory`:** classes que estendem `FSNode`. A classe `FSDirectory` implementa os métodos principais de manipulação lógica como `criarDiretorio()`, `apagarDiretorio()`, `criarArquivo()`, `apagarArquivo()`, `renomear()` e `copiar()`, que alteram dinamicamente as tabelas de espalhamento (`HashMap`) do diretório atual (`currentDir`).
* **Classe `Journal`:** classe utilitária que expõe o método `logOperation(String operation)`. Utiliza os recursos de I/O físico do Java (`FileWriter` e `BufferedWriter`) configurados no modo de anexação (*append*), escrevendo no arquivo `journal.log` de forma sequencial.

---

## Parte 4: Instalação e Funcionamento

### Recursos Usados na Implementação
* **Linguagem:** Java (JDK 8 ou superior).
* **Bibliotecas Padrão:** `java.io` (gerenciamento do log físico), `java.util` (coleções `HashMap`, `Scanner` e `Map`) e `java.text.SimpleDateFormat` (formatação do tempo).
* **Ambiente:** independente de sistema operacional. Pode ser executado em ambientes Linux, Windows ou macOS através do terminal.

### Orientações sobre a Execução do Simulador

1. **Clonagem do Repositório:**
   Abra o seu terminal e clone o projeto com o comando:
   ```bash
   git clone https://github.com/pedro-lucas-dias-freitas/simulador-sistema-arquivos-journaling.git
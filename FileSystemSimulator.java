import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class FileSystemSimulator {

    //Estruturas de dados do sistema de arquivos
    abstract static class FSNode {
        String name;
        FSDirectory parent;

        public FSNode(String name, FSDirectory parent) {
            this.name = name;
            this.parent = parent;
        }
        public abstract FSNode cloneNode(String newName);
    }

    static class FSFile extends FSNode {
        public FSFile(String name, FSDirectory parent) { super(name, parent); }
        @Override
        public FSNode cloneNode(String newName) { return new FSFile(newName, this.parent); }
    }

    static class FSDirectory extends FSNode {
        Map<String, FSNode> children;
        
        public FSDirectory(String name, FSDirectory parent) {
            super(name, parent);
            this.children = new HashMap<>();
        }
        @Override
        public FSNode cloneNode(String newName) {
            //Cópia simples de diretório
            return new FSDirectory(newName, this.parent);
        }
    }

    //Gerenciador de Journaling Write-Ahead Logging
    static class Journal {
        private final String logFile = "journal.log";

        public void logOperation(String operation) {
            try (FileWriter fw = new FileWriter(logFile, true);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter out = new PrintWriter(bw)) {
                String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                out.println("[" + timeStamp + "] WAL: " + operation);
            } catch (IOException e) {
                System.out.println("Erro ao escrever no journal: " + e.getMessage());
            }
        }
    }

    //Variáveis Globais do simulador
    private static FSDirectory root = new FSDirectory("root", null);
    private static FSDirectory currentDir = root;
    private static Journal journal = new Journal();

    //Motor principal(Shell)
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("------- Simulador de Sistema de arquivos com Journaling -------");
        System.out.println("Digite 'help' para comandos ou 'exit' para sair.\n");

        while (true) {
            System.out.print(getPath(currentDir) + "$ ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            String[] tokens = input.split("\\s+");
            String cmd = tokens[0].toLowerCase();

            if (cmd.equals("exit")) {
                System.out.println("Encerrando simulador...");
                break;
            }

            //Write-Ahead Logging: registra a intenção antes de executar na memória
            journal.logOperation(input);
            executeCommand(tokens);
        }
        scanner.close();
    }

    private static void executeCommand(String[] tokens) {
        String cmd = tokens[0].toLowerCase();
        try {
            switch (cmd) {
                case "help":
                    printHelp();
                    break;

                case "ls":
                    listar(tokens);
                    break;

                case "cd":
                    if (tokens.length < 2) throw new IllegalArgumentException("Uso: cd <nome_diretorio> ou cd ..");
                    mudarDiretorio(tokens[1]);
                    break;

                case "mkdir":
                    if (tokens.length < 2) throw new IllegalArgumentException("Uso: mkdir <nome_diretorio>");
                    criarDiretorio(tokens[1]);
                    break;

                case "rmdir":
                    if (tokens.length < 2) throw new IllegalArgumentException("Uso: rmdir <nome_diretorio>");
                    apagarDiretorio(tokens[1]);
                    break;

                case "touch":
                    if (tokens.length < 2) throw new IllegalArgumentException("Uso: touch <nome_arquivo>");
                    criarArquivo(tokens[1]);
                    break;

                case "rm":
                    if (tokens.length < 2) throw new IllegalArgumentException("Uso: rm <nome_arquivo>");
                    apagarArquivo(tokens[1]);
                    break;

                case "rename":
                    if (tokens.length < 3) throw new IllegalArgumentException("Uso: rename <antigo> <novo>");
                    renomear(tokens[1], tokens[2]);
                    break;

                case "cp":
                    if (tokens.length < 3) throw new IllegalArgumentException("Uso: cp <origem> <destino>");
                    copiar(tokens[1], tokens[2]);
                    break;

                default:
                    System.out.println("Comando não reconhecido. Digite 'help'.");
            }
        } catch (Exception e) {
            System.out.println("Erro: " + e.getMessage());
        }
    }

    //Implementação das operações
    private static void listar(String[] tokens) {
        FSDirectory targetDir = currentDir;
        if (tokens.length > 1) {
            String targetName = tokens[1];
            FSNode node = currentDir.children.get(targetName);
            if (node == null) {
                System.out.println("Erro: Diretório '" + targetName + "' não encontrado.");
                return;
            }
            if (!(node instanceof FSDirectory)) {
                System.out.println("Erro: '" + targetName + "' não é um diretório.");
                return;
            }
            targetDir = (FSDirectory) node;
        }

        if (targetDir.children.isEmpty()) {
            System.out.println("(vazio)");
            return;
        }
        for (FSNode node : targetDir.children.values()) {
            if (node instanceof FSDirectory) System.out.println("[DIR]  " + node.name);
            else System.out.println("[FILE] " + node.name);
        }
    }

    private static void mudarDiretorio(String name) {
        if (name.equals("..")) {
            if (currentDir.parent != null) {
                currentDir = currentDir.parent;
            } else {
                System.out.println("Você já está no diretório raiz(root).");
            }
            return;
        }

        FSNode node = currentDir.children.get(name);
        if (node == null) {
            System.out.println("Erro: Diretório '" + name + "' não encontrado.");
        } else if (!(node instanceof FSDirectory)) {
            System.out.println("Erro: '" + name + "' não é um diretório, é um arquivo.");
        } else {
            currentDir = (FSDirectory) node;
        }
    }

    private static void criarDiretorio(String name) {
        if (currentDir.children.containsKey(name)) {
            System.out.println("Erro: Já existe um item com o nome '" + name + "'.");
            return;
        }
        currentDir.children.put(name, new FSDirectory(name, currentDir));
        System.out.println("Diretório criado com sucesso.");
    }

    private static void apagarDiretorio(String name) {
        FSNode node = currentDir.children.get(name);
        if (node instanceof FSDirectory) {
            currentDir.children.remove(name);
            System.out.println("Diretório apagado.");
        } else {
            System.out.println("Erro: Diretório não encontrado ou é um arquivo.");
        }
    }

    private static void criarArquivo(String name) {
        if (currentDir.children.containsKey(name)) {
            System.out.println("Erro: Já existe um item com o nome '" + name + "'.");
            return;
        }
        currentDir.children.put(name, new FSFile(name, currentDir));
        System.out.println("Arquivo criado.");
    }

    private static void apagarArquivo(String name) {
        FSNode node = currentDir.children.get(name);
        if (node instanceof FSFile) {
            currentDir.children.remove(name);
            System.out.println("Arquivo apagado.");
        } else {
            System.out.println("Erro: Arquivo não encontrado ou é um diretório.");
        }
    }

    private static void renomear(String oldName, String newName) {
        FSNode node = currentDir.children.get(oldName);
        if (node == null) {
            System.out.println("Erro: Item '" + oldName + "' não encontrado.");
            return;
        }
        if (currentDir.children.containsKey(newName)) {
            System.out.println("Erro: Já existe um item com o nome '" + newName + "'.");
            return;
        }
        currentDir.children.remove(oldName);
        node.name = newName;
        currentDir.children.put(newName, node);
        System.out.println("Renomeado com sucesso.");
    }

    private static void copiar(String srcName, String destName) {
        FSNode node = currentDir.children.get(srcName);
        if (node == null) {
            System.out.println("Erro: Origem '" + srcName + "' não encontrada.");
            return;
        }
        if (currentDir.children.containsKey(destName)) {
            System.out.println("Erro: Já existe um item com o nome '" + destName + "'.");
            return;
        }
        FSNode copy = node.cloneNode(destName);
        currentDir.children.put(destName, copy);
        System.out.println("Cópia realizada com sucesso.");
    }

    //Utilitários
    private static String getPath(FSDirectory dir) {
        if (dir.parent == null) return "/" + dir.name;
        return getPath(dir.parent) + "/" + dir.name;
    }

    private static void printHelp() {
        System.out.println("Comandos disponíveis:");
        System.out.println("  ls                     - Lista o diretório atual");
        System.out.println("  ls <diretorio>         - Lista o conteúdo de um diretório específico");
        System.out.println("  cd <diretorio>         - Entra em um diretório");
        System.out.println("  cd ..                  - Volta para o diretório pai");
        System.out.println("  mkdir <dir>            - Cria um diretório");
        System.out.println("  rmdir <dir>            - Apaga um diretório");
        System.out.println("  touch <arq>            - Cria um arquivo em branco");
        System.out.println("  rm <arq>               - Apaga um arquivo");
        System.out.println("  rename <antigo> <novo> - Renomeia arquivo ou diretório");
        System.out.println("  cp <origem> <dest>     - Copia um arquivo ou diretório");
        System.out.println("  exit                   - Sai do simulador");
    }
}
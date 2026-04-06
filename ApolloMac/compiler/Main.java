import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws Exception {
        Path sourcePath = Paths.get(args.length > 0 ? args[0] : "main.apollo").toAbsolutePath().normalize();
        compileApollo(sourcePath.toString(), "output.cpp", new LinkedHashSet<>());
    }

    static void compileApollo(String inputPath, String out, Set<String> visited) throws Exception {
        Path sourcePath = Paths.get(inputPath).toAbsolutePath().normalize();
        Path importRoot = determineImportRoot(sourcePath);
        Set<String> generatedFiles = new LinkedHashSet<>();
        compileApollo(sourcePath, Paths.get(out), visited, importRoot, generatedFiles);
        writeCleanupManifest(generatedFiles);
    }

    static void compileApollo(Path sourcePath, Path out, Set<String> visited, Path importRoot, Set<String> generatedFiles) throws Exception {
        String visitedKey = sourcePath.toAbsolutePath().toString();
        if (!visited.add(visitedKey)) {
            return;
        }

        System.out.println("INPUT PATH: " + sourcePath);
        System.out.println("INPUT PATH (abs): " + sourcePath.toAbsolutePath());
        System.out.println("EXISTS: " + Files.exists(sourcePath));
        if (Files.exists(sourcePath)) {
            System.out.println("FILE SIZE: " + Files.size(sourcePath));
        }

        String program = new String(Files.readAllBytes(sourcePath), StandardCharsets.UTF_8);

        System.out.println("PROGRAM LENGTH: " + program.length());
        CharStream input = CharStreams.fromString(program);
        compilerv1Lexer lexer = new compilerv1Lexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();
        compilerv1Parser parser = new compilerv1Parser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new DiagnosticErrorListener());
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
                                    String msg, RecognitionException e) {
                System.out.println("SYNTAX ERROR at line " + line + ":" + charPositionInLine + " - " + msg);
            }
        });

        compilerv1Parser.ProgramContext tree = parser.program();

        int syntaxErrors = parser.getNumberOfSyntaxErrors();
        System.out.println("Syntax errors: " + syntaxErrors);
        if (syntaxErrors > 0) {
            throw new IllegalStateException("Compilation aborted due to syntax errors in " + sourcePath);
        }

        CppCodeGenVisitor gen = new CppCodeGenVisitor(out.toString(), buildModuleKey(importRoot, sourcePath));
        gen.visit(tree);
        gen.close();
        generatedFiles.add(out.toAbsolutePath().normalize().toString());

        System.out.println("Wrote " + out);

        for (String dependency : gen.getDependencies()) {
            Path dependencySourcePath = resolveImportPath(importRoot, dependency);
            Path dependencyHeaderPath = resolveImportPath(importRoot, toHeaderOutputPath(dependency));
            compileApollo(dependencySourcePath, dependencyHeaderPath, visited, importRoot, generatedFiles);
        }
    }

    static Path determineImportRoot(Path sourcePath) {
        Path parent = sourcePath.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            return parent;
        }
        return Paths.get(".").toAbsolutePath().normalize();
    }

    static Path resolveImportPath(Path importRoot, String importPath) {
        Path path = Paths.get(importPath);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return importRoot.resolve(path).normalize();
    }

    static String toHeaderOutputPath(String sourcePath) {
        Path path = Paths.get(sourcePath).normalize();
        String fileName = path.getFileName().toString();
        String headerName;
        if (fileName.endsWith(".apollo")) {
            headerName = fileName.substring(0, fileName.length() - ".apollo".length()) + ".hpp";
        } else if (fileName.endsWith(".aph")) {
            headerName = fileName.substring(0, fileName.length() - ".aph".length()) + ".hpp";
        } else {
            headerName = fileName + ".hpp";
        }
        Path parent = path.getParent();
        return parent == null ? headerName : parent.resolve(headerName).toString();
    }

    static String buildModuleKey(Path importRoot, Path sourcePath) {
        Path normalizedImportRoot = importRoot.toAbsolutePath().normalize();
        Path normalizedSourcePath = sourcePath.toAbsolutePath().normalize();
        Path modulePath;
        if (normalizedSourcePath.startsWith(normalizedImportRoot)) {
            modulePath = normalizedImportRoot.relativize(normalizedSourcePath);
        } else if (normalizedSourcePath.getFileName() != null) {
            modulePath = normalizedSourcePath.getFileName();
        } else {
            modulePath = normalizedSourcePath;
        }

        String moduleKey = modulePath.toString().replace('\\', '/');
        if (moduleKey.endsWith(".apollo")) {
            moduleKey = moduleKey.substring(0, moduleKey.length() - ".apollo".length());
        } else if (moduleKey.endsWith(".aph")) {
            moduleKey = moduleKey.substring(0, moduleKey.length() - ".aph".length());
        }
        return moduleKey;
    }

    static void writeCleanupManifest(Set<String> generatedFiles) throws Exception {
        Path manifestPath = Paths.get("output", "cleanup-manifest.txt").toAbsolutePath().normalize();
        Path parent = manifestPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(manifestPath, generatedFiles, StandardCharsets.UTF_8);
    }
}

class main_c {
    public static void construct(String filepath, String out) throws Exception {
        Main.compileApollo(filepath != null ? filepath : "main.apollo", out, new LinkedHashSet<>());
    }
}

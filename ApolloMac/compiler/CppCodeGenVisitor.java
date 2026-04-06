import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.ArrayList;

public class CppCodeGenVisitor extends compilerv1BaseVisitor<Void> {
    private static final class ApolloType {
        private final String name;
        private final List<ApolloType> arguments;

        private ApolloType(String name, List<ApolloType> arguments) {
            this.name = name;
            this.arguments = arguments;
        }
    }

    private static final class AssignTargetInfo {
        private final String renderedTarget;
        private final ApolloType valueType;
        private final ApolloType appendReceiverType;

        private AssignTargetInfo(String renderedTarget, ApolloType valueType, ApolloType appendReceiverType) {
            this.renderedTarget = renderedTarget;
            this.valueType = valueType;
            this.appendReceiverType = appendReceiverType;
        }
    }

    private final BufferedWriter out;
    private final boolean headerMode;
    private final Deque<String> returnTypes = new ArrayDeque<>();
    private final Deque<Map<String, ApolloType>> typeScopes = new ArrayDeque<>();
    private int indentLevel = 0;
    private final Set<String> includes = new LinkedHashSet<>();
    private final List<String> wildcardImports = new ArrayList<>();
    private final List<String> dependencies = new ArrayList<>();
    private final String moduleSymbol;

    public List<String> getDependencies() {
        return dependencies;
    }
    private final Map<String, String> instanceModes = new HashMap<>();
    private final Map<String, String> instanceTypes = new HashMap<>();
    private final Deque<String> enclosingTypes = new ArrayDeque<>();
    private final Set<String> declaredTypes = new LinkedHashSet<>();
    public CppCodeGenVisitor(String filename, String moduleKey) {
        try {
            Path outputPath = Paths.get(filename);
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            out = new BufferedWriter(new FileWriter(filename));
            headerMode = filename.endsWith(".hpp") || filename.endsWith(".h");
            moduleSymbol = sanitizeModuleSymbol(moduleKey);
            typeScopes.push(new HashMap<>());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void write(String s) {
        try {
            out.write(s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writec(String s, String filepath) {
        try {
            Path p = Paths.get(filepath);
            Path parent = p.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.write(p, s.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String indent() {
        return "    ".repeat(Math.max(0, indentLevel));
    }

    private void writeLine(String s) {
        write(indent() + s + "\n");
    }

    private String mapType(String type) {
        if (type == null) {
            return null;
        }
        if ("str".equals(type)) {
            return "std::string";
        }
        if ("i16".equals(type)) {
            return "std::int16_t";
        }
        if ("i32".equals(type)) {
            return "std::int32_t";
        }
        if ("i64".equals(type)) {
            return "std::int64_t";
        }
        if ("f64".equals(type)) {
            return "double";
        }
        if (declaredTypes.contains(type)) {
            return "apo_" + type;
        }
        return type;
    }

    // Use simpler built-in names for function signatures (int, long long, double, etc.)
    private String mapTypeForFunction(String type) {
        if (type == null) return "void";
        if ("str".equals(type)) return "std::string";
        if ("i16".equals(type)) return "short";
        if ("i32".equals(type)) return "int";
        if ("i64".equals(type)) return "long long";
        if ("f64".equals(type)) return "double";
        if (declaredTypes.contains(type)) return "apo_" + type;
        return type;
    }

    private String mapDeclaredType(String typeName) {
        if (typeName == null) {
            return null;
        }
        if (declaredTypes.contains(typeName)) {
            return "apo_" + typeName;
        }
        return typeName;
    }

    private void pushTypeScope() {
        typeScopes.push(new HashMap<>());
    }

    private void popTypeScope() {
        if (typeScopes.size() > 1) {
            typeScopes.pop();
        }
    }

    private void bindVariableType(String name, ApolloType type) {
        if (name == null || type == null || typeScopes.isEmpty()) {
            return;
        }
        typeScopes.peek().put(name, type);
    }

    private ApolloType resolveVariableType(String name) {
        for (Map<String, ApolloType> scope : typeScopes) {
            ApolloType type = scope.get(name);
            if (type != null) {
                return type;
            }
        }
        return null;
    }

    private ApolloType typeFromContext(compilerv1Parser.TypeRefContext ctx) {
        if (ctx == null) {
            return null;
        }
        if (ctx.functionType() != null) {
            List<ApolloType> arguments = new ArrayList<>();
            arguments.add(typeFromReturnContext(ctx.functionType().returnType()));
            if (ctx.functionType().functionTypeArgs() != null) {
                for (compilerv1Parser.TypeRefContext argument : ctx.functionType().functionTypeArgs().typeRef()) {
                    arguments.add(typeFromContext(argument));
                }
            }
            return new ApolloType("fn", arguments);
        }
        if (ctx.genericType() != null) {
            List<ApolloType> arguments = new ArrayList<>();
            for (compilerv1Parser.TypeRefContext argument : ctx.genericType().typeRef()) {
                arguments.add(typeFromContext(argument));
            }
            return new ApolloType(ctx.genericType().ID().getText(), arguments);
        }
        if (ctx.TYPE() != null) {
            return new ApolloType(ctx.TYPE().getText(), List.of());
        }
        if (ctx.FTYPE() != null) {
            return new ApolloType(ctx.FTYPE().getText(), List.of());
        }
        if (ctx.ID() != null) {
            return new ApolloType(ctx.ID().getText(), List.of());
        }
        return null;
    }

    private ApolloType typeFromReturnContext(compilerv1Parser.ReturnTypeContext ctx) {
        if (ctx == null || "void".equals(ctx.getText())) {
            return new ApolloType("void", List.of());
        }
        return typeFromContext(ctx.typeRef());
    }

    private String renderType(ApolloType type) {
        return renderType(type, false);
    }

    private String renderTypeForFunction(ApolloType type) {
        return renderType(type, true);
    }

    private String renderType(ApolloType type, boolean functionSignature) {
        if (type == null) {
            return functionSignature ? "void" : "auto";
        }
        if ("fn".equals(type.name)) {
            if (type.arguments.isEmpty()) {
                throw new IllegalArgumentException("fn<> requires a return type");
            }
            StringBuilder builder = new StringBuilder("std::function<");
            builder.append(renderType(type.arguments.get(0), true)).append("(");
            for (int index = 1; index < type.arguments.size(); index++) {
                if (index > 1) {
                    builder.append(", ");
                }
                builder.append(renderType(type.arguments.get(index), true));
            }
            builder.append(")>");
            return builder.toString();
        }
        if ("vector".equals(type.name)) {
            if (type.arguments.size() != 1) {
                throw new IllegalArgumentException("vector<T> expects exactly one type argument");
            }
            return "std::vector<" + renderType(type.arguments.get(0), functionSignature) + ">";
        }
        if ("hsh".equals(type.name)) {
            if (type.arguments.size() != 2) {
                throw new IllegalArgumentException("hsh<K,V> expects exactly two type arguments");
            }
            String keyType = renderType(type.arguments.get(0), functionSignature);
            String valueType = renderType(type.arguments.get(1), functionSignature);
            return "std::unordered_map<" + keyType + ", " + valueType + ", __apo_hash<" + keyType + ">>";
        }
        if (!type.arguments.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            builder.append(functionSignature ? mapTypeForFunction(type.name) : mapType(type.name)).append("<");
            for (int index = 0; index < type.arguments.size(); index++) {
                if (index > 0) {
                    builder.append(", ");
                }
                builder.append(renderType(type.arguments.get(index), functionSignature));
            }
            builder.append(">");
            return builder.toString();
        }
        return functionSignature ? mapTypeForFunction(type.name) : mapType(type.name);
    }

    private ApolloType valueTypeForIndexedAccess(ApolloType type) {
        if (type == null || type.arguments.isEmpty()) {
            return null;
        }
        if ("vector".equals(type.name)) {
            return type.arguments.get(0);
        }
        if ("hsh".equals(type.name) && type.arguments.size() > 1) {
            return type.arguments.get(1);
        }
        return null;
    }

    private ApolloType keyTypeForIndexedAccess(ApolloType type) {
        if (type == null) {
            return null;
        }
        if ("vector".equals(type.name)) {
            return new ApolloType("i32", List.of());
        }
        if ("hsh".equals(type.name) && !type.arguments.isEmpty()) {
            return type.arguments.get(0);
        }
        return null;
    }

    private String normalizeNativeLang(String lang) {
        if (lang == null) {
            return null;
        }
        if ("rust".equals(lang)) return "rs";
        if ("csharp".equals(lang)) return "cs";
        if ("python".equals(lang)) return "py";
        if ("javascript".equals(lang)) return "js";
        if ("typescript".equals(lang)) return "ts";
        if ("golang".equals(lang)) return "go";
        if ("ruby".equals(lang)) return "rb";
        if ("kotlin".equals(lang)) return "kt";
        return lang;
    }

    private void collectDeclaredTypes(org.antlr.v4.runtime.tree.ParseTree node) {
        if (node == null) {
            return;
        }
        if (node instanceof compilerv1Parser.ClassContext) {
            declaredTypes.add(((compilerv1Parser.ClassContext) node).ID().getText());
        } else if (node instanceof compilerv1Parser.StructContext) {
            declaredTypes.add(((compilerv1Parser.StructContext) node).ID().getText());
        } else if (node instanceof compilerv1Parser.InterfaceContext) {
            declaredTypes.add(((compilerv1Parser.InterfaceContext) node).ID().getText());
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            collectDeclaredTypes(node.getChild(i));
        }
    }

    private void collectIncludes(org.antlr.v4.runtime.tree.ParseTree node) {
        if (node == null) {
            return;
        }
        if (node instanceof compilerv1Parser.ImportStmtContext) {
            visitImportStmt((compilerv1Parser.ImportStmtContext) node);
        } else if (node instanceof compilerv1Parser.IncludeContext) {
            visitInclude((compilerv1Parser.IncludeContext) node);
        }
        if (node instanceof compilerv1Parser.InstanceContext
                && "crt".equals(((compilerv1Parser.InstanceContext) node).INSTANCE_MODE().getText())) {
            includes.add("any");
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            collectIncludes(node.getChild(i));
        }
    }

    private boolean usesGarbageCollector() {
        return includes.contains("gc/gc.h")
                || includes.contains("gc.h")
                || includes.contains("gc/gc_cpp.h")
                || includes.contains("gc_cpp.h");
    }

    private String toPackageSourcePath(String importPathText) {
        String[] parts = importPathText.split("\\.");
        if (parts.length == 0) {
            return importPathText;
        }
        boolean explicitExtension = "apollo".equals(parts[parts.length - 1]) || "aph".equals(parts[parts.length - 1]);
        int pathPartCount = explicitExtension ? parts.length - 1 : parts.length;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < pathPartCount; i++) {
            if (i > 0) {
                builder.append('/');
            }
            builder.append(parts[i]);
        }
        if (builder.length() == 0) {
            return importPathText;
        }
        builder.append(explicitExtension && "aph".equals(parts[parts.length - 1]) ? ".aph" : ".apollo");
        return builder.toString();
    }

    private String toPackageHeaderPath(String sourcePath) {
        if (sourcePath.endsWith(".apollo")) {
            return sourcePath.substring(0, sourcePath.length() - ".apollo".length()) + ".hpp";
        }
        if (sourcePath.endsWith(".aph")) {
            return sourcePath.substring(0, sourcePath.length() - ".aph".length()) + ".hpp";
        }
        return sourcePath + ".hpp";
    }

    private String sanitizeModuleSymbol(String raw) {
        String sanitized = raw.replaceAll("[^A-Za-z0-9]", "_");
        if (sanitized.isEmpty()) {
            return "module";
        }
        if (Character.isDigit(sanitized.charAt(0))) {
            return "module_" + sanitized;
        }
        return sanitized;
    }

    private String toModuleInitName(String sourcePath) {
        String normalized = sourcePath.replace('\\', '/');
        if (normalized.endsWith(".apollo")) {
            normalized = normalized.substring(0, normalized.length() - ".apollo".length());
        } else if (normalized.endsWith(".aph")) {
            normalized = normalized.substring(0, normalized.length() - ".aph".length());
        }
        return "__apo_init_" + sanitizeModuleSymbol(normalized);
    }

    private String currentModuleInitName() {
        return "__apo_init_" + moduleSymbol;
    }

    private String rewriteAsyncInteropCalls(String lang, String code) {
        if ("java".equals(lang)) {
            return code
                    .replace("async_write(", "ApoAsyncIR.async_write(")
                    .replace("async_read(", "ApoAsyncIR.async_read(")
                    .replace("async_exec(", "ApoAsyncIR.async_exec(");
        }
        if ("cs".equals(lang)) {
            return code
                    .replace("async_write(", "ApoAsyncIR.async_write(")
                    .replace("async_read(", "ApoAsyncIR.async_read(")
                    .replace("async_exec(", "ApoAsyncIR.async_exec(");
        }
        if ("rs".equals(lang)) {
            return code
                    .replace("async_write(", "async_write!(")
                    .replace("async_read(", "async_read!(")
                    .replace("async_exec(", "async_exec!(");
        }
        return code;
    }

    private String buildShellExecScript() {
        return String.join("\n",
                "#!/usr/bin/env bash",
                "set -euo pipefail",
                "",
                "Kind=${1-}",
                "Name=${2-}",
                "scriptDir=$(CDPATH= cd -- \"$(dirname -- \"${BASH_SOURCE[0]}\")\" && pwd -P)",
                "compilerDir=$(dirname -- \"$scriptDir\")",
                "toolchainEnv=\"$compilerDir/toolchain-env.sh\"",
                "if [ -f \"$toolchainEnv\" ]; then . \"$toolchainEnv\"; fi",
                "outputDir=\"$compilerDir/output\"",
                "irPath=\"$outputDir/apo_ir.txt\"",
                "clangExe=${APOLLO_LLVM_BIN:+$APOLLO_LLVM_BIN/clang}",
                "clangxxExe=${APOLLO_LLVM_BIN:+$APOLLO_LLVM_BIN/clang++}",
                "javacExe=${APOLLO_JAVA_BIN:+$APOLLO_JAVA_BIN/javac}",
                "javaExe=${APOLLO_JAVA_BIN:+$APOLLO_JAVA_BIN/java}",
                "if [ -z \"$clangExe\" ] || [ ! -x \"$clangExe\" ]; then clangExe=$(command -v clang); fi",
                "if [ -z \"$clangxxExe\" ] || [ ! -x \"$clangxxExe\" ]; then clangxxExe=$(command -v clang++); fi",
                "if [ -z \"$javacExe\" ] || [ ! -x \"$javacExe\" ]; then javacExe=$(command -v javac); fi",
                "if [ -z \"$javaExe\" ] || [ ! -x \"$javaExe\" ]; then javaExe=$(command -v java); fi",
                "clangTargetArgs=()",
                "if [ -n \"${APOLLO_CLANG_TARGET:-}\" ]; then clangTargetArgs+=(--target=\"$APOLLO_CLANG_TARGET\"); fi",
                "if [ -n \"${APOLLO_SYSROOT:-}\" ]; then clangTargetArgs+=(--sysroot=\"$APOLLO_SYSROOT\"); fi",
                "if [ -n \"${APOLLO_GCC_TOOLCHAIN:-}\" ]; then clangTargetArgs+=(--gcc-toolchain=\"$APOLLO_GCC_TOOLCHAIN\"); fi",
                "",
                "unescape_field() {",
                "    local value=$1",
                "    local result=\"\"",
                "    local index=0",
                "    local char=\"\"",
                "    local next_char=\"\"",
                "    while [ $index -lt ${#value} ]; do",
                "        char=${value:$index:1}",
                "        if [ \"$char\" = '\\\\' ] && [ $((index + 1)) -lt ${#value} ]; then",
                "            next_char=${value:$((index + 1)):1}",
                "            case \"$next_char\" in",
                "                n) result+=$'\\n' ;;",
                "                r) result+=$'\\r' ;;",
                "                t) result+=$'\\t' ;;",
                "                *) result+=\"$next_char\" ;;",
                "            esac",
                "            index=$((index + 2))",
                "            continue",
                "        fi",
                "        result+=\"$char\"",
                "        index=$((index + 1))",
                "    done",
                "    printf '%s' \"$result\"",
                "}",
                "",
                "find_record() {",
                "    if [ ! -f \"$irPath\" ]; then",
                "        return 1",
                "    fi",
                "",
                "    local latest_lang=\"\"",
                "    local latest_payload=\"\"",
                "    local raw_kind=\"\"",
                "    local raw_name=\"\"",
                "    local raw_type=\"\"",
                "    local raw_lang=\"\"",
                "    local raw_payload=\"\"",
                "    local decoded_kind=\"\"",
                "    local decoded_name=\"\"",
                "",
                "    while IFS=$'\\t' read -r raw_kind raw_name raw_type raw_lang raw_payload; do",
                "        if [ -z \"$raw_lang$raw_payload\" ]; then",
                "            continue",
                "        fi",
                "",
                "        decoded_kind=$(unescape_field \"$raw_kind\")",
                "        decoded_name=$(unescape_field \"$raw_name\")",
                "        if [ -n \"$Kind\" ] && [ \"$decoded_kind\" != \"$Kind\" ]; then",
                "            continue",
                "        fi",
                "        if [ \"$decoded_name\" != \"$Name\" ]; then",
                "            continue",
                "        fi",
                "",
                "        latest_lang=$raw_lang",
                "        latest_payload=$raw_payload",
                "    done < \"$irPath\"",
                "",
                "    if [ -z \"$latest_lang\" ] && [ -z \"$latest_payload\" ]; then",
                "        return 1",
                "    fi",
                "",
                "    printf '%s\\t%s' \"$latest_lang\" \"$latest_payload\"",
                "}",
                "",
                "run_payload() {",
                "    local lang=$1",
                "    local code=$2",
                "    mkdir -p \"$outputDir\"",
                "    case \"$lang\" in",
                "        cpp)",
                "            printf '%s' \"$code\" > \"$outputDir/apo_async_cpp.cpp\"",
                "            \"$clangxxExe\" \"${clangTargetArgs[@]}\" \"$outputDir/apo_async_cpp.cpp\" -o \"$outputDir/apo_async_cpp\"",
                "            \"$outputDir/apo_async_cpp\"",
                "            ;;",
                "        c)",
                "            printf '%s' \"$code\" > \"$outputDir/apo_async_c.c\"",
                "            \"$clangExe\" \"${clangTargetArgs[@]}\" \"$outputDir/apo_async_c.c\" -o \"$outputDir/apo_async_c\"",
                "            \"$outputDir/apo_async_c\"",
                "            ;;",
                "        rs)",
                "            printf '%s' \"$code\" > \"$outputDir/apo_async_rs.rs\"",
                "            rustc \"$outputDir/apo_async_rs.rs\" -o \"$outputDir/apo_async_rs\"",
                "            \"$outputDir/apo_async_rs\"",
                "            ;;",
                "        java)",
                "            printf '%s' \"$code\" > \"$outputDir/ApoAsyncTask.java\"",
                "            \"$javacExe\" \"$outputDir/ApoAsyncTask.java\" -d \"$outputDir\"",
                "            \"$javaExe\" -cp \"$outputDir\" ApoAsyncTask",
                "            ;;",
                "        cs)",
                "            printf '%s' \"$code\" > \"$outputDir/ApoAsyncTask.cs\"",
                "            if command -v csc >/dev/null 2>&1; then",
                "                csc /nologo /out:\"$outputDir/ApoAsyncTask.exe\" \"$outputDir/ApoAsyncTask.cs\"",
                "            elif command -v mcs >/dev/null 2>&1; then",
                "                mcs -out:\"$outputDir/ApoAsyncTask.exe\" \"$outputDir/ApoAsyncTask.cs\"",
                "            else",
                "                echo 'csc or mcs is required for cs async payloads' >&2",
                "                return 1",
                "            fi",
                "            if command -v mono >/dev/null 2>&1; then",
                "                mono \"$outputDir/ApoAsyncTask.exe\"",
                "            elif command -v dotnet >/dev/null 2>&1; then",
                "                dotnet \"$outputDir/ApoAsyncTask.exe\"",
                "            else",
                "                \"$outputDir/ApoAsyncTask.exe\"",
                "            fi",
                "            ;;",
                "        py)",
                "            printf '%s' \"$code\" > \"$outputDir/apo_async.py\"",
                "            if command -v python3 >/dev/null 2>&1; then",
                "                python3 \"$outputDir/apo_async.py\"",
                "            else",
                "                python \"$outputDir/apo_async.py\"",
                "            fi",
                "            ;;",
                "        js)",
                "            printf '%s' \"$code\" > \"$outputDir/apo_async.js\"",
                "            node \"$outputDir/apo_async.js\"",
                "            ;;",
                "        ts)",
                "            printf '%s' \"$code\" > \"$outputDir/apo_async.ts\"",
                "            tsc \"$outputDir/apo_async.ts\" --outDir \"$outputDir\"",
                "            node \"$outputDir/apo_async.js\"",
                "            ;;",
                "        go)",
                "            printf '%s' \"$code\" > \"$outputDir/apo_async_go.go\"",
                "            go build -o \"$outputDir/apo_async_go\" \"$outputDir/apo_async_go.go\"",
                "            \"$outputDir/apo_async_go\"",
                "            ;;",
                "        php)",
                "            printf '%s' \"$code\" > \"$outputDir/apo_async.php\"",
                "            php \"$outputDir/apo_async.php\"",
                "            ;;",
                "        rb)",
                "            printf '%s' \"$code\" > \"$outputDir/apo_async.rb\"",
                "            ruby \"$outputDir/apo_async.rb\"",
                "            ;;",
                "        kt)",
                "            printf '%s' \"$code\" > \"$outputDir/ApoAsyncTask.kt\"",
                "            kotlinc \"$outputDir/ApoAsyncTask.kt\" -include-runtime -d \"$outputDir/ApoAsyncTask.jar\"",
                "            java -jar \"$outputDir/ApoAsyncTask.jar\"",
                "            ;;",
                "        *)",
                "            echo \"unsupported stored language: $lang\" >&2",
                "            return 1",
                "            ;;",
                "    esac",
                "}",
                "",
                "record=$(find_record) || { echo \"IR entry not found: $Kind $Name\" >&2; exit 1; }",
                "record_lang=${record%%$'\\t'*}",
                "record_payload=${record#*$'\\t'}",
                "run_payload \"$(unescape_field \"$record_lang\")\" \"$(unescape_field \"$record_payload\")\"") + "\n";
    }

    private String buildCppIrPrelude() {
        return String.join("\n",
            "#include <cstdlib>",
                "#include <filesystem>",
                "#include <fstream>",
                "#include <string>",
                "#include <vector>",
                "",
                "static std::string __apo_ir_escape(const std::string& value) {",
                "    std::string escaped;",
                "    for (char ch : value) {",
                "        if (ch == '\\\\') escaped += \"\\\\\\\\\";",
                "        else if (ch == '\\t') escaped += \"\\\\t\";",
                "        else if (ch == '\\n') escaped += \"\\\\n\";",
                "        else if (ch == '\\r') escaped += \"\\\\r\";",
                "        else escaped.push_back(ch);",
                "    }",
                "    return escaped;",
                "}",
                "",
                "static std::vector<std::string> __apo_ir_parse_line(const std::string& line) {",
                "    std::vector<std::string> fields;",
                "    std::string current;",
                "    bool escaping = false;",
                "    for (char ch : line) {",
                "        if (escaping) {",
                "            if (ch == 'n') current.push_back('\\n');",
                "            else if (ch == 'r') current.push_back('\\r');",
                "            else if (ch == 't') current.push_back('\\t');",
                "            else current.push_back(ch);",
                "            escaping = false;",
                "            continue;",
                "        }",
                "        if (ch == '\\\\') {",
                "            escaping = true;",
                "            continue;",
                "        }",
                "        if (ch == '\\t') {",
                "            fields.push_back(current);",
                "            current.clear();",
                "            continue;",
                "        }",
                "        current.push_back(ch);",
                "    }",
                "    if (escaping) current.push_back('\\\\');",
                "    fields.push_back(current);",
                "    return fields;",
                "}",
                "",
                "static void async_write(const std::string& kind, const std::string& name, const std::string& typeName, const std::string& payload, const std::string& sourceLang = \"cpp\") {",
                "    std::filesystem::create_directories(\"output\");",
                "    std::ofstream out(\"output/apo_ir.txt\", std::ios::app);",
                "    out << __apo_ir_escape(kind) << '\\t' << __apo_ir_escape(name) << '\\t' << __apo_ir_escape(typeName) << '\\t' << __apo_ir_escape(sourceLang) << '\\t' << __apo_ir_escape(payload) << '\\n';",
                "}",
                "",
                "static std::string async_read(const std::string& kind, const std::string& name) {",
                "    std::ifstream in(\"output/apo_ir.txt\");",
                "    if (!in) return \"\";",
                "    std::vector<std::string> lines;",
                "    std::string line;",
                "    while (std::getline(in, line)) {",
                "        if (!line.empty()) lines.push_back(line);",
                "    }",
                "    for (auto it = lines.rbegin(); it != lines.rend(); ++it) {",
                "        std::vector<std::string> fields = __apo_ir_parse_line(*it);",
                "        if (fields.size() < 5) continue;",
                "        if (!kind.empty() && fields[0] != kind) continue;",
                "        if (fields[1] != name) continue;",
                "        return fields[4];",
                "    }",
                "    return \"\";",
                "}",
                "",
                "static std::string async_read(const std::string& name) {",
                "    return async_read(\"\", name);",
                "}",
                "",
                "static std::string __apo_clangTargetFlags() {",
                "    const char* target = std::getenv(\"APOLLO_CLANG_TARGET\");",
                "    std::string flags;",
                "    if (target != nullptr && *target != '\\0') flags = \"--target=\" + std::string(target);",
                "    const char* sysroot = std::getenv(\"APOLLO_SYSROOT\");",
                "    if (sysroot != nullptr && *sysroot != '\\0') flags += (flags.empty() ? std::string() : std::string(\" \")) + \"--sysroot=\\\"\" + std::string(sysroot) + \"\\\"\";",
                "    const char* gccToolchain = std::getenv(\"APOLLO_GCC_TOOLCHAIN\");",
                "    if (gccToolchain != nullptr && *gccToolchain != '\\0') flags += (flags.empty() ? std::string() : std::string(\" \")) + \"--gcc-toolchain=\\\"\" + std::string(gccToolchain) + \"\\\"\";",
                "    return flags;",
                "}",
                "",
                "static std::string __apo_shellQuote(const std::string& value) {",
                "    std::string quoted = \"'\";",
                "    for (char ch : value) {",
                "        if (ch == '\'') quoted += \"'\\\\''\";",
                "        else quoted.push_back(ch);",
                "    }",
                "    quoted += \"'\";",
                "    return quoted;",
                "}",
                "",
                "static std::string __apo_toolCommand(const char* envDirVar, const std::string& toolName) {",
                "    const char* envDir = std::getenv(envDirVar);",
                "    if (envDir != nullptr && *envDir != '\\0') {",
                "        return __apo_shellQuote(std::string(envDir) + \"/\" + toolName);",
                "    }",
                "    return toolName;",
                "}",
                "",
                "static int async_exec(const std::string& kind, const std::string& name) {",
                "    std::string command = \"bash output/apo_ir_exec.sh \\\"\" + kind + \"\\\" \\\"\" + name + \"\\\"\";",
                "    return system(command.c_str());",
                "}",
                "",
                "static int async_exec(const std::string& name) {",
                "    return async_exec(\"\", name);",
                "}") + "\n\n";
    }

    private String buildPythonIrPrelude() {
        return String.join("\n",
                "import os",
                "import subprocess",
                "from pathlib import Path",
                "",
                "def _apo_ir_escape(value):",
                "    text = \"\" if value is None else str(value)",
                "    return text.replace('\\\\', '\\\\\\\\').replace('\\t', '\\\\t').replace('\\n', '\\\\n').replace('\\r', '\\\\r')",
                "",
                "def _apo_ir_parse_line(line):",
                "    fields = []",
                "    current = []",
                "    escaping = False",
                "    for ch in line:",
                "        if escaping:",
                "            if ch == 'n':",
                "                current.append('\\n')",
                "            elif ch == 'r':",
                "                current.append('\\r')",
                "            elif ch == 't':",
                "                current.append('\\t')",
                "            else:",
                "                current.append(ch)",
                "            escaping = False",
                "            continue",
                "        if ch == '\\\\':",
                "            escaping = True",
                "            continue",
                "        if ch == '\\t':",
                "            fields.append(''.join(current))",
                "            current = []",
                "            continue",
                "        current.append(ch)",
                "    if escaping:",
                "        current.append('\\\\')",
                "    fields.append(''.join(current))",
                "    return fields",
                "",
                "def async_write(kind, name, type_name, payload, source_lang='py'):",
                "    ir_path = Path('output') / 'apo_ir.txt'",
                "    ir_path.parent.mkdir(parents=True, exist_ok=True)",
                "    record = '\\t'.join([",
                "        _apo_ir_escape(kind),",
                "        _apo_ir_escape(name),",
                "        _apo_ir_escape(type_name),",
                "        _apo_ir_escape(source_lang),",
                "        _apo_ir_escape(payload),",
                "    ]) + '\\n'",
                "    with ir_path.open('a', encoding='utf-8') as handle:",
                "        handle.write(record)",
                "",
                "def async_read(kind_or_name, name=None):",
                "    kind = '' if name is None else str(kind_or_name)",
                "    symbol_name = str(kind_or_name) if name is None else str(name)",
                "    ir_path = Path('output') / 'apo_ir.txt'",
                "    if not ir_path.exists():",
                "        return ''",
                "    lines = ir_path.read_text(encoding='utf-8').splitlines()",
                "    for line in reversed(lines):",
                "        if not line:",
                "            continue",
                "        fields = _apo_ir_parse_line(line)",
                "        if len(fields) < 5:",
                "            continue",
                "        if kind and fields[0] != kind:",
                "            continue",
                "        if fields[1] != symbol_name:",
                "            continue",
                "        return fields[4]",
                "    return ''",
                "",
                "def async_exec(kind_or_name, name=None):",
                "    kind = '' if name is None else str(kind_or_name)",
                "    symbol_name = str(kind_or_name) if name is None else str(name)",
                "    command = ['bash', 'output/apo_ir_exec.sh', kind, symbol_name]",
                "    result = subprocess.run(command)",
                "    return result.returncode") + "\n\n";
    }

    private String buildJavaIrPrelude() {
        return String.join("\n",
                "import java.io.IOException;",
                "import java.nio.charset.StandardCharsets;",
                "import java.nio.file.Files;",
                "import java.nio.file.Path;",
                "import java.nio.file.Paths;",
                "import java.nio.file.StandardOpenOption;",
                "import java.util.ArrayList;",
                "import java.util.List;",
                "import java.lang.ProcessBuilder;",
                "",
                "class ApoAsyncIR {",
                "    private static String escape(String value) {",
                "        String text = value == null ? \"\" : value;",
                "        return text.replace(\"\\\\\", \"\\\\\\\\\")",
                "                .replace(\"\\t\", \"\\\\t\")",
                "                .replace(\"\\n\", \"\\\\n\")",
                "                .replace(\"\\r\", \"\\\\r\");",
                "    }",
                "",
                "    private static List<String> parseLine(String line) {",
                "        List<String> fields = new ArrayList<>();",
                "        StringBuilder current = new StringBuilder();",
                "        boolean escaping = false;",
                "        for (int i = 0; i < line.length(); i++) {",
                "            char ch = line.charAt(i);",
                "            if (escaping) {",
                "                if (ch == 'n') current.append('\\n');",
                "                else if (ch == 'r') current.append('\\r');",
                "                else if (ch == 't') current.append('\\t');",
                "                else current.append(ch);",
                "                escaping = false;",
                "                continue;",
                "            }",
                "            if (ch == '\\\\') {",
                "                escaping = true;",
                "                continue;",
                "            }",
                "            if (ch == '\\t') {",
                "                fields.add(current.toString());",
                "                current.setLength(0);",
                "                continue;",
                "            }",
                "            current.append(ch);",
                "        }",
                "        if (escaping) current.append('\\\\');",
                "        fields.add(current.toString());",
                "        return fields;",
                "    }",
                "",
                "    static void async_write(String kind, String name, String typeName, String payload) {",
                "        async_write(kind, name, typeName, payload, \"java\");",
                "    }",
                "",
                "    static void async_write(String kind, String name, String typeName, String payload, String sourceLang) {",
                "        Path irPath = Paths.get(\"output\", \"apo_ir.txt\");",
                "        try {",
                "            Files.createDirectories(irPath.getParent());",
                "            String record = String.join(\"\\t\",",
                "                    escape(kind),",
                "                    escape(name),",
                "                    escape(typeName),",
                "                    escape(sourceLang),",
                "                    escape(payload)) + System.lineSeparator();",
                "            Files.writeString(irPath, record, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);",
                "        } catch (IOException ex) {",
                "            throw new RuntimeException(ex);",
                "        }",
                "    }",
                "",
                "    static String async_read(String name) {",
                "        return async_read(\"\", name);",
                "    }",
                "",
                "    static String async_read(String kind, String name) {",
                "        Path irPath = Paths.get(\"output\", \"apo_ir.txt\");",
                "        if (!Files.exists(irPath)) {",
                "            return \"\";",
                "        }",
                "        try {",
                "            List<String> lines = Files.readAllLines(irPath, StandardCharsets.UTF_8);",
                "            for (int i = lines.size() - 1; i >= 0; i--) {",
                "                String line = lines.get(i);",
                "                if (line.isEmpty()) continue;",
                "                List<String> fields = parseLine(line);",
                "                if (fields.size() < 5) continue;",
                "                if (!kind.isEmpty() && !fields.get(0).equals(kind)) continue;",
                "                if (!fields.get(1).equals(name)) continue;",
                "                return fields.get(4);",
                "            }",
                "            return \"\";",
                "        } catch (IOException ex) {",
                "            throw new RuntimeException(ex);",
                "        }",
                "    }",
                "",
                "    static int async_exec(String name) {",
                "        return async_exec(\"\", name);",
                "    }",
                "",
                "    static int async_exec(String kind, String name) {",
                "        try {",
                "            List<String> command = List.of(\"bash\", \"output/apo_ir_exec.sh\", kind, name);",
                "            Process process = new ProcessBuilder(command)",
                "                    .inheritIO()",
                "                    .start();",
                "            return process.waitFor();",
                "        } catch (IOException ex) {",
                "            throw new RuntimeException(ex);",
                "        } catch (InterruptedException ex) {",
                "            Thread.currentThread().interrupt();",
                "            throw new RuntimeException(ex);",
                "        }",
                "    }",
                "}") + "\n\n";
    }

    private String buildJavaScriptIrPrelude(String sourceLang, boolean typescript) {
        List<String> lines = new ArrayList<>();
        if (typescript) {
            lines.add("declare function require(name: string): any;");
        }
        lines.add("const fs = require(\"fs\");");
        lines.add("const path = require(\"path\");");
        lines.add("");
        lines.add("function __apoIrEscape(value) {");
        lines.add("    const text = value == null ? \"\" : String(value);");
        lines.add("    return text.replace(/\\\\/g, \"\\\\\\\\\").replace(/\\t/g, \"\\\\t\").replace(/\\n/g, \"\\\\n\").replace(/\\r/g, \"\\\\r\");");
        lines.add("}");
        lines.add("");
        lines.add("function __apoIrParseLine(line) {");
        lines.add("    const fields = [];");
        lines.add("    let current = \"\";");
        lines.add("    let escaping = false;");
        lines.add("    for (const ch of line) {");
        lines.add("        if (escaping) {");
        lines.add("            if (ch === \"n\") current += \"\\n\";");
        lines.add("            else if (ch === \"r\") current += \"\\r\";");
        lines.add("            else if (ch === \"t\") current += \"\\t\";");
        lines.add("            else current += ch;");
        lines.add("            escaping = false;");
        lines.add("            continue;");
        lines.add("        }");
        lines.add("        if (ch === \"\\\\\") {");
        lines.add("            escaping = true;");
        lines.add("            continue;");
        lines.add("        }");
        lines.add("        if (ch === \"\\t\") {");
        lines.add("            fields.push(current);");
        lines.add("            current = \"\";");
        lines.add("            continue;");
        lines.add("        }");
        lines.add("        current += ch;");
        lines.add("    }");
        lines.add("    if (escaping) current += \"\\\\\";");
        lines.add("    fields.push(current);");
        lines.add("    return fields;");
        lines.add("}");
        lines.add("");
        lines.add("function async_write(kind, name, typeName, payload, sourceLang = \"" + sourceLang + "\") {");
        lines.add("    const irPath = path.join(\"output\", \"apo_ir.txt\");");
        lines.add("    fs.mkdirSync(path.dirname(irPath), { recursive: true });");
        lines.add("    const record = [kind, name, typeName, sourceLang, payload].map(__apoIrEscape).join(\"\\t\") + \"\\n\";");
        lines.add("    fs.appendFileSync(irPath, record, \"utf8\");");
        lines.add("}");
        lines.add("");
        lines.add("function async_read(kindOrName, maybeName) {");
        lines.add("    const kind = maybeName === undefined ? \"\" : String(kindOrName ?? \"\");");
        lines.add("    const name = maybeName === undefined ? String(kindOrName ?? \"\") : String(maybeName ?? \"\");");
        lines.add("    const irPath = path.join(\"output\", \"apo_ir.txt\");");
        lines.add("    if (!fs.existsSync(irPath)) return \"\";");
        lines.add("    const lines = fs.readFileSync(irPath, \"utf8\").split(/\\r?\\n/);");
        lines.add("    for (let index = lines.length - 1; index >= 0; index -= 1) {");
        lines.add("        const line = lines[index];");
        lines.add("        if (!line) continue;");
        lines.add("        const fields = __apoIrParseLine(line);");
        lines.add("        if (fields.length < 5) continue;");
        lines.add("        if (kind && fields[0] !== kind) continue;");
        lines.add("        if (fields[1] !== name) continue;");
        lines.add("        return fields[4];");
        lines.add("    }");
        lines.add("    return \"\";");
        lines.add("}");
        lines.add("");
        lines.add("function async_exec(kindOrName, maybeName) {");
        lines.add("    const kind = maybeName === undefined ? \"\" : String(kindOrName ?? \"\");");
        lines.add("    const name = maybeName === undefined ? String(kindOrName ?? \"\") : String(maybeName ?? \"\");");
        lines.add("    try {");
        lines.add("        const command = { file: \"bash\", args: [\"output/apo_ir_exec.sh\", kind, name] };");
        lines.add("        require(\"child_process\").execFileSync(command.file, command.args, { stdio: \"inherit\" });");
        lines.add("        return 0;");
        lines.add("    } catch (error) {");
        lines.add("        return error && typeof error.status === \"number\" ? error.status : -1;");
        lines.add("    }");
        lines.add("}");
        return String.join("\n", lines) + "\n\n";
    }

    private String buildRustIrPrelude() {
        return String.join("\n",
        "#[allow(unused_macros)]",
                "use std::fs::{self, OpenOptions};",
                "use std::io::Write;",
                "use std::process::Command;",
                "",
                "fn __apo_ir_escape(value: &str) -> String {",
                "    value.replace('\\\\', \"\\\\\\\\\").replace('\\t', \"\\\\t\").replace('\\n', \"\\\\n\").replace('\\r', \"\\\\r\")",
                "}",
                "",
                "fn __apo_ir_parse_line(line: &str) -> Vec<String> {",
                "    let mut fields: Vec<String> = Vec::new();",
                "    let mut current = String::new();",
                "    let mut escaping = false;",
                "    for ch in line.chars() {",
                "        if escaping {",
                "            match ch {",
                "                'n' => current.push('\\n'),",
                "                'r' => current.push('\\r'),",
                "                't' => current.push('\\t'),",
                "                _ => current.push(ch),",
                "            }",
                "            escaping = false;",
                "            continue;",
                "        }",
                "        if ch == '\\\\' { escaping = true; continue; }",
                "        if ch == '\\t' { fields.push(current); current = String::new(); continue; }",
                "        current.push(ch);",
                "    }",
                "    if escaping { current.push('\\\\'); }",
                "    fields.push(current);",
                "    fields",
                "}",
                "",
                "fn __apo_async_write(kind: &str, name: &str, type_name: &str, payload: &str, source_lang: &str) {",
                "    let _ = fs::create_dir_all(\"output\");",
                "    let mut file = OpenOptions::new().create(true).append(true).open(\"output/apo_ir.txt\").unwrap();",
                "    writeln!(file, \"{}\\t{}\\t{}\\t{}\\t{}\", __apo_ir_escape(kind), __apo_ir_escape(name), __apo_ir_escape(type_name), __apo_ir_escape(source_lang), __apo_ir_escape(payload)).unwrap();",
                "}",
                "",
                "fn __apo_async_read(kind: &str, name: &str) -> String {",
                "    let content = fs::read_to_string(\"output/apo_ir.txt\").unwrap_or_default();",
                "    for line in content.lines().rev() {",
                "        if line.is_empty() { continue; }",
                "        let fields = __apo_ir_parse_line(line);",
                "        if fields.len() < 5 { continue; }",
                "        if !kind.is_empty() && fields[0] != kind { continue; }",
                "        if fields[1] != name { continue; }",
                "        return fields[4].clone();",
                "    }",
                "    String::new()",
                "}",
                "",
                "fn __apo_async_exec(kind: &str, name: &str) -> i32 {",
                "    let status = Command::new(\"bash\")",
                "        .args([\"output/apo_ir_exec.sh\", kind, name])",
                "        .status()",
                "        .unwrap();",
                "    status.code().unwrap_or(-1)",
                "}",
                "",
                "macro_rules! async_write {",
                "    ($kind:expr, $name:expr, $type_name:expr, $payload:expr) => { __apo_async_write($kind, $name, $type_name, $payload, \"rs\") };",
                "    ($kind:expr, $name:expr, $type_name:expr, $payload:expr, $source_lang:expr) => { __apo_async_write($kind, $name, $type_name, $payload, $source_lang) };",
                "}",
                "",
                "macro_rules! async_read {",
                "    ($name:expr) => { __apo_async_read(\"\", $name) };",
                "    ($kind:expr, $name:expr) => { __apo_async_read($kind, $name) };",
                "}",
                "",
                "macro_rules! async_exec {",
                "    ($name:expr) => { __apo_async_exec(\"\", $name) };",
                "    ($kind:expr, $name:expr) => { __apo_async_exec($kind, $name) };",
                "}") + "\n\n";
    }

    private String buildCSharpIrPrelude() {
        return String.join("\n",
                "using System;",
                "using System.Collections.Generic;",
                "using System.Diagnostics;",
                "using System.IO;",
                "using System.Text;",
                "",
                "static class ApoAsyncIR {",
                "    private static string Escape(string value) {",
                "        string text = value ?? string.Empty;",
                "        return text.Replace(\\\"\\\\\\\", \\\"\\\\\\\\\\\\\\\").Replace(\\\"\\t\\\", \\\"\\\\t\\\").Replace(\\\"\\n\\\", \\\"\\\\n\\\").Replace(\\\"\\r\\\", \\\"\\\\r\\\");",
                "    }",
                "",
                "    private static List<string> ParseLine(string line) {",
                "        List<string> fields = new List<string>();",
                "        StringBuilder current = new StringBuilder();",
                "        bool escaping = false;",
                "        foreach (char ch in line) {",
                "            if (escaping) {",
                "                if (ch == 'n') current.Append('\\n');",
                "                else if (ch == 'r') current.Append('\\r');",
                "                else if (ch == 't') current.Append('\\t');",
                "                else current.Append(ch);",
                "                escaping = false;",
                "                continue;",
                "            }",
                "            if (ch == '\\\\') { escaping = true; continue; }",
                "            if (ch == '\\t') { fields.Add(current.ToString()); current.Clear(); continue; }",
                "            current.Append(ch);",
                "        }",
                "        if (escaping) current.Append('\\\\');",
                "        fields.Add(current.ToString());",
                "        return fields;",
                "    }",
                "",
                "    public static void async_write(string kind, string name, string typeName, string payload) {",
                "        async_write(kind, name, typeName, payload, \"cs\");",
                "    }",
                "",
                "    public static void async_write(string kind, string name, string typeName, string payload, string sourceLang) {",
                "        Directory.CreateDirectory(\"output\");",
                "        string record = string.Join(\"\\t\", Escape(kind), Escape(name), Escape(typeName), Escape(sourceLang), Escape(payload)) + Environment.NewLine;",
                "        File.AppendAllText(Path.Combine(\"output\", \"apo_ir.txt\"), record, Encoding.UTF8);",
                "    }",
                "",
                "    public static string async_read(string name) {",
                "        return async_read(\"\", name);",
                "    }",
                "",
                "    public static string async_read(string kind, string name) {",
                "        string path = Path.Combine(\"output\", \"apo_ir.txt\");",
                "        if (!File.Exists(path)) return string.Empty;",
                "        string[] lines = File.ReadAllLines(path, Encoding.UTF8);",
                "        for (int index = lines.Length - 1; index >= 0; index -= 1) {",
                "            string line = lines[index];",
                "            if (string.IsNullOrEmpty(line)) continue;",
                "            List<string> fields = ParseLine(line);",
                "            if (fields.Count < 5) continue;",
                "            if (!string.IsNullOrEmpty(kind) && fields[0] != kind) continue;",
                "            if (fields[1] != name) continue;",
                "            return fields[4];",
                "        }",
                "        return string.Empty;",
                "    }",
                "",
                "    public static int async_exec(string name) {",
                "        return async_exec(\"\", name);",
                "    }",
                "",
                "    public static int async_exec(string kind, string name) {",
                "        Process process = new Process();",
                "        process.StartInfo = new ProcessStartInfo {",
                "            FileName = \"bash\",",
                "            Arguments = \"output/apo_ir_exec.sh \\\"\" + kind + \"\\\" \\\"\" + name + \"\\\"\",",
                "            UseShellExecute = false",
                "        };",
                "        process.Start();",
                "        process.WaitForExit();",
                "        return process.ExitCode;",
                "    }",
                "}") + "\n\n";
    }

    private String buildGoIrPrelude() {
        return String.join("\n",
                "package main",
                "",
                "import (",
                "    \"os\"",
                "    \"os/exec\"",
                "    \"path/filepath\"",
                "    \"strings\"",
                ")",
                "",
                "func apoIrEscape(value string) string {",
                "    replacer := strings.NewReplacer(\\\"\\\\\\\", \\\"\\\\\\\\\\\\\\\", \\\"\\t\\\", \\\"\\\\t\\\", \\\"\\n\\\", \\\"\\\\n\\\", \\\"\\r\\\", \\\"\\\\r\\\")",
                "    return replacer.Replace(value)",
                "}",
                "",
                "func apoIrParseLine(line string) []string {",
                "    fields := []string{}",
                "    var current strings.Builder",
                "    escaping := false",
                "    for _, ch := range line {",
                "        if escaping {",
                "            switch ch {",
                "            case 'n': current.WriteRune('\\n')",
                "            case 'r': current.WriteRune('\\r')",
                "            case 't': current.WriteRune('\\t')",
                "            default: current.WriteRune(ch)",
                "            }",
                "            escaping = false",
                "            continue",
                "        }",
                "        if ch == '\\\\' { escaping = true; continue }",
                "        if ch == '\\t' { fields = append(fields, current.String()); current.Reset(); continue }",
                "        current.WriteRune(ch)",
                "    }",
                "    if escaping { current.WriteRune('\\\\') }",
                "    fields = append(fields, current.String())",
                "    return fields",
                "}",
                "",
                "func async_write(args ...string) {",
                "    if len(args) < 4 { return }",
                "    kind, name, typeName, payload := args[0], args[1], args[2], args[3]",
                "    sourceLang := \"go\"",
                "    if len(args) > 4 { sourceLang = args[4] }",
                "    _ = os.MkdirAll(\"output\", 0o755)",
                "    record := strings.Join([]string{apoIrEscape(kind), apoIrEscape(name), apoIrEscape(typeName), apoIrEscape(sourceLang), apoIrEscape(payload)}, \"\\t\") + \"\\n\"",
                "    file, _ := os.OpenFile(filepath.Join(\"output\", \"apo_ir.txt\"), os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0o644)",
                "    defer file.Close()",
                "    _, _ = file.WriteString(record)",
                "}",
                "",
                "func async_read(args ...string) string {",
                "    kind := \"\"",
                "    name := \"\"",
                "    if len(args) == 1 { name = args[0] } else if len(args) > 1 { kind = args[0]; name = args[1] }",
                "    content, err := os.ReadFile(filepath.Join(\"output\", \"apo_ir.txt\"))",
                "    if err != nil { return \"\" }",
                "    lines := strings.Split(strings.ReplaceAll(string(content), \"\\r\\n\", \"\\n\"), \"\\n\")",
                "    for index := len(lines) - 1; index >= 0; index -= 1 {",
                "        line := lines[index]",
                "        if line == \"\" { continue }",
                "        fields := apoIrParseLine(line)",
                "        if len(fields) < 5 { continue }",
                "        if kind != \"\" && fields[0] != kind { continue }",
                "        if fields[1] != name { continue }",
                "        return fields[4]",
                "    }",
                "    return \"\"",
                "}",
                "",
                "func async_exec(args ...string) int {",
                "    kind := \"\"",
                "    name := \"\"",
                "    if len(args) == 1 { name = args[0] } else if len(args) > 1 { kind = args[0]; name = args[1] }",
                "    command := exec.Command(\"bash\", \"output/apo_ir_exec.sh\", kind, name)",
                "    command.Stdout = os.Stdout",
                "    command.Stderr = os.Stderr",
                "    err := command.Run()",
                "    if err == nil { return 0 }",
                "    if exitError, ok := err.(*exec.ExitError); ok { return exitError.ExitCode() }",
                "    return -1",
                "}") + "\n\n";
    }

    private String buildPhpIrPrelude() {
        return String.join("\n",
                "function _apo_ir_escape($value) {",
                "    $text = $value === null ? '' : (string)$value;",
                "    return str_replace([\\\"\\\\\\\", \\\"\\t\\\", \\\"\\n\\\", \\\"\\r\\\"], [\\\"\\\\\\\\\\\\\\\", \\\"\\\\t\\\", \\\"\\\\n\\\", \\\"\\\\r\\\"], $text);",
                "}",
                "",
                "function _apo_ir_parse_line($line) {",
                "    $fields = [];",
                "    $current = '';",
                "    $escaping = false;",
                "    $length = strlen($line);",
                "    for ($index = 0; $index < $length; $index += 1) {",
                "        $ch = $line[$index];",
                "        if ($escaping) {",
                "            if ($ch === 'n') $current .= \"\\n\";",
                "            else if ($ch === 'r') $current .= \"\\r\";",
                "            else if ($ch === 't') $current .= \"\\t\";",
                "            else $current .= $ch;",
                "            $escaping = false;",
                "            continue;",
                "        }",
                "        if ($ch === '\\\\') { $escaping = true; continue; }",
                "        if ($ch === \"\\t\") { $fields[] = $current; $current = ''; continue; }",
                "        $current .= $ch;",
                "    }",
                "    if ($escaping) $current .= '\\\\';",
                "    $fields[] = $current;",
                "    return $fields;",
                "}",
                "",
                "function async_write($kind, $name, $typeName, $payload, $sourceLang = 'php') {",
                "    if (!is_dir('output')) mkdir('output', 0777, true);",
                "    $record = implode(\"\\t\", [_apo_ir_escape($kind), _apo_ir_escape($name), _apo_ir_escape($typeName), _apo_ir_escape($sourceLang), _apo_ir_escape($payload)]) . PHP_EOL;",
                "    file_put_contents('output/apo_ir.txt', $record, FILE_APPEND);",
                "}",
                "",
                "function async_read($kindOrName, $maybeName = null) {",
                "    $kind = $maybeName === null ? '' : (string)$kindOrName;",
                "    $name = $maybeName === null ? (string)$kindOrName : (string)$maybeName;",
                "    if (!file_exists('output/apo_ir.txt')) return '';",
                "    $lines = file('output/apo_ir.txt', FILE_IGNORE_NEW_LINES);",
                "    for ($index = count($lines) - 1; $index >= 0; $index -= 1) {",
                "        $line = $lines[$index];",
                "        if ($line === '') continue;",
                "        $fields = _apo_ir_parse_line($line);",
                "        if (count($fields) < 5) continue;",
                "        if ($kind !== '' && $fields[0] !== $kind) continue;",
                "        if ($fields[1] !== $name) continue;",
                "        return $fields[4];",
                "    }",
                "    return '';",
                "}",
                "",
                "function async_exec($kindOrName, $maybeName = null) {",
                "    $kind = $maybeName === null ? '' : (string)$kindOrName;",
                "    $name = $maybeName === null ? (string)$kindOrName : (string)$maybeName;",
                "    $command = 'bash ' . escapeshellarg('output/apo_ir_exec.sh') . ' ' . escapeshellarg($kind) . ' ' . escapeshellarg($name);",
                "    passthru($command, $exitCode);",
                "    return $exitCode;",
                "}") + "\n\n";
    }

    private String buildRubyIrPrelude() {
        return String.join("\n",
                "require 'fileutils'",
                "",
                "def __apo_ir_escape(value)",
                "  text = value.nil? ? '' : value.to_s",
                "  text.gsub('\\\\', '\\\\\\\\').gsub(\"\\t\", '\\\\t').gsub(\"\\n\", '\\\\n').gsub(\"\\r\", '\\\\r')",
                "end",
                "",
                "def __apo_ir_parse_line(line)",
                "  fields = []",
                "  current = ''",
                "  escaping = false",
                "  line.each_char do |ch|",
                "    if escaping",
                "      current << case ch when 'n' then \"\\n\" when 'r' then \"\\r\" when 't' then \"\\t\" else ch end",
                "      escaping = false",
                "      next",
                "    end",
                "    if ch == '\\\\'",
                "      escaping = true",
                "      next",
                "    end",
                "    if ch == \"\\t\"",
                "      fields << current",
                "      current = ''",
                "      next",
                "    end",
                "    current << ch",
                "  end",
                "  current << '\\\\' if escaping",
                "  fields << current",
                "  fields",
                "end",
                "",
                "def async_write(kind, name, type_name, payload, source_lang = 'rb')",
                "  FileUtils.mkdir_p('output')",
                "  record = [kind, name, type_name, source_lang, payload].map { |value| __apo_ir_escape(value) }.join(\"\\t\") + \"\\n\"",
                "  File.open('output/apo_ir.txt', 'a:utf-8') { |file| file.write(record) }",
                "end",
                "",
                "def async_read(kind_or_name, name = nil)",
                "  kind = name.nil? ? '' : kind_or_name.to_s",
                "  symbol_name = name.nil? ? kind_or_name.to_s : name.to_s",
                "  return '' unless File.exist?('output/apo_ir.txt')",
                "  File.readlines('output/apo_ir.txt', chomp: true).reverse_each do |line|",
                "    next if line.empty?",
                "    fields = __apo_ir_parse_line(line)",
                "    next if fields.length < 5",
                "    next unless kind.empty? || fields[0] == kind",
                "    next unless fields[1] == symbol_name",
                "    return fields[4]",
                "  end",
                "  ''",
                "end",
                "",
                "def async_exec(kind_or_name, name = nil)",
                "  kind = name.nil? ? '' : kind_or_name.to_s",
                "  symbol_name = name.nil? ? kind_or_name.to_s : name.to_s",
                "  system('bash', 'output/apo_ir_exec.sh', kind, symbol_name)",
                "  $?.exitstatus || -1",
                "end") + "\n\n";
    }

    private String buildKotlinIrPrelude() {
        return String.join("\n",
                "import java.nio.charset.StandardCharsets",
                "import java.nio.file.Files",
                "import java.nio.file.Paths",
                "import java.nio.file.StandardOpenOption",
                "",
                "fun apoIrEscape(value: String?): String {",
                "    val text = value ?: \"\"",
                "    return text.replace(\\\"\\\\\\\", \\\"\\\\\\\\\\\\\\\").replace(\\\"\\t\\\", \\\"\\\\t\\\").replace(\\\"\\n\\\", \\\"\\\\n\\\").replace(\\\"\\r\\\", \\\"\\\\r\\\")",
                "}",
                "",
                "fun apoIrParseLine(line: String): List<String> {",
                "    val fields = mutableListOf<String>()",
                "    val current = StringBuilder()",
                "    var escaping = false",
                "    for (ch in line) {",
                "        if (escaping) {",
                "            when (ch) {",
                "                'n' -> current.append('\\n')",
                "                'r' -> current.append('\\r')",
                "                't' -> current.append('\\t')",
                "                else -> current.append(ch)",
                "            }",
                "            escaping = false",
                "            continue",
                "        }",
                "        if (ch == '\\\\') { escaping = true; continue }",
                "        if (ch == '\\t') { fields += current.toString(); current.setLength(0); continue }",
                "        current.append(ch)",
                "    }",
                "    if (escaping) current.append('\\\\')",
                "    fields += current.toString()",
                "    return fields",
                "}",
                "",
                "fun async_write(kind: String, name: String, typeName: String, payload: String, sourceLang: String = \"kt\") {",
                "    val irPath = Paths.get(\"output\", \"apo_ir.txt\")",
                "    Files.createDirectories(irPath.parent)",
                "    val record = listOf(kind, name, typeName, sourceLang, payload).joinToString(\"\\t\") { apoIrEscape(it) } + System.lineSeparator()",
                "    Files.write(irPath, record.toByteArray(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND)",
                "}",
                "",
                "fun async_read(name: String): String = async_read(\"\", name)",
                "",
                "fun async_read(kind: String, name: String): String {",
                "    val irPath = Paths.get(\"output\", \"apo_ir.txt\")",
                "    if (!Files.exists(irPath)) return \"\"",
                "    val lines = Files.readAllLines(irPath, StandardCharsets.UTF_8)",
                "    for (index in lines.indices.reversed()) {",
                "        val line = lines[index]",
                "        if (line.isEmpty()) continue",
                "        val fields = apoIrParseLine(line)",
                "        if (fields.size < 5) continue",
                "        if (kind.isNotEmpty() && fields[0] != kind) continue",
                "        if (fields[1] != name) continue",
                "        return fields[4]",
                "    }",
                "    return \"\"",
                "}",
                "",
                "fun async_exec(name: String): Int = async_exec(\"\", name)",
                "",
                "fun async_exec(kind: String, name: String): Int {",
                "    val command = listOf(\"bash\", \"output/apo_ir_exec.sh\", kind, name)",
                "    val process = ProcessBuilder(command)",
                "        .inheritIO()",
                "        .start()",
                "    return process.waitFor()",
                "}") + "\n\n";
    }

    private void writeIrRuntimeSupport() {
        writeLine("static std::vector<std::string> __apo_syscallQueue;");
        writeLine("static int __apo_irReset = []() { std::filesystem::create_directories(\"output\"); std::ofstream out(\"output/apo_ir.txt\", std::ios::trunc); return 0; }();");
        writeLine("static void __apo_writeFile(const std::string& path, const std::string& content) { std::filesystem::create_directories(\"output\"); std::ofstream out(path, std::ios::trunc); out << content; }");
        writeLine("static int __apo_irExecInit = []() {");
        indentLevel++;
        write(indent() + "__apo_writeFile(\"output/apo_ir_exec.sh\", R\"APO(\n");
        write(buildShellExecScript());
        write(indent() + ")APO\");\n");
        writeLine("return 0;");
        indentLevel--;
        writeLine("}();");
        writeLine("static std::string __apo_escapeIrField(const std::string& value) {");
        indentLevel++;
        writeLine("std::string escaped;");
        writeLine("for (char ch : value) {");
        indentLevel++;
        writeLine("if (ch == '\\\\') escaped += \"\\\\\\\\\";");
        writeLine("else if (ch == '\\t') escaped += \"\\\\t\";");
        writeLine("else if (ch == '\\n') escaped += \"\\\\n\";");
        writeLine("else if (ch == '\\r') escaped += \"\\\\r\";");
        writeLine("else escaped.push_back(ch);");
        indentLevel--;
        writeLine("}");
        writeLine("return escaped;");
        indentLevel--;
        writeLine("}");
        writeLine("static std::vector<std::string> __apo_parseIrLine(const std::string& line) {");
        indentLevel++;
        writeLine("std::vector<std::string> fields;");
        writeLine("std::string current;");
        writeLine("bool escaping = false;");
        writeLine("for (char ch : line) {");
        indentLevel++;
        writeLine("if (escaping) {");
        indentLevel++;
        writeLine("if (ch == 'n') current.push_back('\\n');");
        writeLine("else if (ch == 'r') current.push_back('\\r');");
        writeLine("else if (ch == 't') current.push_back('\\t');");
        writeLine("else current.push_back(ch);");
        writeLine("escaping = false;");
        writeLine("continue;");
        indentLevel--;
        writeLine("}");
        writeLine("if (ch == '\\\\') {");
        indentLevel++;
        writeLine("escaping = true;");
        writeLine("continue;");
        indentLevel--;
        writeLine("}");
        writeLine("if (ch == '\\t') {");
        indentLevel++;
        writeLine("fields.push_back(current);");
        writeLine("current.clear();");
        writeLine("continue;");
        indentLevel--;
        writeLine("}");
        writeLine("current.push_back(ch);");
        indentLevel--;
        writeLine("}");
        writeLine("if (escaping) current.push_back('\\\\');");
        writeLine("fields.push_back(current);");
        writeLine("return fields;");
        indentLevel--;
        writeLine("}");
        writeLine("static void async_write(const std::string& kind, const std::string& name, const std::string& typeName, const std::string& payload, const std::string& sourceLang) {");
        indentLevel++;
        writeLine("std::filesystem::create_directories(\"output\");");
        writeLine("std::ofstream out(\"output/apo_ir.txt\", std::ios::app);");
        writeLine("out << __apo_escapeIrField(kind) << '\\t' << __apo_escapeIrField(name) << '\\t' << __apo_escapeIrField(typeName) << '\\t' << __apo_escapeIrField(sourceLang) << '\\t' << __apo_escapeIrField(payload) << '\\n';");
        indentLevel--;
        writeLine("}");
        writeLine("static void async_write(const std::string& kind, const std::string& name, const std::string& typeName, const std::string& payload) {");
        indentLevel++;
        writeLine("async_write(kind, name, typeName, payload, \"apollo\");");
        indentLevel--;
        writeLine("}");
        writeLine("static std::string async_read(const std::string& kind, const std::string& name) {");
        indentLevel++;
        writeLine("std::ifstream in(\"output/apo_ir.txt\");");
        writeLine("if (!in) return \"\";");
        writeLine("std::vector<std::string> lines;");
        writeLine("std::string line;");
        writeLine("while (std::getline(in, line)) {");
        indentLevel++;
        writeLine("if (!line.empty()) lines.push_back(line);");
        indentLevel--;
        writeLine("}");
        writeLine("for (auto it = lines.rbegin(); it != lines.rend(); ++it) {");
        indentLevel++;
        writeLine("std::vector<std::string> fields = __apo_parseIrLine(*it);");
        writeLine("if (fields.size() < 5) continue;");
        writeLine("if (!kind.empty() && fields[0] != kind) continue;");
        writeLine("if (fields[1] != name) continue;");
        writeLine("return fields[4];");
        indentLevel--;
        writeLine("}");
        writeLine("return \"\";");
        indentLevel--;
        writeLine("}");
        writeLine("static std::string async_read(const std::string& name) {");
        indentLevel++;
        writeLine("return async_read(\"\", name);");
        indentLevel--;
        writeLine("}");
        writeLine("static std::string __apo_clangTargetFlags() {");
        indentLevel++;
        writeLine("const char* target = std::getenv(\"APOLLO_CLANG_TARGET\");");
        writeLine("std::string flags;");
        writeLine("if (target != nullptr && *target != '\\0') flags = \"--target=\" + std::string(target);");
        writeLine("const char* sysroot = std::getenv(\"APOLLO_SYSROOT\");");
        writeLine("if (sysroot != nullptr && *sysroot != '\\0') flags += (flags.empty() ? std::string() : std::string(\" \")) + \"--sysroot=\\\"\" + std::string(sysroot) + \"\\\"\";");
        writeLine("const char* gccToolchain = std::getenv(\"APOLLO_GCC_TOOLCHAIN\");");
        writeLine("if (gccToolchain != nullptr && *gccToolchain != '\\0') flags += (flags.empty() ? std::string() : std::string(\" \")) + \"--gcc-toolchain=\\\"\" + std::string(gccToolchain) + \"\\\"\";");
        writeLine("return flags;");
        indentLevel--;
        writeLine("}");
        writeLine("static std::string __apo_shellQuote(const std::string& value) {");
        indentLevel++;
        writeLine("std::string quoted = \"'\";");
        writeLine("for (char ch : value) {");
        indentLevel++;
        writeLine("if (ch == '\\'') quoted += \"'\\\\'\\''\";");
        writeLine("else quoted.push_back(ch);");
        indentLevel--;
        writeLine("}");
        writeLine("quoted += \"'\";");
        writeLine("return quoted;");
        indentLevel--;
        writeLine("}");
        writeLine("static std::string __apo_toolCommand(const char* envDirVar, const std::string& toolName) {");
        indentLevel++;
        writeLine("const char* envDir = std::getenv(envDirVar);");
        writeLine("if (envDir != nullptr && *envDir != '\\0') {");
        indentLevel++;
        writeLine("return __apo_shellQuote(std::string(envDir) + \"/\" + toolName);");
        indentLevel--;
        writeLine("}");
        writeLine("return toolName;");
        indentLevel--;
        writeLine("}");
        writeLine("static int __apo_runPayload(const std::string& payload) {");
    }

    private String wrapNativeSource(String lang, String code) {
        String normalized = code.strip();
        normalized = rewriteAsyncInteropCalls(lang, normalized);
        if ("cpp".equals(lang)) {
            if (!normalized.contains("#include")) {
                normalized = "#include <iostream>\n\n" + normalized;
            }
            return buildCppIrPrelude() + normalized + "\n";
        }
        if ("py".equals(lang)) {
            normalized = normalized.replaceAll("(?m)^\\s*sys\\.println\\((.*)\\);?\\s*$", "print($1)");
            return buildPythonIrPrelude() + normalized + "\n";
        }
        if ("java".equals(lang)) {
            return buildJavaIrPrelude() + normalized + "\n";
        }
        if ("rs".equals(lang)) {
            return buildRustIrPrelude() + normalized + "\n";
        }
        if ("cs".equals(lang)) {
            return buildCSharpIrPrelude() + normalized + "\n";
        }
        if ("js".equals(lang)) {
            return buildJavaScriptIrPrelude("js", false) + normalized + "\n";
        }
        if ("ts".equals(lang)) {
            return buildJavaScriptIrPrelude("ts", true) + normalized + "\n";
        }
        if ("go".equals(lang)) {
            return buildGoIrPrelude() + normalized + "\n";
        }
        if ("php".equals(lang)) {
            String phpBody = normalized;
            if (phpBody.startsWith("<?php")) {
                phpBody = phpBody.substring(5).stripLeading();
            }
            return "<?php\n" + buildPhpIrPrelude() + phpBody + "\n";
        }
        if ("rb".equals(lang)) {
            return buildRubyIrPrelude() + normalized + "\n";
        }
        if ("kt".equals(lang)) {
            return buildKotlinIrPrelude() + normalized + "\n";
        }
        return normalized + "\n";
    }

    private String escapeCppString(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\"", "\\\"");
    }

    private String buildQueuedPayload(compilerv1Parser.NativemodeContext ctx) {
        String langTok = null;
        if (ctx.NATIVE() != null) langTok = ctx.NATIVE().getText();
        else if (ctx.ID() != null) langTok = ctx.ID().getText();
        String lang = normalizeNativeLang(langTok);
        String code = wrapNativeSource(lang, ctx.INCLUSIVE().getText());
        return lang + "\n" + code;
    }

    private String renderEnqueuePayload(compilerv1Parser.NativemodeContext ctx) {
        return "__apo_queuePayload(\"" + escapeCppString(buildQueuedPayload(ctx)) + "\");";
    }

    private String renderImmediatePayload(compilerv1Parser.NativemodeContext ctx) {
        return "__apo_executePayload(\"" + escapeCppString(buildQueuedPayload(ctx)) + "\");";
    }

    private String renderSyscallExecution(boolean drainAll) {
        if (drainAll) {
            return "__apo_executeQueuedPayload(true);";
        }
        return "__apo_executeQueuedPayload(false);";
    }

    private void writeHashSupport() {
        writeLine("#ifndef __APO_HASH_SUPPORT");
        writeLine("#define __APO_HASH_SUPPORT");
        writeLine("template <typename T>");
        writeLine("inline void __apo_hash_combine(std::size_t& seed, const T& value) {");
        indentLevel++;
        writeLine("seed ^= value + 0x9e3779b97f4a7c15ULL + (seed << 6) + (seed >> 2);");
        indentLevel--;
        writeLine("}");
        writeLine("template <typename T>");
        writeLine("struct __apo_hash {");
        indentLevel++;
        writeLine("std::size_t operator()(const T& value) const { return std::hash<T>{}(value); }");
        indentLevel--;
        writeLine("};");
        writeLine("template <typename T>");
        writeLine("struct __apo_hash<std::vector<T>> {");
        indentLevel++;
        writeLine("std::size_t operator()(const std::vector<T>& values) const {");
        indentLevel++;
        writeLine("std::size_t seed = 0;");
        writeLine("for (const auto& value : values) {");
        indentLevel++;
        writeLine("__apo_hash_combine(seed, __apo_hash<T>{}(value));");
        indentLevel--;
        writeLine("}");
        writeLine("return seed;");
        indentLevel--;
        writeLine("}");
        indentLevel--;
        writeLine("};");
        writeLine("template <typename K, typename V, typename H, typename E, typename A>");
        writeLine("struct __apo_hash<std::unordered_map<K, V, H, E, A>> {");
        indentLevel++;
        writeLine("std::size_t operator()(const std::unordered_map<K, V, H, E, A>& values) const {");
        indentLevel++;
        writeLine("std::size_t seed = 0;");
        writeLine("for (const auto& entry : values) {");
        indentLevel++;
        writeLine("std::size_t entrySeed = __apo_hash<K>{}(entry.first);");
        writeLine("__apo_hash_combine(entrySeed, __apo_hash<V>{}(entry.second));");
        writeLine("seed ^= entrySeed + 0x9e3779b97f4a7c15ULL;");
        indentLevel--;
        writeLine("}");
        writeLine("return seed;");
        indentLevel--;
        writeLine("}");
        indentLevel--;
        writeLine("};");
        writeLine("#endif");
        write("\n");
    }

    private String renderExpression(compilerv1Parser.ExpressionContext ctx) {
        return renderExpression(ctx, null);
    }

    private String renderExpression(compilerv1Parser.ExpressionContext ctx, ApolloType expectedType) {
        if (ctx == null) {
            return "";
        }
        return renderOrExpr(ctx.orExpr(), expectedType);
    }

    private String renderOrExpr(compilerv1Parser.OrExprContext ctx, ApolloType expectedType) {
        StringBuilder builder = new StringBuilder();
        List<compilerv1Parser.AndExprContext> entries = ctx.andExpr();
        for (int index = 0; index < entries.size(); index++) {
            if (index > 0) {
                builder.append(" || ");
            }
            builder.append(renderAndExpr(entries.get(index), expectedType));
        }
        return builder.toString();
    }

    private String renderAndExpr(compilerv1Parser.AndExprContext ctx, ApolloType expectedType) {
        StringBuilder builder = new StringBuilder();
        List<compilerv1Parser.EqualityExprContext> entries = ctx.equalityExpr();
        for (int index = 0; index < entries.size(); index++) {
            if (index > 0) {
                builder.append(" && ");
            }
            builder.append(renderEqualityExpr(entries.get(index), expectedType));
        }
        return builder.toString();
    }

    private String renderEqualityExpr(compilerv1Parser.EqualityExprContext ctx, ApolloType expectedType) {
        StringBuilder builder = new StringBuilder();
        List<compilerv1Parser.RelationalExprContext> entries = ctx.relationalExpr();
        for (int index = 0; index < entries.size(); index++) {
            if (index > 0) {
                builder.append(' ').append(ctx.getChild(index * 2 - 1).getText()).append(' ');
            }
            builder.append(renderRelationalExpr(entries.get(index), expectedType));
        }
        return builder.toString();
    }

    private String renderRelationalExpr(compilerv1Parser.RelationalExprContext ctx, ApolloType expectedType) {
        StringBuilder builder = new StringBuilder();
        List<compilerv1Parser.AddExprContext> entries = ctx.addExpr();
        for (int index = 0; index < entries.size(); index++) {
            if (index > 0) {
                builder.append(' ').append(ctx.getChild(index * 2 - 1).getText()).append(' ');
            }
            builder.append(renderAddExpr(entries.get(index), expectedType));
        }
        return builder.toString();
    }

    private String renderAddExpr(compilerv1Parser.AddExprContext ctx, ApolloType expectedType) {
        StringBuilder builder = new StringBuilder();
        List<compilerv1Parser.MultExprContext> entries = ctx.multExpr();
        for (int index = 0; index < entries.size(); index++) {
            if (index > 0) {
                builder.append(' ').append(ctx.getChild(index * 2 - 1).getText()).append(' ');
            }
            builder.append(renderMultExpr(entries.get(index), expectedType));
        }
        return builder.toString();
    }

    private String renderMultExpr(compilerv1Parser.MultExprContext ctx, ApolloType expectedType) {
        StringBuilder builder = new StringBuilder();
        List<compilerv1Parser.PrimaryContext> entries = ctx.primary();
        for (int index = 0; index < entries.size(); index++) {
            if (index > 0) {
                builder.append(' ').append(ctx.getChild(index * 2 - 1).getText()).append(' ');
            }
            builder.append(renderPrimary(entries.get(index), expectedType));
        }
        return builder.toString();
    }

    private String renderPrimary(compilerv1Parser.PrimaryContext ctx, ApolloType expectedType) {
        if (ctx.INT() != null) {
            return ctx.INT().getText();
        }
        if (ctx.STRING() != null) {
            return ctx.STRING().getText();
        }
        if (ctx.functionCall() != null) {
            return renderFunctionCall(ctx.functionCall());
        }
        if (ctx.indexedAccess() != null) {
            return renderIndexedAccess(ctx.indexedAccess());
        }
        if (ctx.compositeLiteral() != null) {
            return renderCompositeLiteral(ctx.compositeLiteral(), expectedType);
        }
        if (ctx.instanceValue() != null) {
            return renderInstanceValue(ctx.instanceValue());
        }
        if (ctx.ID() != null) {
            return "null".equals(ctx.ID().getText()) ? "nullptr" : ctx.ID().getText();
        }
        if (ctx.expression() != null) {
            return "(" + renderExpression(ctx.expression()) + ")";
        }
        return ctx.getText();
    }

    private String renderCompositeLiteral(compilerv1Parser.CompositeLiteralContext ctx, ApolloType expectedType) {
        List<compilerv1Parser.ExpressionContext> values = ctx.expression();
        if (expectedType != null && "vector".equals(expectedType.name) && expectedType.arguments.size() == 1) {
            StringBuilder builder = new StringBuilder(renderType(expectedType)).append("{");
            for (int index = 0; index < values.size(); index++) {
                if (index > 0) {
                    builder.append(", ");
                }
                builder.append(renderExpression(values.get(index), expectedType.arguments.get(0)));
            }
            builder.append("}");
            return builder.toString();
        }
        if (expectedType != null && "hsh".equals(expectedType.name) && expectedType.arguments.size() == 2) {
            if (values.size() % 2 != 0) {
                throw new IllegalArgumentException("hsh composite literals require key/value pairs: " + ctx.getText());
            }
            StringBuilder builder = new StringBuilder(renderType(expectedType)).append("{");
            for (int index = 0; index < values.size(); index += 2) {
                if (index > 0) {
                    builder.append(", ");
                }
                builder.append("{")
                       .append(renderExpression(values.get(index), expectedType.arguments.get(0)))
                       .append(", ")
                       .append(renderExpression(values.get(index + 1), expectedType.arguments.get(1)))
                       .append("}");
            }
            builder.append("}");
            return builder.toString();
        }
        if (expectedType != null) {
            StringBuilder builder = new StringBuilder(renderType(expectedType)).append("{");
            for (int index = 0; index < values.size(); index++) {
                if (index > 0) {
                    builder.append(", ");
                }
                builder.append(renderExpression(values.get(index)));
            }
            builder.append("}");
            return builder.toString();
        }
        StringBuilder builder = new StringBuilder("{");
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(renderExpression(values.get(index)));
        }
        builder.append("}");
        return builder.toString();
    }

    private String renderIndexedAccess(compilerv1Parser.IndexedAccessContext ctx) {
        StringBuilder builder = new StringBuilder(ctx.ID().getText());
        ApolloType currentType = resolveVariableType(ctx.ID().getText());
        for (compilerv1Parser.AccessKeyContext key : ctx.accessKey()) {
            if (key.APND() != null) {
                throw new IllegalArgumentException("apnd cannot be used in an expression: " + ctx.getText());
            }
            ApolloType keyType = keyTypeForIndexedAccess(currentType);
            builder.append("[").append(renderExpression(key.expression(), keyType)).append("]");
            currentType = valueTypeForIndexedAccess(currentType);
        }
        return builder.toString();
    }

    private AssignTargetInfo renderAssignTarget(compilerv1Parser.AssignTargetContext ctx) {
        StringBuilder builder = new StringBuilder(ctx.ID().getText());
        ApolloType currentType = resolveVariableType(ctx.ID().getText());
        List<compilerv1Parser.AccessKeyContext> keys = ctx.accessKey();
        if (keys == null || keys.isEmpty()) {
            return new AssignTargetInfo(builder.toString(), currentType, null);
        }

        for (int index = 0; index < keys.size(); index++) {
            compilerv1Parser.AccessKeyContext key = keys.get(index);
            if (key.APND() != null) {
                if (index != keys.size() - 1) {
                    throw new IllegalArgumentException("apnd can only appear at the end of an assignment target: " + ctx.getText());
                }
                builder.append(".__apollo_append__");
                return new AssignTargetInfo(builder.toString(), valueTypeForIndexedAccess(currentType), currentType);
            }
            ApolloType keyType = keyTypeForIndexedAccess(currentType);
            builder.append("[").append(renderExpression(key.expression(), keyType)).append("]");
            currentType = valueTypeForIndexedAccess(currentType);
        }
        return new AssignTargetInfo(builder.toString(), currentType, null);
    }

    private String renderFunctionCall(compilerv1Parser.FunctionCallContext ctx) {
        StringBuilder builder = new StringBuilder();
        builder.append(ctx.ID().getText()).append("(");
        if (ctx.args() != null) {
            List<compilerv1Parser.ExpressionContext> expressions = ctx.args().expression();
            for (int i = 0; i < expressions.size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(renderExpression(expressions.get(i)));
            }
        }
        builder.append(")");
        return builder.toString();
    }

    private compilerv1Parser.PrimaryContext resolveSinglePrimary(compilerv1Parser.ExpressionContext ctx) {
        if (ctx == null || ctx.orExpr() == null || ctx.orExpr().andExpr().size() != 1) {
            return null;
        }
        compilerv1Parser.AndExprContext andCtx = ctx.orExpr().andExpr(0);
        if (andCtx.equalityExpr().size() != 1) {
            return null;
        }
        compilerv1Parser.EqualityExprContext equalityCtx = andCtx.equalityExpr(0);
        if (equalityCtx.relationalExpr().size() != 1) {
            return null;
        }
        compilerv1Parser.RelationalExprContext relationalCtx = equalityCtx.relationalExpr(0);
        if (relationalCtx.addExpr().size() != 1) {
            return null;
        }
        compilerv1Parser.AddExprContext addCtx = relationalCtx.addExpr(0);
        if (addCtx.multExpr().size() != 1) {
            return null;
        }
        compilerv1Parser.MultExprContext multCtx = addCtx.multExpr(0);
        if (multCtx.primary().size() != 1) {
            return null;
        }
        compilerv1Parser.PrimaryContext primaryCtx = multCtx.primary(0);
        if (primaryCtx.expression() != null) {
            return resolveSinglePrimary(primaryCtx.expression());
        }
        return primaryCtx;
    }

    private String inferExpressionType(compilerv1Parser.ExpressionContext ctx) {
        compilerv1Parser.PrimaryContext primaryCtx = resolveSinglePrimary(ctx);
        if (primaryCtx == null) {
            return null;
        }
        if (primaryCtx.instanceValue() != null) {
            return mapDeclaredType(primaryCtx.instanceValue().ID().getText());
        }
        if (primaryCtx.ID() != null) {
            String variableName = primaryCtx.ID().getText();
            String instanceType = instanceTypes.get(variableName);
            if (instanceType != null) {
                return instanceType;
            }
            ApolloType resolvedType = resolveVariableType(variableName);
            if (resolvedType != null) {
                return renderType(resolvedType);
            }
        }
        return null;
    }

    private String renderMemberAccess(compilerv1Parser.MemberaccessContext ctx) {
        String objectName = ctx.ID(0).getText();
        String instanceType = instanceTypes.get(objectName);
        String instanceMode = instanceModes.get(objectName);
        String target = objectName;
        if (instanceType != null && "crt".equals(instanceMode)) {
            target = "std::any_cast<" + instanceType + "&>(" + objectName + ")";
        }
        if (ctx.functionCall() != null) {
            return target + "." + renderFunctionCall(ctx.functionCall());
        }
        return target + "." + ctx.ID(1).getText();
    }

    private String renderInstanceValue(compilerv1Parser.InstanceValueContext ctx) {
        StringBuilder builder = new StringBuilder();
        builder.append(mapDeclaredType(ctx.ID().getText())).append("{");
        if (ctx.args() != null) {
            List<compilerv1Parser.ExpressionContext> expressions = ctx.args().expression();
            for (int i = 0; i < expressions.size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(renderExpression(expressions.get(i)));
            }
        }
        builder.append("}");
        return builder.toString();
    }

    private String renderInstancePush(compilerv1Parser.InstancepushContext ctx) {
        String targetName = ctx.ID().getText();
        String instanceMode = instanceModes.get(targetName);
        if (instanceMode == null) {
            throw new IllegalArgumentException("Instance must be declared before push: " + ctx.getText());
        }
        String value = renderInstanceValue(ctx.instanceValue());
        String instanceType = mapDeclaredType(ctx.instanceValue().ID().getText());
        if ("crt".equals(instanceMode)) {
            instanceTypes.put(targetName, instanceType);
        } else if ("staticx".equals(instanceMode)) {
            String declaredType = instanceTypes.get(targetName);
            if (declaredType == null) {
                throw new IllegalStateException("staticx instance type missing for " + targetName);
            }
            if (!declaredType.equals(instanceType)) {
                throw new IllegalArgumentException("Cannot push " + instanceType + " into staticx instance " + targetName + " of type " + declaredType);
            }
        } else {
            throw new IllegalArgumentException("Unsupported instance mode for push: " + instanceMode);
        }
        return targetName + " = " + value + ";";
    }

    private String renderType(compilerv1Parser.TypeRefContext ctx) {
        return renderType(typeFromContext(ctx));
    }

    private String renderTypeForFunction(compilerv1Parser.TypeRefContext ctx) {
        return renderTypeForFunction(typeFromContext(ctx));
    }

    private String renderReturnType(compilerv1Parser.ReturnTypeContext ctx) {
        if (ctx == null || "void".equals(ctx.getText())) {
            return "void";
        }
        return renderTypeForFunction(ctx.typeRef());
    }

    private String rawReturnType(compilerv1Parser.ReturnTypeContext ctx) {
        if (ctx == null) {
            return "auto";
        }
        return ctx.getText();
    }

    private boolean isFunctionType(ApolloType type) {
        return type != null && "fn".equals(type.name);
    }

    private ApolloType lambdaInferredReturnType(ApolloType declaredType) {
        if (isFunctionType(declaredType) && !declaredType.arguments.isEmpty()) {
            return declaredType.arguments.get(0);
        }
        return null;
    }

    private void bindParameters(compilerv1Parser.ParamsContext params) {
        if (params == null) {
            return;
        }
        for (compilerv1Parser.ParamContext param : params.param()) {
            bindVariableType(param.ID().getText(), typeFromContext(param.typeRef()));
        }
    }

    private String renderParameterSignature(compilerv1Parser.ParamsContext params) {
        if (params == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        List<compilerv1Parser.ParamContext> entries = params.param();
        for (int i = 0; i < entries.size(); i++) {
            compilerv1Parser.ParamContext param = entries.get(i);
            String type = param.typeRef() != null ? renderTypeForFunction(param.typeRef()) : "auto";
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(type).append(" ").append(param.ID().getText());
        }
        return builder.toString();
    }

    private void writeSignature(compilerv1Parser.ReturnTypeContext returnType, String name, compilerv1Parser.ParamsContext params,
            boolean isStaticMethod, boolean isVirtualMethod, boolean isOverrideMethod) {
        StringBuilder prefix = new StringBuilder();
        if (isStaticMethod) {
            prefix.append("static ");
        }
        if (isVirtualMethod) {
            prefix.append("virtual ");
        }
        write(indent() + prefix + renderReturnType(returnType) + " " + name + "(" + renderParameterSignature(params) + ")");
        if (isOverrideMethod) {
            write(" override");
        }
        write(" ");
    }

    private void writeParameterList(compilerv1Parser.ParamsContext params) {
        write("(" + renderParameterSignature(params) + ") ");
    }

    private void writeBlock(compilerv1Parser.BlockContext ctx) {
        writeBlock(ctx, false);
    }

    private void writeBlock(compilerv1Parser.BlockContext ctx, boolean injectGcInit) {
        write("{\n");
        indentLevel++;
        pushTypeScope();
        if (injectGcInit) {
            writeLine("GC_INIT();");
        }
        for (org.antlr.v4.runtime.tree.ParseTree child : ctx.children) {
            if (child instanceof compilerv1Parser.StatementContext) {
                visitStatement((compilerv1Parser.StatementContext) child);
            } else if (child instanceof compilerv1Parser.ReturnStmtContext) {
                visitReturnStmt((compilerv1Parser.ReturnStmtContext) child);
            }
        }
        popTypeScope();
        indentLevel--;
        writeLine("}");
    }

    private void emitAccessLabel(String nextAccess, String[] currentAccess) {
        if (nextAccess != null && !nextAccess.equals(currentAccess[0])) {
            indentLevel--;
            writeLine(nextAccess + ":");
            indentLevel++;
            currentAccess[0] = nextAccess;
        }
    }

    private int methodStartIndex(compilerv1Parser.MethodContext ctx) {
        int methodStartIndex = ctx.ANNOT_OVERRIDE() != null ? 1 : 0;
        if (ctx.CLSTYPE() != null) {
            methodStartIndex++;
        }
        if (ctx.STATIC() != null) {
            methodStartIndex++;
        }
        if (ctx.VIRTUAL() != null) {
            methodStartIndex++;
        }
        return methodStartIndex;
    }

    private void validateMethodModifiers(compilerv1Parser.MethodContext ctx) {
        if (ctx.STATIC() != null && ctx.VIRTUAL() != null) {
            throw new IllegalArgumentException("Methods cannot be both static and virtual: " + ctx.getText());
        }
        if (ctx.STATIC() != null && ctx.ANNOT_OVERRIDE() != null) {
            throw new IllegalArgumentException("Methods cannot be both static and override: " + ctx.getText());
        }
        if (ctx.ANNOT_OVERRIDE() != null && ctx.VIRTUAL() != null) {
            throw new IllegalArgumentException("Override methods should use @Override instead of virtual: " + ctx.getText());
        }
    }

    private String renderInheritanceClause(compilerv1Parser.InheritanceClauseContext ctx) {
        if (ctx == null || ctx.inheritedType() == null || ctx.inheritedType().isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(" : ");
        List<compilerv1Parser.InheritedTypeContext> baseTypes = ctx.inheritedType();
        for (int index = 0; index < baseTypes.size(); index++) {
            if (index > 0) {
                builder.append(", ");
            }
            String access = baseTypes.get(index).CLSTYPE() != null ? baseTypes.get(index).CLSTYPE().getText() : "public";
            builder.append(access).append(" ").append(renderType(baseTypes.get(index).typeRef()));
        }
        return builder.toString();
    }

    private boolean isLifecycleMethod(compilerv1Parser.MethodContext ctx) {
        String methodKind = ctx.getChild(methodStartIndex(ctx)).getText();
        return "__construct".equals(methodKind) || "__destruct".equals(methodKind);
    }
    @Override
    public Void visitVirtualMethod(compilerv1Parser.VirtualMethodContext ctx) {
        writeLine("virtual " + renderReturnType(ctx.returnType()) + " " + ctx.ID().getText() + "(" + renderParameterSignature(ctx.params()) + ") = 0;");
        return null;
    }

    @Override
    public Void visitInterface(compilerv1Parser.InterfaceContext ctx) {
        String typeName = mapDeclaredType(ctx.ID().getText());
        writeLine("class " + typeName + renderInheritanceClause(ctx.inheritanceClause()) + " {");
        indentLevel++;
        writeLine("public:");
        indentLevel++;
        writeLine("virtual ~" + typeName + "() = default;");
        for (compilerv1Parser.VirtualMethodContext method : ctx.virtualMethod()) {
            visitVirtualMethod(method);
        }
        indentLevel--;
        indentLevel--;
        writeLine("};");
        write("\n");
        return null;
    }
    @Override
    public Void visitLambda(compilerv1Parser.LambdaContext ctx) {
        ApolloType declaredType = typeFromContext(ctx.typeRef());
        if (declaredType != null && !"auto".equals(declaredType.name) && !isFunctionType(declaredType)) {
            throw new IllegalArgumentException("Lambda declarations must use auto or fn<...>: " + ctx.getText());
        }

        compilerv1Parser.ParamsContext paramsCtx;
        compilerv1Parser.BlockContext blockCtx;
        compilerv1Parser.ReturnTypeContext returnTypeCtx = null;

        if (ctx.lambdaDefinition().function() != null) {
            compilerv1Parser.FunctionContext functionCtx = ctx.lambdaDefinition().function();
            paramsCtx = functionCtx.params();
            blockCtx = functionCtx.block();
            returnTypeCtx = functionCtx.returnType();
        } else {
            compilerv1Parser.LambdaLiteralContext literalCtx = ctx.lambdaDefinition().lambdaLiteral();
            paramsCtx = literalCtx.params();
            blockCtx = literalCtx.block();
            returnTypeCtx = literalCtx.returnType();
        }

        ApolloType effectiveReturnType = returnTypeCtx != null
                ? typeFromReturnContext(returnTypeCtx)
                : lambdaInferredReturnType(declaredType);

        write(indent() + renderType(ctx.typeRef()) + " " + ctx.ID().getText() + " = [&]");
        writeParameterList(paramsCtx);
        if (effectiveReturnType != null && !"auto".equals(effectiveReturnType.name)) {
            write("-> " + renderType(effectiveReturnType, true) + " ");
        }
        returnTypes.push(effectiveReturnType != null ? effectiveReturnType.name : "auto");
        pushTypeScope();
        bindParameters(paramsCtx);
        writeBlock(blockCtx);
        popTypeScope();
        returnTypes.pop();
        writeLine(";");
        bindVariableType(ctx.ID().getText(), declaredType);
        return null;
    }

    @Override
    public Void visitStdin(compilerv1Parser.StdinContext ctx) {
        write("std::cin>>"+ctx.ID().getText()+";\n");
        return null;
    }
    @Override
    public Void visitMacro(compilerv1Parser.MacroContext ctx) {
        write(indent() + "static inline void " + ctx.ID().getText());
        writeParameterList(ctx.params());
        returnTypes.push("void");
        pushTypeScope();
        bindParameters(ctx.params());
        writeBlock(ctx.block());
        popTypeScope();
        returnTypes.pop();
        write("\n");
        return null;
    }
    @Override
    public Void visitPointer(compilerv1Parser.PointerContext ctx) {
        String type = renderType(ctx.typeRef());
        String name = ctx.ID(0).getText();
        String inheritor = ctx.ID(1).getText();
        if ("nullptr".equals(inheritor)) {
            writeLine(type + "* " + name + " = nullptr;");
        } else {
            writeLine(type + "* " + name + " = &" + inheritor + ";");
        }
        return null;
    }
    @Override 
    public Void visitNativemode(compilerv1Parser.NativemodeContext ctx) {
        if (ctx.OVERRIDE() != null) {
            writeLine(renderImmediatePayload(ctx));
        } else {
            writeLine(renderEnqueuePayload(ctx));
        }
        return null;
    }

    @Override
    public Void visitSyscallStmt(compilerv1Parser.SyscallStmtContext ctx) {
        writeLine(renderSyscallExecution(ctx.ALL() != null));
        return null;
    }
    @Override
    public Void visitMemberaccess(compilerv1Parser.MemberaccessContext ctx) {
        write(renderMemberAccess(ctx));
        return null;
    }

    @Override
    public Void visitInstance(compilerv1Parser.InstanceContext ctx) {
        String instanceMode = ctx.INSTANCE_MODE().getText();
        String name = ctx.ID().getText();
        instanceModes.put(name, instanceMode);
        if ("crt".equals(instanceMode)) {
            includes.add("any");
            if (ctx.instanceValue() != null) {
                String instanceType = mapDeclaredType(ctx.instanceValue().ID().getText());
                instanceTypes.put(name, instanceType);
                writeLine("std::any " + name + " = " + renderInstanceValue(ctx.instanceValue()) + ";");
            } else {
                instanceTypes.remove(name);
                writeLine("std::any " + name + ";");
            }
            return null;
        }
        if (!"staticx".equals(instanceMode)) {
            throw new IllegalArgumentException("Unsupported instance declaration: " + ctx.getText());
        }
        if (ctx.instanceValue() == null) {
            throw new IllegalArgumentException("staticx instances require an initializer: " + ctx.getText());
        }
        String rawType = ctx.instanceValue().ID().getText();
        String mappedType = mapDeclaredType(rawType);
        instanceTypes.put(name, mappedType);
        bindVariableType(name, new ApolloType(rawType, List.of()));
        writeLine(mappedType + " " + name + " = " + renderInstanceValue(ctx.instanceValue()) + ";");
        return null;
    }
    @Override 
    public Void visitPrint(compilerv1Parser.PrintContext ctx) {
        String renderedExpression = renderExpression(ctx.expression());
        String printKind = ctx.getChild(2).getText();
        if ("println".equals(printKind)) {
            write("std::cout<<" + renderedExpression + "<<std::endl;\n");
        } else {
            write("std::cout<<" + renderedExpression + ";\n");
        }
        return null;
    }
    @Override
    public Void visitInstancepush(compilerv1Parser.InstancepushContext ctx) {
        writeLine(renderInstancePush(ctx));
        return null;
    }
    @Override
    public Void visitImportStmt(compilerv1Parser.ImportStmtContext ctx) {
        if (ctx.STRING() != null) {
            includes.add(ctx.STRING().getText().replaceAll("\"", ""));
        } else if (ctx.headerPath() != null) {
            includes.add(ctx.headerPath().getText());
        } else if (ctx.importPath() != null) {
            String pathText = ctx.importPath().getText();
            if (pathText.contains("*")) {
                wildcardImports.add(pathText);
            } else {
                includes.add(pathText);
            }
        }
        return null;
    }
    @Override
    public Void visitInclude(compilerv1Parser.IncludeContext ctx) {
        String sourcePath = toPackageSourcePath(ctx.importPath().getText());
        if (!dependencies.contains(sourcePath)) {
            dependencies.add(sourcePath);
        }
        return null;
    }
    @Override
    public Void visitProgram(compilerv1Parser.ProgramContext ctx) {
        List<String> topLevelStatements = new ArrayList<>();

        collectDeclaredTypes(ctx);
        collectIncludes(ctx);
        includes.add("cstdlib");
        includes.add("cstdint");
        includes.add("filesystem");
        includes.add("fstream");
        includes.add("functional");
        includes.add("iostream");
        includes.add("string");
        includes.add("unordered_map");
        includes.add("vector");

        if (includes.contains("cstdlib")) writeLine("#include <cstdlib>");
        if (includes.contains("cstdint")) writeLine("#include <cstdint>");
        if (includes.contains("filesystem")) writeLine("#include <filesystem>");
        if (includes.contains("fstream")) writeLine("#include <fstream>");
        if (includes.contains("functional")) writeLine("#include <functional>");
        if (includes.contains("iostream")) writeLine("#include <iostream>");
        if (includes.contains("string")) writeLine("#include <string>");
        if (includes.contains("unordered_map")) writeLine("#include <unordered_map>");
        if (includes.contains("vector")) writeLine("#include <vector>");
        if (includes.contains("any")) writeLine("#include <any>");
        for (String inc : includes) {
            if ("cstdlib".equals(inc) || "cstdint".equals(inc) || "filesystem".equals(inc) || "fstream".equals(inc) || "functional".equals(inc) || "iostream".equals(inc) || "string".equals(inc) || "unordered_map".equals(inc) || "vector".equals(inc) || "any".equals(inc)) continue;
            writeLine("#include <" + inc + ">");
        }
        writeHashSupport();
        for (String dependency : dependencies) {
            writeLine("#include \"" + toPackageHeaderPath(dependency) + "\"");
        }
        for (String w : wildcardImports) {
            writeLine("// import wildcard: " + w);
        }
        if (headerMode) {
            writeLine("#pragma once");
            write("\n");
            writeLine("static void __apo_queuePayload(const std::string& payload);");
            writeLine("static int __apo_executePayload(const std::string& payload);");
            writeLine("static int __apo_executeQueuedPayload(bool drainAll);");
            write("\n");
        } else {
            write("using namespace std;\n\n");
            writeIrRuntimeSupport();
            indentLevel++;
            writeLine("size_t split = payload.find('\\n');");
            writeLine("if (split == std::string::npos) { std::cerr << \"invalid syscall payload\" << std::endl; return -1; }");
            writeLine("std::string lang = payload.substr(0, split);");
            writeLine("std::string code = payload.substr(split + 1);");
            writeLine("std::string clangTargetFlags = __apo_clangTargetFlags();");
            writeLine("std::string clangxxCommand = __apo_toolCommand(\"APOLLO_LLVM_BIN\", \"clang++\");");
            writeLine("std::string clangCommand = __apo_toolCommand(\"APOLLO_LLVM_BIN\", \"clang\");");
            writeLine("std::string javacCommand = __apo_toolCommand(\"APOLLO_JAVA_BIN\", \"javac\");");
            writeLine("std::string javaCommand = __apo_toolCommand(\"APOLLO_JAVA_BIN\", \"java\");");
            writeLine("if (lang == \"cpp\") { __apo_writeFile(\"output/apo_async_cpp.cpp\", code); return system((clangxxCommand + (clangTargetFlags.empty() ? std::string() : std::string(\" \") + clangTargetFlags) + \" output/apo_async_cpp.cpp -o output/apo_async_cpp && ./output/apo_async_cpp\").c_str()); }");
            writeLine("if (lang == \"c\") { __apo_writeFile(\"output/apo_async_c.c\", code); return system((clangCommand + (clangTargetFlags.empty() ? std::string() : std::string(\" \") + clangTargetFlags) + \" output/apo_async_c.c -o output/apo_async_c && ./output/apo_async_c\").c_str()); }");
            writeLine("if (lang == \"rs\") { __apo_writeFile(\"output/apo_async_rs.rs\", code); return system(\"rustc output/apo_async_rs.rs -o output/apo_async_rs && ./output/apo_async_rs\"); }");
            writeLine("if (lang == \"java\") { __apo_writeFile(\"output/ApoAsyncTask.java\", code); return system((javacCommand + \" output/ApoAsyncTask.java -d output && \" + javaCommand + \" -cp output ApoAsyncTask\").c_str()); }");
            writeLine("if (lang == \"cs\") { __apo_writeFile(\"output/ApoAsyncTask.cs\", code); return system(\"if command -v csc >/dev/null 2>&1; then csc /nologo /out:output/ApoAsyncTask.exe output/ApoAsyncTask.cs && { if command -v mono >/dev/null 2>&1; then mono output/ApoAsyncTask.exe; elif command -v dotnet >/dev/null 2>&1; then dotnet output/ApoAsyncTask.exe; else ./output/ApoAsyncTask.exe; fi; }; elif command -v mcs >/dev/null 2>&1; then mcs -out:output/ApoAsyncTask.exe output/ApoAsyncTask.cs && mono output/ApoAsyncTask.exe; else echo 'csc or mcs is required for cs async payloads' >&2; exit 1; fi\"); }");
            writeLine("if (lang == \"py\") { __apo_writeFile(\"output/apo_async.py\", code); return system(\"python3 output/apo_async.py || python output/apo_async.py\"); }");
            writeLine("if (lang == \"js\") { __apo_writeFile(\"output/apo_async.js\", code); return system(\"node output/apo_async.js\"); }");
            writeLine("if (lang == \"ts\") { __apo_writeFile(\"output/apo_async.ts\", code); return system(\"tsc output/apo_async.ts --outDir output && node output/apo_async.js\"); }");
            writeLine("if (lang == \"go\") { __apo_writeFile(\"output/apo_async_go.go\", code); return system(\"go build -o output/apo_async_go output/apo_async_go.go && ./output/apo_async_go\"); }");
            writeLine("if (lang == \"php\") { __apo_writeFile(\"output/apo_async.php\", code); return system(\"php output/apo_async.php\"); }");
            writeLine("if (lang == \"rb\") { __apo_writeFile(\"output/apo_async.rb\", code); return system(\"ruby output/apo_async.rb\"); }");
            writeLine("if (lang == \"kt\") { __apo_writeFile(\"output/ApoAsyncTask.kt\", code); return system(\"kotlinc output/ApoAsyncTask.kt -include-runtime -d output/ApoAsyncTask.jar && java -jar output/ApoAsyncTask.jar\"); }");
            writeLine("std::cerr << \"unsupported syscall language: \" << lang << std::endl;");
            writeLine("return -1;");
            indentLevel--;
            writeLine("}");
            writeLine("static void __apo_queuePayload(const std::string& payload) { __apo_syscallQueue.push_back(payload); }");
            writeLine("static int __apo_executePayload(const std::string& payload) { return __apo_runPayload(payload); }");
            writeLine("static int __apo_executeQueuedPayload(bool drainAll) {");
            indentLevel++;
            writeLine("if (__apo_syscallQueue.empty()) { std::cerr << \"syscall queue empty\" << std::endl; return -1; }");
            writeLine("if (drainAll) {");
            indentLevel++;
            writeLine("while (!__apo_syscallQueue.empty()) {");
            indentLevel++;
            writeLine("__apo_runPayload(__apo_syscallQueue.back());");
            writeLine("__apo_syscallQueue.pop_back();");
            indentLevel--;
            writeLine("}");
            writeLine("return 0;");
            indentLevel--;
            writeLine("}");
            writeLine("std::string payload = __apo_syscallQueue.back();");
            writeLine("__apo_syscallQueue.pop_back();");
            writeLine("return __apo_runPayload(payload);");
            indentLevel--;
            writeLine("}");
            writeLine("static std::vector<std::string> __apo_findIrRecord(const std::string& kind, const std::string& name) {");
            indentLevel++;
            writeLine("std::ifstream in(\"output/apo_ir.txt\");");
            writeLine("if (!in) return {}; ");
            writeLine("std::vector<std::string> lines;");
            writeLine("std::string line;");
            writeLine("while (std::getline(in, line)) {");
            indentLevel++;
            writeLine("if (!line.empty()) lines.push_back(line);");
            indentLevel--;
            writeLine("}");
            writeLine("for (auto it = lines.rbegin(); it != lines.rend(); ++it) {");
            indentLevel++;
            writeLine("std::vector<std::string> fields = __apo_parseIrLine(*it);");
            writeLine("if (fields.size() < 5) continue;");
            writeLine("if (!kind.empty() && fields[0] != kind) continue;");
            writeLine("if (fields[1] != name) continue;");
            writeLine("return fields;");
            indentLevel--;
            writeLine("}");
            writeLine("return {};");
            indentLevel--;
            writeLine("}");
            writeLine("static int async_exec(const std::string& kind, const std::string& name) {");
            indentLevel++;
            writeLine("std::vector<std::string> fields = __apo_findIrRecord(kind, name);");
            writeLine("if (fields.size() < 5) { std::cerr << \"IR entry not found: \" << kind << \" \" << name << std::endl; return -1; }");
            writeLine("return __apo_runPayload(fields[3] + \"\\n\" + fields[4]);");
            indentLevel--;
            writeLine("}");
            writeLine("static int async_exec(const std::string& name) {");
            indentLevel++;
            writeLine("return async_exec(\"\", name);");
            indentLevel--;
            writeLine("}");
            write("\n");
        }

        for (org.antlr.v4.runtime.tree.ParseTree child : ctx.children) {
            if (child instanceof compilerv1Parser.InterfaceContext) {
                visitInterface((compilerv1Parser.InterfaceContext) child);
            }
        }

        for (org.antlr.v4.runtime.tree.ParseTree child : ctx.children) {
            if (child instanceof compilerv1Parser.ClassContext) {
                visitClass((compilerv1Parser.ClassContext) child);
            } else if (child instanceof compilerv1Parser.StructContext) {
                visitStruct((compilerv1Parser.StructContext) child);
            }
        }

        for (org.antlr.v4.runtime.tree.ParseTree child : ctx.children) {
                if (child instanceof compilerv1Parser.ImportStmtContext
                    || child instanceof compilerv1Parser.IncludeContext
                    || child instanceof compilerv1Parser.InterfaceContext
                    || child instanceof compilerv1Parser.ClassContext
                    || child instanceof compilerv1Parser.StructContext
                    || !(child instanceof org.antlr.v4.runtime.ParserRuleContext)) {
                continue;
            }
            if (child instanceof compilerv1Parser.InitContext) {
                visitInit((compilerv1Parser.InitContext) child);
            } else if (child instanceof compilerv1Parser.LambdaContext) {
                visitLambda((compilerv1Parser.LambdaContext) child);
            } else if (child instanceof compilerv1Parser.FunctionContext) {
                visitFunction((compilerv1Parser.FunctionContext) child);
            } else if (child instanceof compilerv1Parser.MacroContext) {
                visitMacro((compilerv1Parser.MacroContext) child);
            } else if (child instanceof compilerv1Parser.InstanceContext) {
                visitInstance((compilerv1Parser.InstanceContext) child);
            } else if (child instanceof compilerv1Parser.InstancepushContext) {
                topLevelStatements.add(renderInstancePush((compilerv1Parser.InstancepushContext) child));
            } else if (child instanceof compilerv1Parser.MemberaccessContext) {
                topLevelStatements.add(renderMemberAccess((compilerv1Parser.MemberaccessContext) child) + ";");
            } else if (child instanceof compilerv1Parser.NativemodeContext) {
                compilerv1Parser.NativemodeContext nativeCtx = (compilerv1Parser.NativemodeContext) child;
                topLevelStatements.add(nativeCtx.OVERRIDE() != null ? renderImmediatePayload(nativeCtx) : renderEnqueuePayload(nativeCtx));
            } else if (child instanceof compilerv1Parser.SyscallStmtContext) {
                topLevelStatements.add(renderSyscallExecution(((compilerv1Parser.SyscallStmtContext) child).ALL() != null));
            }
        }
        if (headerMode || !topLevelStatements.isEmpty() || !dependencies.isEmpty()) {
            if (headerMode) {
                writeLine("static void " + currentModuleInitName() + "() {");
                indentLevel++;
                writeLine("static bool __apo_initialized = false;");
                writeLine("if (__apo_initialized) { return; }");
                writeLine("__apo_initialized = true;");
                for (String dependency : dependencies) {
                    writeLine(toModuleInitName(dependency) + "();");
                }
                for (String statement : topLevelStatements) {
                    writeLine(statement);
                }
                indentLevel--;
                writeLine("}");
            } else {
                write("static int __apo_init = []() {\n");
                indentLevel++;
                for (String dependency : dependencies) {
                    writeLine(toModuleInitName(dependency) + "();");
                }
                for (String statement : topLevelStatements) {
                    writeLine(statement);
                }
                writeLine("return 0;");
                indentLevel--;
                write("}();\n");
            }
        }
        return null;
    }

    @Override
    public Void visitFunction(compilerv1Parser.FunctionContext ctx) {
        if (headerMode && "main".equals(ctx.ID().getText())) {
            return null;
        }
        writeSignature(ctx.returnType(), ctx.ID().getText(), ctx.params(), false, false, false);
        returnTypes.push(rawReturnType(ctx.returnType()));
        pushTypeScope();
        bindParameters(ctx.params());
        writeBlock(ctx.block(), "main".equals(ctx.ID().getText()) && usesGarbageCollector());
        popTypeScope();
        returnTypes.pop();
        write("\n");
        return null;
    }

    @Override
    public Void visitMethod(compilerv1Parser.MethodContext ctx) {
        validateMethodModifiers(ctx);
        String methodKind = ctx.getChild(methodStartIndex(ctx)).getText();

        if ("__construct".equals(methodKind)) {
            String typeName = enclosingTypes.peek();
            write(indent() + typeName);
            writeParameterList(ctx.params());
            returnTypes.push("void");
            writeBlock(ctx.block());
            returnTypes.pop();
            write("\n");
            return null;
        }
        if ("__destruct".equals(methodKind)) {
            String typeName = enclosingTypes.peek();
            write(indent() + "~" + typeName + "() ");
            returnTypes.push("void");
            writeBlock(ctx.block());
            returnTypes.pop();
            write("\n");
            return null;
        }
        writeSignature(ctx.returnType(), ctx.ID().getText(), ctx.params(), ctx.STATIC() != null, ctx.VIRTUAL() != null, ctx.ANNOT_OVERRIDE() != null);
        returnTypes.push(rawReturnType(ctx.returnType()));
        pushTypeScope();
        bindParameters(ctx.params());
        writeBlock(ctx.block());
        popTypeScope();
        returnTypes.pop();
        write("\n");
        return null;
    }

    @Override
    public Void visitField(compilerv1Parser.FieldContext ctx) {
        writeLine(renderType(ctx.typeRef()) + " " + ctx.ID().getText() + ";");
        return null;
    }

    @Override
    public Void visitBlock(compilerv1Parser.BlockContext ctx) {
        writeBlock(ctx);
        return null;
    }

    @Override
    public Void visitStatement(compilerv1Parser.StatementContext ctx) {
        if (ctx.pointer() != null) {
            visitPointer(ctx.pointer());
            return null;
        }
        if (ctx.lambda() != null) {
            visitLambda(ctx.lambda());
            return null;
        }
        if (ctx.init() != null) {
            visitInit(ctx.init());
            return null;
        }
        if (ctx.instance() != null) {
            visitInstance(ctx.instance());
            return null;
        }
        if (ctx.instancepush() != null) {
            visitInstancepush(ctx.instancepush());
            return null;
        }
        if (ctx.nativemode() != null) {
            visitNativemode(ctx.nativemode());
            return null;
        }
        if (ctx.syscallStmt() != null) {
            visitSyscallStmt(ctx.syscallStmt());
            return null;
        }
        if (ctx.assignment() != null) {
            visitAssignment(ctx.assignment());
            return null;
        }
        if (ctx.functionCall() != null) {
            writeLine(renderFunctionCall(ctx.functionCall()) + ";");
            return null;
        }
        if (ctx.memberaccess() != null) {
            writeLine(renderMemberAccess(ctx.memberaccess()) + ";");
            return null;
        }
        if (ctx.ifStatement() != null) {
            visitIfStatement(ctx.ifStatement());
            return null;
        }
        if (ctx.whileStatement() != null) {
            visitWhileStatement(ctx.whileStatement());
            return null;
        }
        if (ctx.class_() != null) {
            visitClass(ctx.class_());
            return null;
        }
        if (ctx.struct() != null) {
            visitStruct(ctx.struct());
            return null;
        }
        if (ctx.interface_() != null) {
            visitInterface(ctx.interface_());
            return null;
        }
        if (ctx.print() != null) {
            visitPrint(ctx.print());
            return null;
        }
        if (ctx.stdin() != null) {
            visitStdin(ctx.stdin());
            return null;
        }
        if (ctx.block() != null) {
            visitBlock(ctx.block());
            return null;
        }
        return null;
    }

    @Override
    public Void visitIfStatement(compilerv1Parser.IfStatementContext ctx) {
        write(indent() + "if (" + renderExpression(ctx.expression()) + ") ");
        writeBlock(ctx.block(0));
        if (ctx.ELSE() != null) {
            write(indent() + "else ");
            writeBlock(ctx.block(1));
        }
        return null;
    }

    @Override
    public Void visitWhileStatement(compilerv1Parser.WhileStatementContext ctx) {
        write(indent() + "while (" + renderExpression(ctx.expression()) + ") ");
        writeBlock(ctx.block());
        return null;
    }

    @Override
    public Void visitAssignment(compilerv1Parser.AssignmentContext ctx) {
        AssignTargetInfo targetInfo = renderAssignTarget(ctx.assignTarget());
        String value = renderExpression(ctx.expression(), targetInfo.valueType);
        if (targetInfo.appendReceiverType != null) {
            String receiver = targetInfo.renderedTarget.substring(0, targetInfo.renderedTarget.length() - ".__apollo_append__".length());
            writeLine(receiver + ".push_back(" + value + ");");
            return null;
        }
        String targetName = ctx.assignTarget().ID().getText();
        String instanceMode = instanceModes.get(targetName);
        if (instanceMode != null && (ctx.assignTarget().accessKey() == null || ctx.assignTarget().accessKey().isEmpty())) {
            String assignedType = inferExpressionType(ctx.expression());
            if ("crt".equals(instanceMode)) {
                if (assignedType != null) {
                    instanceTypes.put(targetName, assignedType);
                } else {
                    instanceTypes.remove(targetName);
                }
            } else if ("staticx".equals(instanceMode)) {
                String declaredType = instanceTypes.get(targetName);
                if (declaredType == null) {
                    throw new IllegalStateException("staticx instance type missing for " + targetName);
                }
                if (assignedType != null && !declaredType.equals(assignedType)) {
                    throw new IllegalArgumentException("Cannot assign " + assignedType + " to staticx instance " + targetName + " of type " + declaredType);
                }
            }
        }
        writeLine(targetInfo.renderedTarget + " = " + value + ";");
        return null;
    }

    @Override
    public Void visitInit(compilerv1Parser.InitContext ctx) {
        String initializer = "";
        ApolloType declaredType = typeFromContext(ctx.typeRef());
        if (ctx.expression() != null) {
            initializer = " = " + renderExpression(ctx.expression(), declaredType);
        }
        writeLine(renderType(ctx.typeRef()) + " " + ctx.ID().getText() + initializer + ";");
        bindVariableType(ctx.ID().getText(), declaredType);
        return null;
    }

    @Override
    public Void visitReturnStmt(compilerv1Parser.ReturnStmtContext ctx) {
        if (ctx.expression() == null) {
            writeLine("return;");
            return null;
        }
        String returnType = returnTypes.peek();
        String expression = renderExpression(ctx.expression());
        if ("void".equals(returnType) && expression != null && "nullptr".equals(expression.trim())) {
            writeLine("return;");
            return null;
        }
        writeLine("return " + expression + ";");
        return null;
    }

    @Override
    public Void visitFunctionCall(compilerv1Parser.FunctionCallContext ctx) {
        write(renderFunctionCall(ctx));
        return null;
    }

    @Override
    public Void visitClass(compilerv1Parser.ClassContext ctx) {
        String typeName = mapDeclaredType(ctx.ID().getText());
        writeLine("class " + typeName + renderInheritanceClause(ctx.inheritanceClause()) + " {");
        enclosingTypes.push(typeName);
        indentLevel++;
        String[] currentAccess = {"private"};
        for (compilerv1Parser.ClassMemberContext member : ctx.classBody().classMember()) {
            String nextAccess = null;
            if (member.method() != null && member.method().CLSTYPE() != null) {
                nextAccess = member.method().CLSTYPE().getText();
            } else if (member.method() != null && isLifecycleMethod(member.method())) {
                nextAccess = "public";
            }
            if (member.field() != null && member.field().CLSTYPE() != null) {
                nextAccess = member.field().CLSTYPE().getText();
            }
            emitAccessLabel(nextAccess, currentAccess);
            if (member.method() != null) {
                visitMethod(member.method());
            } else if (member.field() != null) {
                visitField(member.field());
            } else if (member.class_() != null) {
                visitClass(member.class_());
            } else if (member.struct() != null) {
                visitStruct(member.struct());
            }
        }
        indentLevel--;
        enclosingTypes.pop();
        writeLine("};");
        write("\n");
        return null;
    }

    @Override
    public Void visitStruct(compilerv1Parser.StructContext ctx) {
        String typeName = mapDeclaredType(ctx.ID().getText());
        writeLine("struct " + typeName + renderInheritanceClause(ctx.inheritanceClause()) + " {");
        enclosingTypes.push(typeName);
        indentLevel++;
        String[] currentAccess = {"public"};
        for (compilerv1Parser.StructMemberContext member : ctx.structBody().structMember()) {
            String nextAccess = null;
            if (member.method() != null && member.method().CLSTYPE() != null) {
                nextAccess = member.method().CLSTYPE().getText();
            }
            if (member.field() != null && member.field().CLSTYPE() != null) {
                nextAccess = member.field().CLSTYPE().getText();
            }
            emitAccessLabel(nextAccess, currentAccess);
            if (member.method() != null) {
                visitMethod(member.method());
            } else if (member.field() != null) {
                visitField(member.field());
            } else if (member.class_() != null) {
                visitClass(member.class_());
            } else if (member.struct() != null) {
                visitStruct(member.struct());
            }
        }
        indentLevel--;
        enclosingTypes.pop();
        writeLine("};");
        write("\n");
        return null;
    }

    
}

#!/usr/bin/env bash

set -euo pipefail

write_error() {
    echo "$1" >&2
}

resolve_path() {
    local target=$1
    if [[ "$target" = /* ]]; then
        if [[ -d "$target" ]]; then
            (cd -- "$target" && pwd -P)
            return 0
        fi

        local absolute_parent
        absolute_parent=$(dirname -- "$target")
        local absolute_name
        absolute_name=$(basename -- "$target")
        if [[ -d "$absolute_parent" ]]; then
            (cd -- "$absolute_parent" && printf '%s/%s\n' "$(pwd -P)" "$absolute_name")
            return 0
        fi

        printf '%s\n' "$target"
        return 0
    fi

    resolve_path "$PWD/$target"
}

find_tool() {
    local env_dir=$1
    local tool_name=$2

    if [[ -n "$env_dir" && -x "$env_dir/$tool_name" ]]; then
        printf '%s\n' "$env_dir/$tool_name"
        return 0
    fi

    command -v "$tool_name"
}

resolve_native_target() {
    if [[ -n "${APOLLO_CLANG_TARGET-}" ]]; then
        printf '%s\n' "$APOLLO_CLANG_TARGET"
    fi
}

find_apollo_binary() {
    local base_name=$1
    local candidate

    for candidate in "$APOLLO_DIR/$base_name"; do
        if [[ -f "$candidate" ]]; then
            printf '%s\n' "$candidate"
            return 0
        fi
    done

    return 1
}

run_cleanup() {
    local preserve_path=${1-}
    if [[ ! -f "$CLEANUP_SCRIPT" ]]; then
        return 0
    fi

    if [[ -n "$preserve_path" ]]; then
        bash "$CLEANUP_SCRIPT" --manifest-path "$MANIFEST_PATH" --output-dir "$SCRIPT_DIR/output" --preserve-path "$preserve_path" >/dev/null 2>/dev/null || true
        return 0
    fi

    bash "$CLEANUP_SCRIPT" --manifest-path "$MANIFEST_PATH" --output-dir "$SCRIPT_DIR/output" >/dev/null 2>/dev/null || true
}

resolve_gc_paths() {
    if [[ -z "${APOLLO_GC_INCLUDE_DIR-}" || -z "${APOLLO_GC_LIB_DIR-}" ]]; then
        if command -v brew >/dev/null 2>&1; then
            local gc_prefix
            gc_prefix=$(brew --prefix bdw-gc 2>/dev/null || true)
            if [[ -n "$gc_prefix" ]]; then
                APOLLO_GC_INCLUDE_DIR=${APOLLO_GC_INCLUDE_DIR-"$gc_prefix/include"}
                APOLLO_GC_LIB_DIR=${APOLLO_GC_LIB_DIR-"$gc_prefix/lib"}
            fi
        fi

        if [[ -z "${APOLLO_GC_INCLUDE_DIR-}" ]]; then
            local include_candidate
            for include_candidate in /opt/homebrew/include /usr/local/include /usr/include; do
                if [[ -f "$include_candidate/gc_cpp.h" || -f "$include_candidate/gc/gc_cpp.h" || -f "$include_candidate/gc.h" || -f "$include_candidate/gc/gc.h" ]]; then
                    APOLLO_GC_INCLUDE_DIR=$include_candidate
                    break
                fi
            done
        fi

        if [[ -z "${APOLLO_GC_LIB_DIR-}" ]]; then
            local lib_candidate
            for lib_candidate in /opt/homebrew/lib /usr/local/lib /usr/lib /usr/lib64; do
                if [[ -f "$lib_candidate/libgc.a" || -f "$lib_candidate/libgc.dylib" || -f "$lib_candidate/libgc.so" ]]; then
                    APOLLO_GC_LIB_DIR=$lib_candidate
                    break
                fi
            done
        fi
    fi

    if [[ -z "${APOLLO_GC_INCLUDE_DIR-}" || -z "${APOLLO_GC_LIB_DIR-}" ]]; then
        write_error "GC headers or libraries were requested by this Apollo program, but Boehm GC was not found."
        write_error "Run the Unix installer from the Apollo install directory to bootstrap the required toolchain."
        exit 1
    fi

    if [[ ! -f "$APOLLO_GC_INCLUDE_DIR/gc_cpp.h" && ! -f "$APOLLO_GC_INCLUDE_DIR/gc/gc_cpp.h" && ! -f "$APOLLO_GC_INCLUDE_DIR/gc.h" && ! -f "$APOLLO_GC_INCLUDE_DIR/gc/gc.h" ]]; then
        write_error "GC headers were not found in $APOLLO_GC_INCLUDE_DIR."
        exit 1
    fi

    if [[ ! -f "$APOLLO_GC_LIB_DIR/libgc.a" && ! -f "$APOLLO_GC_LIB_DIR/libgc.dylib" && ! -f "$APOLLO_GC_LIB_DIR/libgc.so" ]]; then
        write_error "libgc was not found in $APOLLO_GC_LIB_DIR."
        exit 1
    fi

    GC_COMPILE_FLAGS=(-I"$APOLLO_GC_INCLUDE_DIR")
    if [[ -f "$APOLLO_GC_LIB_DIR/libgccpp.a" || -f "$APOLLO_GC_LIB_DIR/libgccpp.dylib" || -f "$APOLLO_GC_LIB_DIR/libgccpp.so" ]]; then
        GC_LINK_FLAGS=(-L"$APOLLO_GC_LIB_DIR" -lgccpp -lgc)
    else
        GC_LINK_FLAGS=(-L"$APOLLO_GC_LIB_DIR" -lgc)
    fi
}

configure_gc_support() {
    GC_COMPILE_FLAGS=()
    GC_LINK_FLAGS=()

    if ! grep -E -q '#include <gc/|#include <gc_cpp\.h>|#include <gc\.h>' output.cpp 2>/dev/null; then
        return 0
    fi

    resolve_gc_paths
}

BIN_MODE=0
if [[ ${1-} == "-bin" ]]; then
    BIN_MODE=1
    shift
fi

COMMAND=${1-}
if [[ -z "$COMMAND" ]]; then
    write_error "Usage: apollo [-bin] <ctall|run> [filepath] [outputname]"
    exit 1
fi
shift || true

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)
if [[ -n "${APOLLO_COMPILER_DIR-}" ]]; then
    SCRIPT_DIR=$APOLLO_COMPILER_DIR
fi

if [[ -n "${APOLLO_DIR-}" ]]; then
    APOLLO_DIR=$APOLLO_DIR
else
    APOLLO_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd -P)
fi

ANTLR_JAR="$SCRIPT_DIR/antlr-4.13.2-complete.jar"
APOLLO_JIT_EXE=$(find_apollo_binary apollo_jit || true)
CONFIG_EXE=$(find_apollo_binary apollo-config || true)
CLEANUP_SCRIPT="$SCRIPT_DIR/cleanup-output.sh"
MANIFEST_PATH="$SCRIPT_DIR/output/cleanup-manifest.txt"
CALLER_DIR=$PWD
RAW_INPUT=${1-}
RAW_OUTPUT=${2-}
TOOLCHAIN_ENV="$SCRIPT_DIR/toolchain-env.sh"

if [[ -f "$TOOLCHAIN_ENV" ]]; then
    # shellcheck source=/dev/null
    . "$TOOLCHAIN_ENV"
fi

JAVA_EXE=$(find_tool "${APOLLO_JAVA_BIN-}" java)
JAVAC_EXE=$(find_tool "${APOLLO_JAVA_BIN-}" javac)
CLANG_EXE=$(find_tool "${APOLLO_LLVM_BIN-}" clang)
CLANGXX_EXE=$(find_tool "${APOLLO_LLVM_BIN-}" clang++)
LLC_EXE=$(find_tool "${APOLLO_LLVM_BIN-}" llc)
CLANG_TARGET=$(resolve_native_target || true)
LLC_TRIPLE=${APOLLO_LLC_TRIPLE:-${CLANG_TARGET-}}
TARGET_FLAGS=()
if [[ -n "$CLANG_TARGET" ]]; then
    TARGET_FLAGS+=(--target="$CLANG_TARGET")
fi
if [[ -n "${APOLLO_SYSROOT-}" ]]; then
    TARGET_FLAGS+=(--sysroot="$APOLLO_SYSROOT")
fi
if [[ -n "${APOLLO_GCC_TOOLCHAIN-}" ]]; then
    TARGET_FLAGS+=(--gcc-toolchain="$APOLLO_GCC_TOOLCHAIN")
fi

if [[ -n "$RAW_INPUT" ]]; then
    INPUT_FILE=$(resolve_path "$RAW_INPUT")
else
    INPUT_FILE="$CALLER_DIR/main.apollo"
fi

if [[ $BIN_MODE -eq 1 && -z "$RAW_OUTPUT" ]]; then
    write_error "Missing output path. Usage: apollo -bin <ctall|run> [filename] outputname"
    exit 1
fi

if [[ $BIN_MODE -eq 0 && -n "$RAW_OUTPUT" ]]; then
    write_error "Unexpected extra argument. Usage: apollo <ctall|run> [filename]"
    exit 1
fi

case "$COMMAND" in
    run|ctall)
        ;;
    *)
        write_error "Unknown command. Usage: apollo [-bin] <run|ctall> [filename] [outputname]"
        exit 1
        ;;
esac

echo "Running $COMMAND on $INPUT_FILE..."

cd -- "$SCRIPT_DIR"

if [[ ! -f "$INPUT_FILE" ]]; then
    write_error "Input file not found: $INPUT_FILE"
    exit 1
fi

if [[ $BIN_MODE -eq 1 ]]; then
    mkdir -p -- "$(dirname -- "$RAW_OUTPUT")"
    BIN_OUTPUT=$(resolve_path "$RAW_OUTPUT")
fi

echo "Regenerating parser/visitor from compilerv1.g4 into compiler-master"
"$JAVA_EXE" -jar "$ANTLR_JAR" -visitor -Dlanguage=Java -o compiler-master compilerv1.g4

SOURCE_ROOT=$(dirname -- "$INPUT_FILE")
mkdir -p output/classes

pushd compiler-master >/dev/null
"$JAVAC_EXE" -d ../output/classes -cp ".:$ANTLR_JAR" ./*.java
popd >/dev/null

"$JAVAC_EXE" -d output/classes -cp ".:output/classes:$ANTLR_JAR" CppCodeGenVisitor.java Main.java
"$JAVA_EXE" -cp "output/classes:$ANTLR_JAR" Main "$INPUT_FILE"

configure_gc_support

EXECUTION_MODE=aot
if [[ $BIN_MODE -eq 0 && -n "$CONFIG_EXE" && -f "$CONFIG_EXE" ]]; then
    if ! EXECUTION_MODE=$("$CONFIG_EXE" get-mode 2>/dev/null); then
        EXECUTION_MODE=aot
    fi
fi

if [[ -n "$CLANG_TARGET" ]]; then
    echo "Emitting LLVM IR with clang for target $CLANG_TARGET"
else
    echo "Emitting LLVM IR with clang"
fi
clang_args=("$CLANG_EXE" "${TARGET_FLAGS[@]}" -I"$SOURCE_ROOT" -x c++ -std=c++20 -S -emit-llvm output.cpp -o output/output.ll)
if [[ ${#GC_COMPILE_FLAGS[@]} -gt 0 ]]; then
    clang_args=("$CLANG_EXE" "${TARGET_FLAGS[@]}" -I"$SOURCE_ROOT" "${GC_COMPILE_FLAGS[@]}" -x c++ -std=c++20 -S -emit-llvm output.cpp -o output/output.ll)
fi
"${clang_args[@]}"

if [[ "$EXECUTION_MODE" == "jit" ]]; then
    if [[ -z "$APOLLO_JIT_EXE" || ! -f "$APOLLO_JIT_EXE" ]]; then
        write_error "LLVM JIT runner not found. Build or install apollo_jit before selecting jit mode."
        exit 1
    fi

    echo "Running LLVM JIT with $(basename -- "$APOLLO_JIT_EXE")"
    "$APOLLO_JIT_EXE" output/output.ll
    EXIT_CODE=$?
    run_cleanup
    exit $EXIT_CODE
fi

echo "Lowering LLVM IR to an object file with llc"
llc_args=("$LLC_EXE")
if [[ -n "$LLC_TRIPLE" ]]; then
    llc_args+=("-mtriple=$LLC_TRIPLE")
fi
llc_args+=(-relocation-model=pic -filetype=obj output/output.ll -o output/output.o)
"${llc_args[@]}"

LINK_OUTPUT=output/output
if [[ $BIN_MODE -eq 1 ]]; then
    LINK_OUTPUT=$BIN_OUTPUT
fi

if [[ -n "$CLANG_TARGET" ]]; then
    echo "Linking output/output.o with clang++ for target $CLANG_TARGET"
else
    echo "Linking output/output.o with clang++"
fi
clangxx_args=("$CLANGXX_EXE" "${TARGET_FLAGS[@]}" output/output.o -o "$LINK_OUTPUT")
if [[ ${#GC_LINK_FLAGS[@]} -gt 0 ]]; then
    clangxx_args=("$CLANGXX_EXE" "${TARGET_FLAGS[@]}" output/output.o -o "$LINK_OUTPUT" "${GC_LINK_FLAGS[@]}")
fi
"${clangxx_args[@]}"

if [[ $BIN_MODE -eq 1 ]]; then
    run_cleanup "$BIN_OUTPUT"
    echo "Wrote binary to $BIN_OUTPUT"
    exit 0
fi

"$LINK_OUTPUT"
EXIT_CODE=$?
run_cleanup
exit $EXIT_CODE
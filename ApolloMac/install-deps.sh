#!/usr/bin/env bash

set -euo pipefail

INSTALL_DIR=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --install-dir)
            INSTALL_DIR=${2-}
            shift 2
            ;;
        *)
            echo "Unknown option: $1" >&2
            exit 1
            ;;
    esac
done

if [[ -z "$INSTALL_DIR" ]]; then
    echo "install-deps.sh requires --install-dir <path>" >&2
    exit 1
fi

write_status() {
    echo "[apollo-install] $1"
}

require_command() {
    local tool_name=$1
    if ! command -v "$tool_name" >/dev/null 2>&1; then
        echo "Missing required tool: $tool_name" >&2
        exit 1
    fi
}

require_file() {
    local file_path=$1
    local description=$2
    if [[ ! -e "$file_path" ]]; then
        echo "Missing required toolchain component: $description at $file_path" >&2
        exit 1
    fi
}

resolve_java_bin() {
    if [[ -x "${1-}/bin/java" && -x "${1-}/bin/javac" ]]; then
        printf '%s/bin\n' "$1"
        return 0
    fi

    local javac_path
    javac_path=$(command -v javac)
    dirname -- "$javac_path"
}

write_toolchain_env() {
    local compiler_dir=$1
    local llvm_bin=$2
    local java_bin=$3
    local gc_include_dir=$4
    local gc_lib_dir=$5
    local env_file=$compiler_dir/toolchain-env.sh

    require_file "$llvm_bin/clang" "clang"
    require_file "$llvm_bin/clang++" "clang++"
    require_file "$llvm_bin/llc" "llc"
    require_file "$llvm_bin/llvm-config" "llvm-config"
    require_file "$java_bin/java" "java"
    require_file "$java_bin/javac" "javac"

    cat > "$env_file" <<EOF
export APOLLO_LLVM_BIN="$llvm_bin"
export APOLLO_JAVA_BIN="$java_bin"
export APOLLO_GC_INCLUDE_DIR="$gc_include_dir"
export APOLLO_GC_LIB_DIR="$gc_lib_dir"
export PATH="\$APOLLO_LLVM_BIN:\$APOLLO_JAVA_BIN:\$PATH"
EOF

    write_status "Wrote Apollo toolchain environment file to $env_file"
}

install_macos_dependencies() {
    require_command brew

    local formula
    for formula in llvm openjdk@21 bdw-gc; do
        if brew list "$formula" >/dev/null 2>&1; then
            write_status "$formula already present"
            continue
        fi

        write_status "Installing $formula via Homebrew"
        brew install "$formula"
    done

    local llvm_prefix
    local java_prefix
    local gc_prefix
    llvm_prefix=$(brew --prefix llvm)
    java_prefix=$(brew --prefix openjdk@21)
    gc_prefix=$(brew --prefix bdw-gc)

    write_toolchain_env "$INSTALL_DIR/compiler" "$llvm_prefix/bin" "$java_prefix/bin" "$gc_prefix/include" "$gc_prefix/lib"
}

INSTALL_DIR=$(cd -- "$INSTALL_DIR" && pwd -P)
if [[ ! -d "$INSTALL_DIR/compiler" ]]; then
    echo "Apollo compiler directory not found at $INSTALL_DIR/compiler" >&2
    exit 1
fi

case "$(uname -s)" in
    Darwin)
        install_macos_dependencies
        ;;
    *)
        echo "Apollo's dependency installer supports macOS only." >&2
        echo "Install LLVM, Java 21, and Boehm GC manually, then create compiler/toolchain-env.sh." >&2
        exit 1
        ;;
esac

write_status "Dependency bootstrap complete"
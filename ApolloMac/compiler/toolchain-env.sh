if [[ "$(uname -s)" == "Darwin" ]]; then
    if command -v brew >/dev/null 2>&1; then
        _llvm_prefix=$(brew --prefix llvm 2>/dev/null || true)
        _java_prefix=$(brew --prefix openjdk@21 2>/dev/null || brew --prefix openjdk 2>/dev/null || true)
        _gc_prefix=$(brew --prefix bdw-gc 2>/dev/null || true)
        [[ -n "$_llvm_prefix" ]] && export APOLLO_LLVM_BIN="$_llvm_prefix/bin"
        [[ -n "$_java_prefix" ]] && export APOLLO_JAVA_BIN="$_java_prefix/bin"
        [[ -n "$_gc_prefix" ]] && export APOLLO_GC_INCLUDE_DIR="$_gc_prefix/include"
        [[ -n "$_gc_prefix" ]] && export APOLLO_GC_LIB_DIR="$_gc_prefix/lib"
    fi
else
    export APOLLO_LLVM_BIN="/usr/bin"
    _javac_path=$(command -v javac 2>/dev/null || true)
    export APOLLO_JAVA_BIN="${_javac_path:+$(dirname -- "$_javac_path")}"
    export APOLLO_GC_INCLUDE_DIR="/usr/include"
    _gc_arch_lib="/usr/lib/$(uname -m)-linux-gnu"
    if [[ -d "$_gc_arch_lib" ]]; then
        export APOLLO_GC_LIB_DIR="$_gc_arch_lib"
    else
        export APOLLO_GC_LIB_DIR="/usr/lib"
    fi
fi
export PATH="${APOLLO_LLVM_BIN:+$APOLLO_LLVM_BIN:}${APOLLO_JAVA_BIN:+$APOLLO_JAVA_BIN:}$PATH"

.PHONY: all clean

SHELL := /bin/sh

TOOLCHAIN_ENV := compiler/toolchain-env.sh
LLVM_BIN := $(shell if [ -f $(TOOLCHAIN_ENV) ]; then . $(TOOLCHAIN_ENV) >/dev/null 2>&1; printf '%s' "$$APOLLO_LLVM_BIN"; fi)
LLVM_CONFIG := $(if $(LLVM_BIN),$(LLVM_BIN)/llvm-config,llvm-config)
SOURCE_DIR := src
APOLLO_SRC := $(SOURCE_DIR)/apollo.cpp
INSTALL_SRC := $(SOURCE_DIR)/install.cpp
CONFIG_SRC := $(SOURCE_DIR)/config.cpp
JIT_SRC := $(SOURCE_DIR)/apollo_jit.cpp

# macOS target. Override MACOS_ARCH=x86_64 for Intel Macs.
MACOS_ARCH   ?= arm64
MACOS_MIN_OS ?= 14.0
MACOS_TARGET := $(MACOS_ARCH)-apple-macosx$(MACOS_MIN_OS)

# When cross-compiling from a non-macOS host, set MACOS_SDK to a macOS SDK path.
# Example: make MACOS_SDK=/opt/osxcross/SDK/MacOSX14.sdk
MACOS_SDK ?=

UNAME_S := $(shell uname -s 2>/dev/null || echo Unknown)

CXX ?= $(if $(LLVM_BIN),$(LLVM_BIN)/clang++,clang++)
CXXFLAGS ?= -std=c++20 -O2

# On non-macOS hosts, inject the Apple target triple (and optional sysroot) so
# clang emits Mach-O rather than ELF/PE objects.
ifeq ($(UNAME_S),Darwin)
PLATFORM_FLAGS :=
else
PLATFORM_FLAGS := --target=$(MACOS_TARGET)
ifneq ($(MACOS_SDK),)
PLATFORM_FLAGS += --sysroot=$(MACOS_SDK)
endif
endif

ALL_CXXFLAGS  := $(CXXFLAGS) $(PLATFORM_FLAGS)
JIT_CXXFLAGS  := $(shell command -v $(LLVM_CONFIG) >/dev/null 2>&1 && $(LLVM_CONFIG) --cxxflags) $(ALL_CXXFLAGS)
JIT_LDFLAGS   := $(shell command -v $(LLVM_CONFIG) >/dev/null 2>&1 && $(LLVM_CONFIG) --ldflags --system-libs --libs orcjit native irreader) $(PLATFORM_FLAGS)
GC_LINK_FLAGS := $(shell if [ -f $(TOOLCHAIN_ENV) ]; then . $(TOOLCHAIN_ENV) >/dev/null 2>&1; if [ -n "$$APOLLO_GC_LIB_DIR" ]; then printf '%s' "-L$$APOLLO_GC_LIB_DIR"; if [ -e "$$APOLLO_GC_LIB_DIR/libgccpp.a" ] || [ -e "$$APOLLO_GC_LIB_DIR/libgccpp.so" ] || [ -e "$$APOLLO_GC_LIB_DIR/libgccpp.dylib" ]; then printf ' -lgccpp'; fi; if [ -e "$$APOLLO_GC_LIB_DIR/libgc.a" ] || [ -e "$$APOLLO_GC_LIB_DIR/libgc.so" ] || [ -e "$$APOLLO_GC_LIB_DIR/libgc.dylib" ] || ls "$$APOLLO_GC_LIB_DIR"/libgc.so.* >/dev/null 2>&1; then printf ' -lgc'; fi; fi; fi)

TARGETS := apollo apollo-config apollo_jit install

all: $(TARGETS)

apollo: $(APOLLO_SRC) platform_support.hpp
	$(CXX) $(ALL_CXXFLAGS) -o $@ $(APOLLO_SRC)

apollo-config: $(CONFIG_SRC) platform_support.hpp
	$(CXX) $(ALL_CXXFLAGS) -o $@ $(CONFIG_SRC)

apollo_jit: $(JIT_SRC)
	$(CXX) $(JIT_CXXFLAGS) -o $@ $(JIT_SRC) $(JIT_LDFLAGS) $(GC_LINK_FLAGS)

install: $(INSTALL_SRC) platform_support.hpp
	$(CXX) $(ALL_CXXFLAGS) -o $@ $(INSTALL_SRC)

clean:
	rm -f $(TARGETS)
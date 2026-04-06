#include <cstdint>
#include <cstdlib>
#include <iostream>
#include <memory>
#include <string>

#include <llvm/ExecutionEngine/Orc/Core.h>
#include <llvm/ExecutionEngine/Orc/ExecutionUtils.h>
#include <llvm/ExecutionEngine/Orc/LLJIT.h>
#include <llvm/IR/Module.h>
#include <llvm/IRReader/IRReader.h>
#include <llvm/Support/DynamicLibrary.h>
#include <llvm/Support/Error.h>
#include <llvm/Support/InitLLVM.h>
#include <llvm/Support/SourceMgr.h>
#include <llvm/Support/TargetSelect.h>

using namespace llvm;
using namespace llvm::orc;

extern "C" {
#ifndef __APPLE__
uintptr_t __stack_chk_guard = 0x0A90110AULL;
#endif

void __main() {
}

#ifndef __APPLE__
void __stack_chk_fail() {
    std::abort();
}
#endif
}

namespace {
    struct LoadedModule {
        ThreadSafeModule module;
        bool needsGc;

        LoadedModule(ThreadSafeModule loadedModule, bool requiresGc)
            : module(std::move(loadedModule)), needsGc(requiresGc) {
        }
    };

    void printError(Error error) {
        logAllUnhandledErrors(std::move(error), errs(), "apollo_jit: ");
    }

    bool moduleRequiresGc(const Module& module) {
        for (const Function& function : module.functions()) {
            if (function.isDeclaration() && function.getName().starts_with("GC_")) {
                return true;
            }
        }

        for (const GlobalVariable& global : module.globals()) {
            if (global.isDeclaration() && global.getName().starts_with("GC_")) {
                return true;
            }
        }

        return false;
    }

    Expected<std::unique_ptr<LLJIT>> createJit() {
        InitializeNativeTarget();
        InitializeNativeTargetAsmPrinter();
        InitializeNativeTargetAsmParser();

        auto jit = LLJITBuilder().create();
        if (!jit) {
            return jit.takeError();
        }

        auto& mainDylib = (*jit)->getMainJITDylib();
        auto currentProcessGenerator = DynamicLibrarySearchGenerator::GetForCurrentProcess(
            (*jit)->getDataLayout().getGlobalPrefix());
        if (!currentProcessGenerator) {
            return currentProcessGenerator.takeError();
        }
        mainDylib.addGenerator(std::move(*currentProcessGenerator));

        MangleAndInterner mangle((*jit)->getExecutionSession(), (*jit)->getDataLayout());
        SymbolMap shimSymbols;
        shimSymbols[mangle("__main")] = ExecutorSymbolDef(ExecutorAddr::fromPtr(&__main), JITSymbolFlags::Exported);
#ifndef __APPLE__
        shimSymbols[mangle("__stack_chk_fail")] = ExecutorSymbolDef(ExecutorAddr::fromPtr(&__stack_chk_fail), JITSymbolFlags::Exported);
        shimSymbols[mangle("__stack_chk_guard")] = ExecutorSymbolDef(ExecutorAddr::fromPtr(&__stack_chk_guard), JITSymbolFlags::Exported);
#endif
        if (Error error = mainDylib.define(absoluteSymbols(shimSymbols))) {
            return std::move(error);
        }

        return std::move(*jit);
    }

    Error verifyRuntimeSupport(bool needsGc) {
        if (!needsGc) {
            return Error::success();
        }

        for (const char* symbolName : {"GC_malloc", "_GC_malloc"}) {
            if (llvm::sys::DynamicLibrary::SearchForAddressOfSymbol(symbolName) != nullptr) {
                return Error::success();
            }
        }

        return createStringError(
            inconvertibleErrorCode(),
            "Apollo program requires Boehm GC support, but apollo_jit was built without libgc symbols available. Rebuild apollo_jit after installing bdw-gc.");
    }

    Expected<LoadedModule> loadModule(const std::string& inputPath, const DataLayout& dataLayout) {
        auto context = std::make_unique<LLVMContext>();
        SMDiagnostic diagnostics;
        auto module = parseIRFile(inputPath, diagnostics, *context);
        if (!module) {
            std::string message;
            raw_string_ostream stream(message);
            diagnostics.print("apollo_jit", stream);
            stream.flush();
            return createStringError(inconvertibleErrorCode(), message);
        }

        bool needsGc = moduleRequiresGc(*module);
        module->setDataLayout(dataLayout);
        return LoadedModule(ThreadSafeModule(std::move(module), std::move(context)), needsGc);
    }

    int runMain(const ExecutorAddr& mainAddress) {
        using MainFunction = int (*)();
        MainFunction mainFunction = mainAddress.toPtr<MainFunction>();
        return mainFunction();
    }
}

int main(int argc, char* argv[]) {
    InitLLVM initLLVM(argc, argv);

    if (argc < 2) {
        std::cerr << "Usage: apollo_jit <module.ll>" << std::endl;
        return 1;
    }

    auto jit = createJit();
    if (!jit) {
        printError(jit.takeError());
        return 1;
    }

    auto loadedModule = loadModule(argv[1], (*jit)->getDataLayout());
    if (!loadedModule) {
        printError(loadedModule.takeError());
        return 1;
    }

    if (Error error = verifyRuntimeSupport(loadedModule->needsGc)) {
        printError(std::move(error));
        return 1;
    }

    if (Error error = (*jit)->addIRModule(std::move(loadedModule->module))) {
        printError(std::move(error));
        return 1;
    }

    if (Error error = (*jit)->initialize((*jit)->getMainJITDylib())) {
        printError(std::move(error));
        return 1;
    }

    auto mainSymbol = (*jit)->lookup("main");
    if (!mainSymbol) {
        printError(mainSymbol.takeError());
        return 1;
    }

    int exitCode = runMain(*mainSymbol);

    if (Error error = (*jit)->deinitialize((*jit)->getMainJITDylib())) {
        printError(std::move(error));
        return 1;
    }

    return exitCode;
}

#include <algorithm>
#include <cctype>
#include <filesystem>
#include <fstream>
#include <iostream>
#include <string>

#include "../platform_support.hpp"

namespace {
    std::filesystem::path findSiblingBinary(const std::string& baseName) {
        std::filesystem::path executableDir = apollo::platform::getExecutableDir();
        std::filesystem::path candidate = executableDir / baseName;
        return std::filesystem::exists(candidate) ? candidate : std::filesystem::path();
    }

    std::filesystem::path getConfigPath() {
        return apollo::platform::getExecutableDir() / "apollo_backend.cfg";
    }

    std::string trim(const std::string& value) {
        size_t start = 0;
        while (start < value.size() && std::isspace(static_cast<unsigned char>(value[start]))) {
            ++start;
        }

        size_t end = value.size();
        while (end > start && std::isspace(static_cast<unsigned char>(value[end - 1]))) {
            --end;
        }

        return value.substr(start, end - start);
    }

    std::string normalizeMode(std::string value) {
        std::transform(value.begin(), value.end(), value.begin(), [](unsigned char ch) {
            return static_cast<char>(std::tolower(ch));
        });

        value = trim(value);
        if (value == "obj") {
            return "aot";
        }
        if (value == "llvm-jit") {
            return "jit";
        }
        return value;
    }

    std::string readMode() {
        std::ifstream in(getConfigPath());
        if (!in) {
            return "aot";
        }

        std::string value;
        std::getline(in, value);
        value = normalizeMode(value);
        if (value != "jit") {
            return "aot";
        }

        return value;
    }

    bool writeMode(const std::string& mode) {
        std::ofstream out(getConfigPath(), std::ios::trunc);
        if (!out) {
            return false;
        }

        out << mode << std::endl;
        return static_cast<bool>(out);
    }

    bool isJitSupported(std::string* reason = nullptr) {
        std::filesystem::path jitRunnerPath = findSiblingBinary("apollo_jit");
        if (jitRunnerPath.empty()) {
            if (reason != nullptr) {
                *reason = "LLVM JIT support requires apollo_jit in the Apollo install directory.";
            }
            return false;
        }

        if (reason != nullptr) {
            reason->clear();
        }

        return true;
    }

    void printUsage() {
        std::cout << "Usage: apollo-config [aot|jit|get-mode|status|jit-status]" << std::endl;
    }
}

int main(int argc, char* argv[]) {
    if (argc == 1) {
        std::string reason;
        std::cout << "Apollo execution mode: " << readMode() << std::endl;
        if (isJitSupported(&reason)) {
            std::cout << "LLVM JIT support: available" << std::endl;
            return 0;
        }

        std::cout << "LLVM JIT support: unavailable" << std::endl;
        std::cout << reason << std::endl;
        return 0;
    }

    std::string command = normalizeMode(argv[1]);
    if (command == "get-mode") {
        std::cout << readMode() << std::endl;
        return 0;
    }

    if (command == "status") {
        std::string reason;
        std::cout << "Apollo execution mode: " << readMode() << std::endl;
        if (isJitSupported(&reason)) {
            std::cout << "LLVM JIT support: available" << std::endl;
            return 0;
        }

        std::cout << "LLVM JIT support: unavailable" << std::endl;
        std::cout << reason << std::endl;
        return 0;
    }

    if (command == "jit-status") {
        std::string reason;
        if (isJitSupported(&reason)) {
            std::cout << "available" << std::endl;
            return 0;
        }

        std::cerr << reason << std::endl;
        return 1;
    }

    if (command != "aot" && command != "jit") {
        printUsage();
        return 1;
    }

    if (command == "jit") {
        std::string reason;
        if (!isJitSupported(&reason)) {
            std::cerr << reason << std::endl;
            return 1;
        }
    }

    if (!writeMode(command)) {
        std::cerr << "Failed to update " << getConfigPath().string() << std::endl;
        return 1;
    }

    std::cout << "Apollo execution mode set to " << command << std::endl;
    return 0;
}
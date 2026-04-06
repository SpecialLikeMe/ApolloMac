#pragma once

#include <cstdlib>
#include <filesystem>
#include <limits.h>
#include <spawn.h>
#include <string>
#include <sys/wait.h>
#include <unistd.h>
#include <vector>

#ifdef __APPLE__
#include <mach-o/dyld.h>
#endif
extern char **environ;

namespace apollo::platform {

inline std::filesystem::path getExecutablePath() {
#ifdef __APPLE__
    std::vector<char> buffer(1024, '\0');
    uint32_t size = static_cast<uint32_t>(buffer.size());
    while (_NSGetExecutablePath(buffer.data(), &size) != 0) {
        buffer.assign(size, '\0');
    }

    std::error_code error;
    std::filesystem::path executablePath(buffer.data());
    std::filesystem::path canonicalPath = std::filesystem::weakly_canonical(executablePath, error);
    if (error) {
        return executablePath;
    }
    return canonicalPath;
#else
    std::vector<char> buffer(PATH_MAX > 0 ? PATH_MAX : 1024, '\0');
    while (true) {
        ssize_t length = readlink("/proc/self/exe", buffer.data(), buffer.size());
        if (length < 0) {
            return {};
        }
        if (static_cast<size_t>(length) < buffer.size()) {
            return std::filesystem::path(std::string(buffer.data(), static_cast<size_t>(length)));
        }
        buffer.assign(buffer.size() * 2, '\0');
    }
#endif
}

inline std::filesystem::path getExecutableDir() {
    std::filesystem::path executablePath = getExecutablePath();
    return executablePath.empty() ? std::filesystem::current_path() : executablePath.parent_path();
}

inline std::filesystem::path getCommandInterpreter() {
    if (const char* shell = std::getenv("SHELL"); shell != nullptr && *shell != '\0') {
        std::filesystem::path shellPath(shell);
        if (std::filesystem::exists(shellPath)) {
            return shellPath;
        }
    }

    return "/bin/sh";
}

inline std::filesystem::path getBashInterpreter() {
    if (const char* bashEnv = std::getenv("BASH"); bashEnv != nullptr && *bashEnv != '\0') {
        std::filesystem::path bashPath(bashEnv);
        if (std::filesystem::exists(bashPath)) {
            return bashPath;
        }
    }

    std::filesystem::path defaultBash("/bin/bash");
    if (std::filesystem::exists(defaultBash)) {
        return defaultBash;
    }

    return getCommandInterpreter();
}

inline bool setEnvironmentVariable(const std::string& name, const std::string& value) {
    return setenv(name.c_str(), value.c_str(), 1) == 0;
}

inline int spawnProcessAndWait(const std::vector<std::string>& argv) {
    if (argv.empty()) {
        return -1;
    }

    std::vector<char*> rawArgs;
    rawArgs.reserve(argv.size() + 1);
    for (const std::string& arg : argv) {
        rawArgs.push_back(const_cast<char*>(arg.c_str()));
    }
    rawArgs.push_back(nullptr);

    pid_t processId = 0;
    int spawnResult = posix_spawnp(&processId, argv.front().c_str(), nullptr, nullptr, rawArgs.data(), environ);
    if (spawnResult != 0) {
        return -1;
    }

    int status = 0;
    if (waitpid(processId, &status, 0) < 0) {
        return -1;
    }

    if (WIFEXITED(status)) {
        return WEXITSTATUS(status);
    }

    if (WIFSIGNALED(status)) {
        return 128 + WTERMSIG(status);
    }

    return -1;
}

inline std::filesystem::path resolveHomeDir() {
    if (const char* home = std::getenv("HOME"); home != nullptr && *home != '\0') {
        return std::filesystem::path(home);
    }
    return {};
}

inline bool isMacOS() {
#ifdef __APPLE__
    return true;
#else
    return false;
#endif
}

}  // namespace apollo::platform
#include <filesystem>
#include <fstream>
#include <iostream>
#include <iterator>
#include <string>
#include <thread>
#include <chrono>
#include <vector>

#include "../platform_support.hpp"

namespace {
    std::filesystem::path findApolloBinary(const std::filesystem::path& installDir, const std::string& baseName) {
        std::filesystem::path candidate = installDir / baseName;
        return std::filesystem::exists(candidate) ? candidate : std::filesystem::path();
    }

    bool setExecutablePermissions(const std::filesystem::path& filePath) {
        std::error_code error;
        std::filesystem::permissions(
            filePath,
            std::filesystem::perms::owner_read |
                std::filesystem::perms::owner_write |
                std::filesystem::perms::owner_exec |
                std::filesystem::perms::group_read |
                std::filesystem::perms::group_exec |
                std::filesystem::perms::others_read |
                std::filesystem::perms::others_exec,
            std::filesystem::perm_options::add,
            error);
        return !error;
    }

    bool bootstrapDependencies(const std::filesystem::path& installDir) {
        std::filesystem::path scriptPath = installDir / "install-deps.sh";
        if (!std::filesystem::exists(scriptPath)) {
            std::cerr << "Install failed: missing dependency bootstrap script at " << scriptPath.string() << std::endl;
            return false;
        }

        std::filesystem::path bashPath = apollo::platform::getBashInterpreter();
        if (bashPath.empty()) {
            std::cerr << "Install failed: bash was not found on this Unix system." << std::endl;
            return false;
        }

        int exitCode = apollo::platform::spawnProcessAndWait({
            bashPath.string(),
            scriptPath.string(),
            "--install-dir",
            installDir.string()});
        if (exitCode != 0) {
            std::cerr << "Install failed while bootstrapping external dependencies. Exit code: " << exitCode << std::endl;
            return false;
        }

        return true;
    }

    bool ensureLinePresent(const std::filesystem::path& filePath, const std::string& line) {
        std::string contents;
        {
            std::ifstream in(filePath);
            if (in) {
                contents.assign((std::istreambuf_iterator<char>(in)), std::istreambuf_iterator<char>());
            }
        }

        if (contents.find(line) != std::string::npos) {
            return true;
        }

        std::ofstream out(filePath, std::ios::app);
        if (!out) {
            return false;
        }

        if (!contents.empty() && contents.back() != '\n') {
            out << std::endl;
        }
        out << line << std::endl;
        return static_cast<bool>(out);
    }

    bool writeUnixWrapper(const std::filesystem::path& wrapperPath, const std::string& targetLine) {
        std::ofstream wrapper(wrapperPath, std::ios::trunc);
        if (!wrapper.is_open()) {
            return false;
        }

        wrapper << "#!/usr/bin/env bash\n";
        wrapper << "set -e\n";
        wrapper << targetLine << "\n";
        wrapper.close();

        return static_cast<bool>(wrapper) && setExecutablePermissions(wrapperPath);
    }

    bool installUnixWrappers(const std::filesystem::path& installDir, std::string* wrapperDirPath) {
        std::filesystem::path homeDir = apollo::platform::resolveHomeDir();
        if (homeDir.empty()) {
            return false;
        }

        std::filesystem::path wrapperDir = homeDir / ".local" / "bin";
        std::error_code error;
        std::filesystem::create_directories(wrapperDir, error);
        if (error) {
            return false;
        }

        std::filesystem::path compilerScript = installDir / "compiler" / "exec.sh";
        if (!setExecutablePermissions(compilerScript)) {
            return false;
        }

        for (const std::filesystem::path& scriptPath : {
                 installDir / "install-deps.sh",
                 installDir / "compiler" / "cleanup-output.sh",
                 installDir / "compiler" / "exec.sh"}) {
            if (std::filesystem::exists(scriptPath) && !setExecutablePermissions(scriptPath)) {
                return false;
            }
        }

        for (const std::filesystem::path& binaryPath : {
                 installDir / "apollo",
                 installDir / "apollo-config",
                 installDir / "apollo_jit",
                 installDir / "install"}) {
            if (std::filesystem::exists(binaryPath) && !setExecutablePermissions(binaryPath)) {
                return false;
            }
        }

        std::filesystem::path apolloWrapper = wrapperDir / "apollo";
        if (!writeUnixWrapper(apolloWrapper, "exec \"" + compilerScript.string() + "\" \"$@\"")) {
            return false;
        }

        std::filesystem::path configBinary = findApolloBinary(installDir, "apollo-config");
        if (!configBinary.empty()) {
            std::filesystem::path configWrapper = wrapperDir / "apollo-config";
            if (!writeUnixWrapper(configWrapper, "exec \"" + configBinary.string() + "\" \"$@\"")) {
                return false;
            }
        }

        std::filesystem::path profilePath = apollo::platform::isMacOS()
            ? homeDir / ".zprofile"
            : homeDir / ".profile";
        if (!ensureLinePresent(profilePath, "export PATH=\"$HOME/.local/bin:$PATH\"")) {
            return false;
        }

        if (wrapperDirPath != nullptr) {
            *wrapperDirPath = wrapperDir.string();
        }

        return true;
    }
}

int main() {
    std::filesystem::path installDirPath = apollo::platform::getExecutableDir();
    std::filesystem::path compilerScriptPath = installDirPath / "compiler" / "exec.sh";
    std::filesystem::path configBinary = findApolloBinary(installDirPath, "apollo-config");

    if (!std::filesystem::exists(compilerScriptPath)) {
        std::cerr << "Install failed: missing compiler script at " << compilerScriptPath.string() << std::endl;
        return 1;
    }

    if (configBinary.empty()) {
        std::cerr << "Install warning: missing apollo-config binary in the install directory." << std::endl;
        std::cerr << "Apollo will still run in AOT mode, but mode switching will be unavailable until apollo-config is restored." << std::endl;
    }

    std::cout << "CHECKING DEPENDENCIES" << std::endl;
    if (!bootstrapDependencies(installDirPath)) {
        return 1;
    }

    std::cout << "PREPARING SHELL WRAPPERS" << std::endl;
    std::this_thread::sleep_for(std::chrono::milliseconds(999));
    std::cout << "LOADED INSTALLER. BEGINNING EXECUTION" << std::endl;
    std::this_thread::sleep_for(std::chrono::milliseconds(100));
    std::cout << "STATUS : [";
    for (int i = 0; i < 20; ++i) {
        std::cout << "#";
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }
    std::cout << "]" << std::endl;

    std::string wrapperDir;
    if (!installUnixWrappers(installDirPath, &wrapperDir)) {
        std::cerr << "Failed to create Apollo shell wrappers under ~/.local/bin." << std::endl;
        return 1;
    }

    std::cout << "Installation complete, Welcome to Apollo!" << std::endl;
    std::cout << "The apollo command is now installed through shell wrappers at " << wrapperDir << std::endl;
    std::cout << "Open a new shell session if your current PATH does not include ~/.local/bin yet." << std::endl;
    return 0;
}

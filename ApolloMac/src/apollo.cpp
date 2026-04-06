#include <filesystem>
#include <iostream>
#include <string>
#include <vector>

#include "../platform_support.hpp"

namespace {
    std::filesystem::path findCompilerScript(const std::filesystem::path& apolloDir) {
        std::filesystem::path shellScript = apolloDir / "compiler" / "exec.sh";
        if (std::filesystem::exists(shellScript)) {
            return shellScript;
        }

        return {};
    }

    std::vector<std::string> buildLaunchArguments(
        const std::filesystem::path& scriptPath,
        const std::string& command,
        const std::string& inputPath,
        const std::string& outputPath,
        bool binaryOnly) {
        std::vector<std::string> args;
        std::filesystem::path interpreter = apollo::platform::getBashInterpreter();
        args.push_back(interpreter.string());

        args.push_back(scriptPath.string());

        if (binaryOnly) {
            args.push_back("-bin");
        }

        args.push_back(command);

        if (!inputPath.empty()) {
            args.push_back(inputPath);
        }

        if (binaryOnly) {
            args.push_back(outputPath);
        }

        return args;
    }

    std::string normalizeOutputPath(const std::string& outputArgument) {
        std::filesystem::path outputPath = std::filesystem::absolute(outputArgument).lexically_normal();
        return outputPath.string();
    }
}

int main(int argc, char* argv[]) {
    if (argc < 2) {
        std::cerr << "Usage: apollo [-bin] <ctall|run> [filepath] [outputname]" << std::endl;
        return 1;
    }

    try {
        bool binaryOnly = false;
        int argIndex = 1;
        while (argIndex < argc) {
            std::string flag = argv[argIndex];
            if (flag == "-bin") {
                binaryOnly = true;
                ++argIndex;
                continue;
            }
            break;
        }

        if (argIndex >= argc) {
            std::cerr << "Usage: apollo [-bin] <ctall|run> [filepath] [outputname]" << std::endl;
            return 1;
        }

        std::string command = argv[argIndex++];
        std::filesystem::path apolloDir = apollo::platform::getExecutableDir();
        std::filesystem::path execScript = findCompilerScript(apolloDir);
        std::filesystem::path compilerDir = apolloDir / "compiler";

        if (execScript.empty()) {
            std::cerr << "Error: Cannot find compiler script under " << compilerDir.string() << std::endl;
            return 1;
        }

        if (!apollo::platform::setEnvironmentVariable("APOLLO_DIR", apolloDir.string()) ||
            !apollo::platform::setEnvironmentVariable("APOLLO_COMPILER_DIR", compilerDir.string())) {
            std::cerr << "Error: Failed to configure Apollo environment variables." << std::endl;
            return 1;
        }

        std::string inputPathStr;
        if (argIndex < argc) {
            inputPathStr = std::filesystem::absolute(argv[argIndex++]).lexically_normal().string();
        }

        std::string outputPathStr;
        if (binaryOnly) {
            if (argIndex >= argc) {
                std::cerr << "Usage: apollo -bin <ctall|run> [filepath] <outputname>" << std::endl;
                return 1;
            }

            outputPathStr = normalizeOutputPath(argv[argIndex++]);
        }

        if (argIndex < argc) {
            std::cerr << "Error: too many arguments." << std::endl;
            return 1;
        }

        std::vector<std::string> launchArgs = buildLaunchArguments(execScript, command, inputPathStr, outputPathStr, binaryOnly);
        int result = apollo::platform::spawnProcessAndWait(launchArgs);

        if (result < 0) {
            std::cerr << "Error: Failed to launch compiler script at: " << execScript.string() << std::endl;
            return 1;
        }

        return result;

    } catch (const std::exception& ex) {
        std::cerr << "Internal Error: " << ex.what() << std::endl;
        return 1;
    }
}

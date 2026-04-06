Welcome to the Apollo language!

This is a quick documentation on how to use it.


DEVELOPER NOTES (REMOVE BEFORE PUBLISH):
  INCLUDES ARE AT LINE ~2250


Compiler pipeline:
    `apollo ctall file.apollo` emits `output/output.ll` with the Unix `clang` from `compiler/toolchain-env.sh` first.
    In `aot` mode, Apollo lowers that IR to a native object file with `llc`, links it with `clang++`, and runs it.
    On Unix the default runnable binary is `output/output`.
    If generated code includes Boehm GC headers such as `extern gc/gc_cpp.h;`, Apollo injects `GC_INIT()` into `main` and links Boehm GC automatically.
    The mode is controlled by `apollo-config` in the repo root.
    Apollo `extern {package.name}` imports are resolved from the directory containing the entry `.apollo` file.
    Apollo cleans generated temp output after each run using `compiler/cleanup-output.sh`.
    If you need cross-target output, set `APOLLO_CLANG_TARGET`, and optionally `APOLLO_SYSROOT` or `APOLLO_GCC_TOOLCHAIN`, before running Apollo.

Source layout:
    Native binaries are built from `elf-build/apollo.cpp`, `elf-build/install.cpp`, `elf-build/config.cpp`, and `elf-build/apollo_jit.cpp`.

Execution mode commands:
    `apollo-config status` shows the current mode and JIT availability.
    `apollo-config aot` selects the LLVM IR -> object -> executable flow.
    `apollo-config jit` selects the LLVM JIT flow when an `apollo_jit` binary is present.
    `make` in the repo root builds `apollo`, `apollo-config`, `apollo_jit`, and `install`.

Binary output:
    `apollo -bin ctall file.apollo dist/program` does the same on Unix.
    `-bin` always uses the AOT path because JIT does not produce a standalone executable.

Install and launch:
    macOS: `install` bootstraps `llvm`, `openjdk@21`, and `bdw-gc` through Homebrew, writes `compiler/toolchain-env.sh`, installs `apollo` wrappers under `~/.local/bin`, and adds that directory to `~/.zprofile`.
    Linux: the Unix installer is best-effort and currently targets `apt-get`; it writes the same `compiler/toolchain-env.sh` and installs wrappers under `~/.local/bin`, appending that directory to `~/.profile`.
    Manual Unix fallback: run `bash compiler/exec.sh ctall file.apollo` from the Apollo repo.

Build from source on Unix:
    Run `make` in the repo root to build `apollo`, `apollo-config`, `apollo_jit`, and `install`.
    Run `make -C compiler` to build or regenerate the Java compiler side from within `compiler/`.

Important notes:
    Apollo is semicolon seperated
    Apollo requires the use of curly braces

Type aliases used by Apollo:

- `i32` for 32-bit integers
- `f64` for 64-bit floating point values
- `str` for strings

Container notes:

- `vector<T>` and `hsh<K,V>` support recursive nesting.
- `name[key]` works for lookup and assignment.
- `name[apnd] = value;` appends to a vector.
- If the target type is a nested container, composite literals like `<"key", 7>` are typed from context, so `tester["hello"] = <"world", 7>;` works for `hsh<str, hsh<str, int>>`.
- Composite lookup keys also work when the map key type is itself a container, for example `keyed[<"alpha", "beta">]` for `hsh<vector<str>, int>`.

Functions:

Functions are declared with a return type, name and paramaters with the contents parsed by semicolons.

returntype name(paramaters) {
    code to execute
}

A function with the name main and return type i32 is the entry point for the system. The return value is the status code of execution.
i32 main() {
    sys.println("Hello, world");
    return 0;
}

Here are the equivalents in other languages:

C++:
int main() {
    std::cout<<"Hello, world";
    return 0;
}

Java:
public class main {
    public static void main(String[] args) {
        System.out.println("Hello, world");
        //void return type
    }
}

Rust:
fn main() {
    println!("Hello, world");
}

Lambda functions:

Apollo supports inline lambdas with a simplified syntax that does not require an inner function name.

auto add = lmd-> (i32 left, i32 right) {
    return left + right;
}

If you want an explicit return type, you can write it directly after `lmd->`.

auto add = lmd-> i32(i32 left, i32 right) {
    return left + right;
}

Apollo also supports first-class function types using `fn<returnType(paramTypes...)>`.

fn<i32(i32, i32)> multiply = lmd-> (i32 left, i32 right) {
    return left * right;
}

sys.println(add(2, 3));
sys.println(multiply(3, 4));

Interfaces, inheritance, and instances:

Apollo supports interface declarations with `itr`, parent lists using `* public Parent; public OtherParent`, virtual methods in base classes, and `@Override` on derived methods. Both direct instance initialization and `.push(Type{})` instance population are supported.

itr Speaker {
    virtual void speak();
}

itr Walker {
    virtual void walk();
}

class Animal * public Speaker; public Walker {
    public virtual void speak() {
        sys.println("animal");
        return;
    }

    public virtual void walk() {
        sys.println("walk");
        return;
    }
}

class Dog * public Animal {
    @Override
    public void speak() {
        sys.println("dog");
        return;
    }

    @Override
    public void walk() {
        sys.println("walk");
        return;
    }
}

crt instance dog;
dog.push(Dog{});
dog.speak();

staticx instance direct = Dog{};
direct.walk();

Built in functions:

console output:
sys.println(arg);

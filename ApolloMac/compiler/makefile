.PHONY: ctall run compile cmprun regen runopt clean

SHELL := /bin/sh

ANTLR_JAR := antlr-4.13.2-complete.jar
CLASSES_DIR := output/classes
MAIN_JAVAC_CP := .:output/classes:$(ANTLR_JAR)
MAIN_JAVA_CP := output/classes:$(ANTLR_JAR)

ctall:
	bash ./exec.sh ctall main.apollo

run:
	bash ./exec.sh run main.apollo

compile: regen
	mkdir -p output/classes
	cd compiler-master && javac -d ../output/classes -cp ".:../$(ANTLR_JAR)" ./*.java

cmprun: compile
	javac -d $(CLASSES_DIR) -cp "$(MAIN_JAVAC_CP)" CppCodeGenVisitor.java Main.java
	java -cp "$(MAIN_JAVA_CP)" Main main.apollo

regen:
	@echo "Regenerating parser/visitor from compilerv1.g4 into compiler-master"
	java -jar $(ANTLR_JAR) -visitor -Dlanguage=Java -o compiler-master compilerv1.g4

runopt:
	bash ./exec.sh run main.apollo

clean:
	rm -rf output/classes compiler-master/*.class CppCodeGenVisitor.class Main.class Main$$1.class main_c.class
	bash ./cleanup-output.sh --manifest-path output/cleanup-manifest.txt --output-dir output >/dev/null 2>/dev/null || true

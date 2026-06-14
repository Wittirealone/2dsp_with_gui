.PHONY: run build clean

build:
	mkdir -p out
	javac -d out src/*.java

run: build
	java -cp out Main

clean:
	rm -rf out

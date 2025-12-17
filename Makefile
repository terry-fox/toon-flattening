.PHONY: build clean run-client run-server run-data test help

# Default target
help:
	@echo "ToonFlattening Minecraft Mod - Available Commands:"
	@echo ""
	@echo "  make build          Build the mod JAR"
	@echo "  make clean          Clean build artifacts"
	@echo "  make run-client     Run Minecraft client with mod"
	@echo "  make run-server     Run Minecraft server with mod"
	@echo "  make run-data       Run data generation"
	@echo "  make test           Run tests"
	@echo "  make clean-build    Clean and build"
	@echo "  make refresh        Refresh IDE (IntelliJ/Gradle)"
	@echo "  make help           Show this help message"

build:
	./gradlew build

clean:
	./gradlew clean

run-client:
	./gradlew runClient

run-server:
	./gradlew runServer

run-data:
	./gradlew runData

test:
	./gradlew test

clean-build: clean build

refresh:
	./gradlew cleanEclipse cleanIdea idea

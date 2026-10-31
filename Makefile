.PHONY: build

MAVEN ?= mvn
MODS_DIR ?= ../../mods

build:
	$(MAVEN) --batch-mode package
	@set -e; jar_name="$$($(MAVEN) --quiet --non-recursive help:evaluate -Dexpression=project.build.finalName -DforceStdout)"; \
		mkdir -p "$(MODS_DIR)"; \
		cp "target/$$jar_name.jar" "$(MODS_DIR)/"

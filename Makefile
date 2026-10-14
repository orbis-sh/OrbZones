.PHONY: build

MAVEN ?= mvn

build:
	$(MAVEN) --batch-mode package

.PHONY: build

MAVEN ?= mvn

build:
	powershell -NoProfile -ExecutionPolicy Bypass -File "..\build-mod.ps1" -MavenCommand "$(MAVEN)"

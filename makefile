SOURCE_DIR  := ./src/
OUTPUT_FILE := program
BUILD_DIR   := ./.bin/
ASSETS_DIR  := ./assets/

COLLECTION_DIRS := $(notdir $(patsubst %/,%,$(wildcard $(SOURCE_DIR)*/)))
COLLECTIONS := $(foreach d,$(COLLECTION_DIRS),-collection:$(d)=$(SOURCE_DIR)$(d))

ifeq ($(OS), Windows_NT)
	MKDIR  := mkdir
	RM     := rmdir /S /Q
	CP     := copy
	PATHSEP := \\
	EXTENSION := .exe
else
	MKDIR  := mkdir -p
	RM     := rm -rf
	CP     := cp
	PATHSEP := /
	EXTENSION := .out
endif

define fixpath
$(subst /,$(PATHSEP),$1)
endef

usage:
	@echo Available targets: build, run, dev, clean

build:
ifeq ($(OS), Windows_NT)
	@if not exist $(call fixpath,$(BUILD_DIR)) $(MKDIR) $(call fixpath,$(BUILD_DIR))
endif
	odin build $(call fixpath,$(SOURCE_DIR)) -debug -out:$(call fixpath,$(BUILD_DIR)$(OUTPUT_FILE)$(EXTENSION)) $(COLLECTIONS) -collection:assets=$(call fixpath,$(ASSETS_DIR))

run:
	$(call fixpath,$(BUILD_DIR)$(OUTPUT_FILE)$(EXTENSION))

dev: build run

clean:
	@$(RM) $(call fixpath,$(BUILD_DIR))

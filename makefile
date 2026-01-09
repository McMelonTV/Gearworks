SOURCE_DIR  := ./src/
OUTPUT_FILE := program
BUILD_DIR   := ./.bin/
ASSETS_DIR  := ./assets/

ifeq ($(OS), Windows_NT)
	COLLECTION_DIRS := $(notdir $(patsubst %/,%,$(wildcard $(SOURCE_DIR)*/)))
else 
	COLLECTION_DIRS := $(shell find $(SOURCE_DIR) -type d -maxdepth 1 -mindepth 1 -exec basename {} \;)
endif
COLLECTIONS := $(foreach d, $(COLLECTION_DIRS), -collection:$(d)=$(SOURCE_DIR)$(d))

ifeq ($(OS), Windows_NT)
	RM     := rmdir /S /Q
	PATHSEP := \\
	EXTENSION := .exe
else
	RM     := rm -rf
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
	@if not exist $(call fixpath, $(BUILD_DIR)) mkdir $(call fixpath, $(BUILD_DIR))
else
	@mkdir -p $(call fixpath, $(BUILD_DIR))
endif
	odin build $(call fixpath, $(SOURCE_DIR)) -debug -out:$(call fixpath,$(BUILD_DIR)$(OUTPUT_FILE)$(EXTENSION)) $(COLLECTIONS) -collection:assets=$(call fixpath,$(ASSETS_DIR))

run:
	$(call fixpath,$(BUILD_DIR)$(OUTPUT_FILE)$(EXTENSION))

dev: build run

clean:
	@$(RM) $(call fixpath,$(BUILD_DIR))

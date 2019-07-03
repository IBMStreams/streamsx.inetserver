.PHONY: build test test-full clean clean-all help scripthelp

SCRIPSPATH = $(abspath scripts)
INPUTDIR = tests
WORKDIR ?= workdir

define HELPTEXT =
The targets:
	build: expand the scripts from the install archive
	test: executes the quick test
	test-full: execute the full test
	clean: clean all test artifacts
	clean-all: clean all test and all scripts
	scripthelp: print testframework help command
	help: print this help
	
You can add further command line options to the test targets by adding a command line like:
make test CMD_ARGS=...
endef
build: scripts

scripts: testframeInstaller_*.sh
	@echo 'expand this $$< $<'
	pwd
	./$< $(SCRIPSPATH)

test: scripts
	./scripts/bin/runTTF --directory $(INPUTDIR) --workdir $(WORKDIR) --category quick $(CMD_ARGS)

test-full: scripts
	./scripts/bin/runTTF --directory $(INPUTDIR) --workdir $(WORKDIR) $(CMD_ARGS)

clean:
	rm -rf $(WORKDIR)

clean-all: clean
	rm -rf scripts

help:
	$(info $(HELPTEXT))
	@echo

scripthelp: scripts
	./scripts/bin/runTTF -h

version: scripts
	./scripts/bin/runTTF --version

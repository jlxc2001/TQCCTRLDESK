#!/usr/bin/env bash
set -euo pipefail

# Build macOS standalone packages for the current Mac architecture.
# Intel Mac builds x64 packages. Apple Silicon Mac builds arm64 packages.
./gradlew clean build packageMacAppImage packageMacDmg packageMacPkg --no-daemon --stacktrace

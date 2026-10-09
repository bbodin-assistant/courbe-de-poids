# Local Android build and instrumented-test shortcuts.
# Requirements: JDK 17, Android SDK, Gradle 8.9, and a running emulator
# or a USB-connected Android device with USB debugging enabled.

GRADLE ?= gradle
VERSION_NAME ?= 0.3-14
ADB ?= adb

.PHONY: help build test test-instrumented test-unit install clean devices

help:
	@echo "Available targets:"
	@echo "  make build              Build the debug APK"
	@echo "  make test               Run instrumented tests on a connected device/emulator"
	@echo "  make test-instrumented  Alias for 'make test'"
	@echo "  make devices            List connected Android devices/emulators"
	@echo "  make install            Build and install the debug APK"
	@echo "  make clean              Remove generated build files"
	@echo ""
	@echo "Override tools/version if needed: make test GRADLE=./gradlew VERSION_NAME=0.3-14"

build:
	$(GRADLE) assembleDebug -PVERSION_NAME=$(VERSION_NAME)

test: test-instrumented

test-instrumented:
	$(ADB) get-state
	$(GRADLE) connectedDebugAndroidTest -PVERSION_NAME=$(VERSION_NAME)

test-unit:
	$(GRADLE) testDebugUnitTest -PVERSION_NAME=$(VERSION_NAME)

devices:
	$(ADB) devices

install: build
	$(ADB) install -r app/build/outputs/apk/debug/app-debug.apk

clean:
	$(GRADLE) clean

# Local Android build, test, install, and release shortcuts.
# Requirements: JDK 17, Android SDK, Gradle 8.9, and a device/emulator for instrumented tests.

-include .env
.EXPORT_ALL_VARIABLES:

GRADLE ?= gradle
ADB ?= adb
VERSION_NAME ?= 0.3-14
ADB_SERIAL ?=

ADB_CMD = $(ADB) $(if $(strip $(ADB_SERIAL)),-s "$(ADB_SERIAL)",)

.PHONY: help build release test test-instrumented test-unit install clean devices

help:
	@echo "Available targets:"
	@echo "  make build              Build the debug APK"
	@echo "  make release            Build a signed release APK (requires signing values in .env)"
	@echo "  make test               Run instrumented tests on a connected device/emulator"
	@echo "  make test-unit          Run local JVM unit tests"
	@echo "  make devices            List connected Android devices/emulators"
	@echo "  make install            Build and install the debug APK"
	@echo "  make clean              Remove generated build files"
	@echo "  make help               Show this help"
	@echo ""
	@echo "Copy .env.example to .env and configure local tools, device, and signing values."

build:
	"$(GRADLE)" assembleDebug -PVERSION_NAME="$(VERSION_NAME)"

release:
	@test -n "$(RELEASE_KEYSTORE_PATH)" || (echo "ERROR: Set RELEASE_KEYSTORE_PATH in .env"; exit 1)
	@test -n "$(ANDROID_KEYSTORE_PASSWORD)" || (echo "ERROR: Set ANDROID_KEYSTORE_PASSWORD in .env"; exit 1)
	@test -n "$(ANDROID_KEY_ALIAS)" || (echo "ERROR: Set ANDROID_KEY_ALIAS in .env"; exit 1)
	@test -n "$(ANDROID_KEY_PASSWORD)" || (echo "ERROR: Set ANDROID_KEY_PASSWORD in .env"; exit 1)
	@test -f "$(RELEASE_KEYSTORE_PATH)" || (echo "ERROR: Keystore not found: $(RELEASE_KEYSTORE_PATH) (path is relative to app/ or absolute)"; exit 1)
	"$(GRADLE)" assembleRelease -PVERSION_NAME="$(VERSION_NAME)"
	@echo "Release APK: app/build/outputs/apk/release/app-release.apk"

test: test-instrumented

test-instrumented:
	$(ADB_CMD) get-state
	"$(GRADLE)" connectedDebugAndroidTest -PVERSION_NAME="$(VERSION_NAME)"

test-unit:
	"$(GRADLE)" testDebugUnitTest -PVERSION_NAME="$(VERSION_NAME)"

devices:
	$(ADB) devices

install: build
	$(ADB_CMD) install -r app/build/outputs/apk/debug/app-debug.apk

clean:
	"$(GRADLE)" clean

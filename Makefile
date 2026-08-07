# 行箸 · 构建辅助
# 用法：make help 查看全部命令

SHELL := /bin/bash

# JDK 17（AGP 要求）
JAVA_HOME ?= $(HOME)/Library/Java/JavaVirtualMachines/jdk-17.0.20.jdk/Contents/Home
GRADLE := JAVA_HOME="$(JAVA_HOME)" ./gradlew

# adb 路径（可用 make install ADB=你的adb 覆盖）
ADB ?= $(HOME)/Library/Android/sdk/platform-tools/adb

APK := app/build/outputs/apk/debug/app-debug.apk
RELEASE_APK := app/build/outputs/apk/release/app-release.apk

.PHONY: help build apk release install clean test deps update-check

help:
	@echo "行箸 · 构建命令："
	@echo ""
	@echo "  make build         构建 debug 安装包（可安装）"
	@echo "  make release       构建已签名的 release 安装包"
	@echo "  make install       安装 debug 包到已连接设备/模拟器"
	@echo "  make test          运行单元测试"
	@echo "  make clean         清理构建产物"
	@echo "  make deps          刷新依赖缓存并打印依赖树"
	@echo "  make update-check  检查依赖是否有新版本"

build:
	$(GRADLE) :app:assembleDebug

apk: build

release:
	$(GRADLE) :app:assembleRelease

install: build
	$(ADB) install -r $(APK)

clean:
	$(GRADLE) clean
	rm -rf app/build engine/build

test:
	$(GRADLE) :engine:test :app:testDebugUnitTest

deps:
	$(GRADLE) --refresh-dependencies :app:dependencies --configuration debugRuntimeClasspath

update-check:
	$(GRADLE) dependencyUpdates -Drevision=release

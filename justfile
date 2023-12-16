#!/usr/bin/env just --justfile

macosX64:
    ./gradlew macosX64TestBinaries

macosArm64:
    ./gradlew macosArm64TestBinaries

linuxX64:
    ./gradlew linuxX64TestBinaries

linuxArm64:
    ./gradlew linuxArm64TestBinaries

windowsX64:
    ./gradlew mingwX64TestBinaries

all: macosX64 macosArm64 linuxX64 linuxArm64 windowsX64

clean:
    ./gradlew clean

linuxX64Test: linuxX64
    -docker run -itd --name linuxX64Test \
        -v ./build/bin/linuxX64:/kommand \
        -v ./eko/target/x86_64-unknown-linux-gnu/release:/kommand/eko/target/release \
        -w /kommand \
        -e HTTP_PROXY=host.docker.internal:6152 \
        -e HTTPS_PROXY=host.docker.internal:6152 \
        -e ALL_PROXY=host.docker.internal:6153 \
        --platform linux/amd64 \
        -m 900m \
        --cpus=1 \
        azul/zulu-openjdk:11-latest \
        bash
    sleep 1
    -docker exec linuxX64Test ./debugTest/test.kexe
    docker rm -f linuxX64Test

linuxArm64Test: linuxArm64
    -docker run -itd --name linuxArm64Test \
        -v ./build/bin/linuxArm64:/kommand \
        -v ./eko/target/aarch64-unknown-linux-gnu/release:/kommand/eko/target/release \
        -w /kommand \
        -e HTTP_PROXY=host.docker.internal:6152 \
        -e HTTPS_PROXY=host.docker.internal:6152 \
        -e ALL_PROXY=host.docker.internal:6153 \
        --platform linux/arm64 \
        -m 900m \
        --cpus=1 \
        azul/zulu-openjdk:11-latest \
        bash
    sleep 1
    -docker exec linuxArm64Test ./debugTest/test.kexe
    docker rm -f linuxArm64Test

macosX64Test: macosX64
    ./gradlew macosX64Test

macosArm64Test: macosArm64
    ./gradlew macosArm64Test

windowsX64Test: windowsX64
    ./gradlew mingwX64Test
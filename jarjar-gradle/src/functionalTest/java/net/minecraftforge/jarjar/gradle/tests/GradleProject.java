/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.gradle.tests;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class GradleProject {
    protected final Path projectDir;
    protected final String buildFile;
    protected final String settingsFile;

    protected GradleProject(Path projectDir, String buildFile, String settingsFile) {
        this.projectDir = projectDir;
        this.buildFile = buildFile;
        this.settingsFile = settingsFile;
    }

    protected static final String BASIC_SETTINGS = """
        plugins {
          id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
        }

        rootProject.name = "test"
        """;

    protected void settingsFile() throws IOException {
        settingsFile(BASIC_SETTINGS);
    }

    protected void settingsFile(String content) throws IOException {
        writeFile(projectDir.resolve(settingsFile), content);
    }

    protected static final String BASIC_BUILD = """
        plugins {
          id("java")
          id("net.minecraftforge.jarjar")
        }

        repositories {
          mavenCentral();
        }
        """;

    protected void buildFile() throws IOException {
        buildFile(BASIC_BUILD);
    }

    protected void buildFile(int javaVersion) throws IOException {
        buildFile(BASIC_BUILD +
            "java.toolchain.languageVersion = JavaLanguageVersion.of(" + javaVersion + ")\n"
        );
    }

    protected void buildFile(String content) throws IOException {
        writeFile(projectDir.resolve(buildFile), content);
    }

    protected void writeFile(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    // Simple sanity test, making sure that the normal jar file can be built.
    protected abstract void simpleProject() throws IOException;

    // Simplest test, fully packaging a library
    protected abstract void simpleJarJardLibrary(String library) throws IOException;

    // Constraint only, don't package the library
    protected abstract void libraryConstraint(String library) throws IOException;
}

/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.gradle.tests;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import net.minecraftforge.jarjar.metadata.ContainedJarMetadata;
import net.minecraftforge.jarjar.metadata.Metadata;
import net.minecraftforge.jarjar.metadata.MetadataIOHandler;

import static org.junit.jupiter.api.Assertions.*;

public class FunctionalTests {
    private static final String GRADLE_VERSION = "9.0.0";
    private static final String JAR = ":jar";
    private static final String JARJAR = ":jarJar";
    private static final String METADATA = "META-INF/jarjar/metadata.json";

    @TempDir(cleanup = CleanupMode.ON_SUCCESS)
    private Path projectDir;

    @RegisterExtension
    AfterTestExecutionCallback afterTestExecutionCallback = this::after;

    private void after(ExtensionContext context) throws Exception {
        if (context.getExecutionException().isPresent())
            System.out.println(context.getDisplayName() + " Failed: " + projectDir);

    }

    private static final String BASIC_SETTINGS_GROOVY = """
        rootProject.name = 'test'
        """;

    private static final String BASIC_SETTINGS_KOTLIN = """
        rootProject.name = "test"
        """;

    private static final String BASIC_BUILD_GROOVY = """
        plugins {
          id 'java'
          id 'net.minecraftforge.jarjar'
        }

        repositories {
          mavenCentral();
        }
        """;

    private static final String BASIC_BUILD_KOTLIN = """
        plugins {
          id("java")
          id("net.minecraftforge.jarjar")
        }

        repositories {
          mavenCentral()
        }
        """;

    private void settingsFile(String content) throws IOException {
        writeFile(projectDir.resolve("settings.gradle"), content);
    }

    private void kotlinSettingsFile(String content) throws IOException {
        writeFile(projectDir.resolve("settings.gradle.kts"), content);
    }

    private void buildFile(String content) throws IOException {
        writeFile(projectDir.resolve("build.gradle"), content);
    }

    private void kotlinBuildFile(String content) throws IOException {
        writeFile(projectDir.resolve("build.gradle.kts"), content);
    }

    private void writeFile(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private BuildResult build(String... args) {
        return GradleRunner.create()
            .withGradleVersion(GRADLE_VERSION)
            .withProjectDir(projectDir.toFile())
            .withArguments(args)
            .withPluginClasspath()
            .build();
    }

    private static void assertTaskSuccess(BuildResult result, String task) {
        assertTaskOutcome(result, task, TaskOutcome.SUCCESS);
    }

    private static void assertTaskFailed(BuildResult result, String task) {
        assertTaskOutcome(result, task, TaskOutcome.FAILED);
    }

    private static void assertTaskOutcome(BuildResult result, String task, TaskOutcome expected) {
        var info = result.task(task);
        assertNotNull(info, "Could not find task `" + task + "` in build results");
        assertEquals(expected, info.getOutcome());
    }

    private static byte[] readJarEntry(Path path, String name) throws IOException {
        try (var fs = FileSystems.newFileSystem(path)) {
            var target = fs.getPath(name);
            assertTrue(Files.exists(target), "Archive " + path + " does not contain " + name);
            return Files.readAllBytes(target);
        }
    }

    @Test
    public void slimJarBuilds() throws IOException {
        settingsFile(BASIC_SETTINGS_GROOVY);
        buildFile(BASIC_BUILD_GROOVY);

        var results = build(JAR);
        assertTaskSuccess(results, JAR);
    }

    @Test
    public void slimJarBuildsKotlin() throws IOException {
        kotlinSettingsFile(BASIC_SETTINGS_KOTLIN);
        kotlinBuildFile(BASIC_BUILD_KOTLIN);

        var results = build(JAR);
        assertTaskSuccess(results, JAR);
    }

    @Test
    public void jarJarSimple() throws IOException {
        settingsFile(BASIC_SETTINGS_GROOVY);
        buildFile(BASIC_BUILD_GROOVY + """
            jarJar.register()

            dependencies {
                jarJar('org.apache.maven:maven-artifact:3.9.11') {
                    transitive = false
                    jarJar.configure(it)
                }
            }
            """);
        jarJarSimpleShared();
    }

    @Test
    public void jarJarSimpleKotlin() throws IOException {
        kotlinSettingsFile(BASIC_SETTINGS_KOTLIN);
        kotlinBuildFile(BASIC_BUILD_KOTLIN + """
            jarJar.register()

            dependencies {
                "jarJar"("org.apache.maven:maven-artifact:3.9.11") {
                    isTransitive = false
                    jarJar.configure(this)
                }
            }
            """);
        jarJarSimpleShared();
    }

    private void jarJarSimpleShared() throws IOException {
        var results = build(JARJAR);
        System.out.println(results.getOutput());
        assertTaskSuccess(results, JARJAR);
        var all = projectDir.resolve("build/libs/test-all.jar");
        assertTrue(Files.exists(all), "JarJar'd jar does not exist at: " + all);
        readJarEntry(all, "META-INF/jarjar/maven-artifact-3.9.11.jar");
        var meta = assertMetadataExists(all);
        var jar = assertHasSingleJar(meta, "org.apache.maven:maven-artifact");
        assertEquals("[3.9.11,)", jar.version().range().toString());
        assertEquals("3.9.11", jar.version().artifactVersion().toString());
    }

    private static Metadata assertMetadataExists(Path file) throws IOException {
        var data = readJarEntry(file, METADATA);
        var meta = MetadataIOHandler.fromStream(new ByteArrayInputStream(data)).orElse(null);
        assertNotNull(meta, "Invalid metadata file was generated: \n" + new String(data));
        return meta;
    }

    private static ContainedJarMetadata assertHasSingleJar(Metadata meta, String artifact) {
        assertEquals(1, meta.jars().size(), "Jars did not contain expected list");
        var jar = meta.jars().get(0);
        assertEquals(artifact, jar.identifier().group() + ':' + jar.identifier().artifact());
        return jar;
    }
}

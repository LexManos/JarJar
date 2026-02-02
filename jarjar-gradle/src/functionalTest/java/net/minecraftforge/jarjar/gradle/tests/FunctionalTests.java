/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.gradle.tests;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.junit.jupiter.api.Test;

import net.minecraftforge.jarjar.metadata.ContainedJarIdentifier;
import net.minecraftforge.jarjar.metadata.ContainedJarMetadata;
import net.minecraftforge.jarjar.metadata.ContainedVersion;
import net.minecraftforge.jarjar.metadata.Metadata;
import net.minecraftforge.jarjar.metadata.MetadataIOHandler;

import static org.junit.jupiter.api.Assertions.*;

public class FunctionalTests extends FunctionalTestsBase {
    private static final String JAR = ":jar";
    private static final String JARJAR = ":jarJar";
    private static final String METADATA = "META-INF/jarjar/metadata.json";

    public FunctionalTests() {
        super("9.0.0");
    }

    @Test
    public void slimJarBuilds() throws IOException {
        slimJarBuilds(new GroovyProject(projectDir));
    }

    @Test
    public void slimJarBuildsKotlin() throws IOException {
        slimJarBuilds(new KotlinProject(projectDir));
    }

    private void slimJarBuilds(GradleProject project) throws IOException {
        project.simpleProject();
        var results = build(JAR);
        assertTaskSuccess(results, JAR);
    }

    @Test
    public void jarJarSimple() throws IOException {
        jarJarSimple(new GroovyProject(projectDir));
    }

    @Test
    public void jarJarSimpleKotlin() throws IOException {
        jarJarSimple(new KotlinProject(projectDir));
    }

    private void jarJarSimple(GradleProject project) throws IOException {
        project.simpleJarJardLibrary("org.apache.maven:maven-artifact:3.9.11");

        var results = build(JARJAR);
        assertTaskSuccess(results, JARJAR);

        var expected = new Metadata(List.of(
            new ContainedJarMetadata(
                id("org.apache.maven", "maven-artifact"),
                version(rangeSpec("[3.9.11,)"), "3.9.11"),
                "META-INF/jarjar/maven-artifact-3.9.11.jar",
                false
            )
        ));

        var archive = projectDir.resolve("build/libs/test-all.jar");
        assertTrue(Files.exists(archive), "JarJar'd jar does not exist at: " + archive);
        readJarEntry(archive, expected.jars().get(0).path());
        var actual = assertMetadataExists(archive);
        assertMetadata(archive, expected, actual);
    }

    @Test
    public void libraryConstraint() throws IOException {
        libraryConstraint(new GroovyProject(projectDir));
    }

    @Test
    public void libraryConstraintKotlin() throws IOException {
        libraryConstraint(new KotlinProject(projectDir));
    }

    private void libraryConstraint(GradleProject project) throws IOException {
        project.libraryConstraint("org.apache.maven:maven-artifact:3.9.11");

        var results = build(JARJAR);
        assertTaskSuccess(results, JARJAR);

        var expected = new Metadata(List.of(
            new ContainedJarMetadata(
                id("org.apache.maven", "maven-artifact"),
                version(rangeSpec("[3.9.11,)"), "3.9.11"),
                // It's a constraint, but for legacy reasons we still add the path
                "META-INF/jarjar/maven-artifact-3.9.11.jar",
                false
            )
        ));

        var archive = projectDir.resolve("build/libs/test-all.jar");
        assertTrue(Files.exists(archive), "JarJar'd jar does not exist at: " + archive);
        // Make sure we don't actually ship the jar
        assertFileMissing(archive, expected.jars().get(0).path());
        var actual = assertMetadataExists(archive);
        assertMetadata(archive, expected, actual);
    }


    // ==========================================================
    //                          Helpers
    // ==========================================================
    protected static ContainedJarIdentifier id(String group, String artifact) {
        return new ContainedJarIdentifier(group, artifact);
    }
    protected static VersionRange rangeSpec(String spec) {
        try {
            return VersionRange.createFromVersionSpec(spec);
        } catch (InvalidVersionSpecificationException e) {
            throw new RuntimeException(e);
        }
    }
    protected static VersionRange range(String spec) {
        return VersionRange.createFromVersion(spec);
    }
    protected static ArtifactVersion version(String version) {
        return new DefaultArtifactVersion(version);
    }
    protected static ContainedVersion version(VersionRange range, String version) {
        return new ContainedVersion(range, version(version));
    }

    protected static Metadata assertMetadataExists(Path file) throws IOException {
        var data = readJarEntry(file, METADATA);
        var meta = MetadataIOHandler.fromStream(new ByteArrayInputStream(data)).orElse(null);
        assertNotNull(meta, "Invalid metadata file was generated: \n" + new String(data));
        return meta;
    }

    protected static void assertMetadata(Path archive, Metadata expected, Metadata actual) throws IOException {
        assertEquals(expected.jars().size(), actual.jars().size(), "Metadata did not have the correct number of jars.");
        for (var jar : expected.jars()) {
            ContainedJarMetadata ajar = null;
            for (var tmp : actual.jars()) {
                if (jar.identifier().equals(tmp.identifier())) {
                    ajar = tmp;
                    break;
                }
            }
            assertNotNull(ajar, "Could not find " + jar.identifier().group() + ':' + jar.identifier().artifact() + " in metadata");
            assertMetadata(archive, jar, ajar);
        }
    }

    protected static void assertMetadata(Path archive, ContainedJarMetadata expected, ContainedJarMetadata actual) throws IOException {
        assertEquals(expected.identifier(), actual.identifier());
        assertEquals(expected.path(), actual.path(), "Path");
        assertEquals(expected.isObfuscated(), actual.isObfuscated(), "isObfusicated");
        assertMetadata(expected.version(), actual.version());
    }

    protected static void assertMetadata(ContainedVersion expected, ContainedVersion actual) {
        if (expected == null) {
            assertNull(actual, "Expected null contained version, actual: " + actual);
        } else {
            assertNotNull(actual, "Expected non-null contained version");
            assertEquals(expected.range(), actual.range(), "Invalid Range");
            assertEquals(expected.artifactVersion(), actual.artifactVersion(), "Invalid ArtifactVersion");
        }
    }
}

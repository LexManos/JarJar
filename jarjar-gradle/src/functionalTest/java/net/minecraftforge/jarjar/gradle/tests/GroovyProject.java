/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.gradle.tests;

import java.io.IOException;
import java.nio.file.Path;

public class GroovyProject extends GradleProject {
    protected GroovyProject(Path projectDir) {
        super(projectDir, "build.gradle", "settings.gradle");
    }

    @Override
    protected void simpleProject() throws IOException {
        settingsFile();
        buildFile();
    }

    @Override
    protected void simpleJarJardLibrary(String library) throws IOException {
        settingsFile();
        buildFile(BASIC_BUILD + """
            jarJar.register()

            dependencies {
                jarJar('{library}') {
                    transitive = false
                    jarJar.configure(it)
                }
            }
            """.replace("{library}", library));
    }

    @Override
    protected void libraryConstraint(String library) throws IOException {
        settingsFile();
        buildFile(BASIC_BUILD + """
            jarJar.register()

            dependencies {
                jarJar('{library}') {
                    transitive = false
                    jarJar.configure(it) {
                        constraint = true
                    }
                }
            }
            """.replace("{library}", library));
    }
}

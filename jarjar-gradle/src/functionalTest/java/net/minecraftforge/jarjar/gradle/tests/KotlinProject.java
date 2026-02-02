/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.jarjar.gradle.tests;

import java.io.IOException;
import java.nio.file.Path;

public class KotlinProject extends GradleProject {
    protected KotlinProject(Path projectDir) {
        super(projectDir, "build.gradle.kts", "settings.gradle.kts");
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
                "jarJar"("{library}") {
                    isTransitive = false
                    jarJar.configure(this)
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
                "jarJar"("{library}") {
                    isTransitive = false
                    jarJar.configure(this) {
                        setConstraint(true)
                    }
                }
            }
            """.replace("{library}", library));
    }
}

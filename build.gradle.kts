subprojects {
    apply(plugin = "java")

    repositories {
        mavenCentral()
    }

    // Force all Jackson modules to the same version declared in libs.versions.toml.
    // Spring Boot 3.2.x BOM pins jackson-core to ~2.16.x, but jackson-databind 2.17.0
    // calls BufferRecycler.releaseToPool() which was added in jackson-core 2.17.0.
    // Without this force, the BOM wins and the runtime gets a mismatched jackson-core.
    configurations.all {
        resolutionStrategy.force(
            "com.fasterxml.jackson.core:jackson-core:2.17.0",
            "com.fasterxml.jackson.core:jackson-databind:2.17.0",
            "com.fasterxml.jackson.core:jackson-annotations:2.17.0",
        )
    }

    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.addAll(listOf("--release", "17"))
    }
}

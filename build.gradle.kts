plugins {
    java
    id("org.springframework.boot") version "3.2.4" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    id("com.google.protobuf") version "0.9.4" apply false
}

allprojects {
    group = "com.aetheris"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    dependencies {
        implementation("org.slf4j:slf4j-api:2.0.12")
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

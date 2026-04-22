plugins {
    id("com.google.protobuf")
}

dependencies {
    implementation("com.google.protobuf:protobuf-java:3.25.3")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.3"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                java {}
            }
        }
    }
}

sourceSets {
    main {
        proto {
            srcDir("../../proto")
        }
    }
}

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":backend:shared"))
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("com.google.protobuf:protobuf-java:3.25.3")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

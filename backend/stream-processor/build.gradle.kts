plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dep.mgmt)
    java
}


dependencies {
    implementation(project(":backend:shared"))
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.kafka)
    implementation(libs.postgresql.driver)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgres)
    implementation(libs.hibernate.spatial)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.parameter.names)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgres)
    testImplementation(libs.testcontainers.kafka)
}

tasks.test { useJUnitPlatform() }

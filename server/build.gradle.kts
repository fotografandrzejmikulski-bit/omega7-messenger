plugins {
    application
    java
}

group = "com.omega7"
version = "1.0.0"

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

dependencies {
    implementation("org.postgresql:postgresql:42.7.13")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.22.2")
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.1")
}

application { mainClass.set("com.omega7.server.Main") }
tasks.test { useJUnitPlatform() }

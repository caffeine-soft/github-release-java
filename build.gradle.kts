plugins {
    id("java")
    id("application")
}

group = "com.caffeinesoft.github.release"
version = "1.0.0"

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(25)) }
}

application {
    mainClass.set("com.caffeinesoft.github.release.Main")
}

repositories { mavenCentral() }

dependencies {
    implementation("org.json:json:20240303")
}

tasks.jar {
    archiveFileName.set("github-release-java.jar")
    manifest { attributes["Main-Class"] = "com.caffeinesoft.github.release.Main" }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({ configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) } })
}
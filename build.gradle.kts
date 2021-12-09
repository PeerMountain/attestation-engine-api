import com.google.protobuf.gradle.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.ajoberstar.grgit.Grgit

plugins {
    idea
    id("java-library")
    id("maven-publish")

    id("com.google.protobuf") version "0.8.17"
    kotlin("jvm") version "1.3.61"
    id("org.ajoberstar.grgit") version "4.0.1"
    id("com.github.node-gradle.node") version "3.1.0"
}

repositories {
    maven {
        url = uri("https://gitlab.amlapi.com/api/v4/projects/KYC3/packages/maven")
        name = "GitLab"
    }
}

description = "Protobuf Registry"
group = "com.kyc3"

val grgit = Grgit.open(mapOf("dir" to project.projectDir))
val commit = grgit.head().abbreviatedId
version = commit

val protoDir: File = sourceSets["main"].proto.srcDirs.first()

val protoVersion = File("$protoDir/prototool.yaml").useLines { lines ->
    lines.first { it.contains("version:") }.substringAfter(':').trim()
}

dependencies {
    api(platform("com.google.protobuf:protobuf-bom:$protoVersion"))
    api("com.google.protobuf:protobuf-java")
    api("com.google.protobuf:protobuf-java-util")

    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib"))

    val junitVersion = "5.6.0"
    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly(platform("org.junit:junit-bom:$junitVersion"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

java {
    withSourcesJar()
}

val protoJar by tasks.registering(Jar::class) {
    group = "build"
    description = "Assembles a JAR containing the Protobuf files."
    archiveClassifier.set("proto")

    from(sourceSets["main"].proto) {
        exclude("**/*.md", "**/*.yaml")
    }
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            artifactId = "oracle-definitions"
            from(components["java"])
            artifact(protoJar.get())
            suppressAllPomMetadataWarnings()
        }
    }
    repositories {
        maven {
            url = uri("https://gitlab.amlapi.com/api/v4/projects/57/packages/maven")
            credentials(HttpHeaderCredentials::class.java) {
                name = "Job-Token"
                value = System.getenv("CI_JOB_TOKEN")
            }
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
        }
    }
}

val DART_PROTO_PATH = extra.properties["dart.protoc.path"] as String? ?: "/root/.pub-cache/bin/protoc-gen-dart"

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protoVersion"
    }

    plugins {
        id("ts") {
            path = "./node_modules/ts-protoc-gen/bin/protoc-gen-ts"
        }
        id("dart") {
            path = DART_PROTO_PATH
        }
    }

    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("ts") {
                    outputSubDir = "ts"
                }
            }
            task.builtins {
                id("js") {
                    option("import_style=commonjs")
                    option("binary")
                }
            }
            task.plugins {
                id("dart") {
                    outputSubDir = "dart"
                }
            }
        }
    }
}

tasks {

    withType<KotlinCompile> {
        kotlinOptions {
            allWarningsAsErrors = true
            freeCompilerArgs = listOf("-progressive", "-Xassertions=jvm", "-Xjsr305=strict")
            jvmTarget = "1.8"
        }
    }

    jar {
        // Do not include documentation and our prototool configuration.
        exclude("**/*.md", "**/*.yaml")
    }

    javadoc {
        enabled = false
    }

    test {
        useJUnitPlatform()
    }

    register("generate") {
        group = "generation"
        description = "Runs all code generators defined for the repo"
    }
}
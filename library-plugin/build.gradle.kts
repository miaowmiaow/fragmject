@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("kotlin")
    `maven-publish`
}

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())

    implementation(libs.asm)
    implementation(libs.asm.commons)
    implementation(libs.asm.analysis)
    implementation(libs.asm.util)
    implementation(libs.asm.tree)
    implementation(libs.gradle) {
        exclude(group = "org.ow2.asm", module = "asm")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.example.miaow"
            artifactId = "plugin"
            version = "1.0.0"

            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri(rootProject.file("repo"))
        }
    }
}
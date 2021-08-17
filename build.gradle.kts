import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.1.4"
    id("org.jetbrains.kotlin.jvm") version "1.5.10"
}

group = "np.com.susanthapa"
version = "0.8.2"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains:marketplace-zip-signer:0.1.5")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    updateSinceUntilBuild.set(false)
    version.set("212.4746.92")
    plugins.set(listOf("gradle", "android"))
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    runIde {
        jbrVersion.set("11.0.11+9-b1504.5")
        ideDir.set(project.getLayout().getProjectDirectory().dir("/opt/android-studio-custom/"))
    }

    patchPluginXml {
        changeNotes.set("""
        Added feature to toggle both loaded and unloaded module at the same time
      """)
    }

    signPlugin {
        certificateChain.set(System.getenv("JETBRAIN_CERT_CHAIN"))
        privateKey.set(System.getenv("JETBRAIN_PRIVATE_KEY"))
        password.set(System.getenv("JETBRAIN_KEY_PASS"))
    }

    publishPlugin {
        token.set(System.getenv("JETBRAIN_PUBLISH_TOKEN"))
    }

    test {
        useJUnitPlatform()
    }
}
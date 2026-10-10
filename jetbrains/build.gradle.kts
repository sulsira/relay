import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    intellijPlatform {
        intellijIdea(providers.gradleProperty("platformVersion"))
        bundledPlugins(
            providers.gradleProperty("platformBundledPlugins").map { plugins ->
                plugins.split(",").map(String::trim).filter(String::isNotEmpty)
            },
        )
        testFramework(TestFrameworkType.Platform)
    }
}

intellijPlatform {
    buildSearchableOptions = false
    pluginConfiguration {
        id = "com.sulsira.relay"
        name = "Relay"
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = provider { null }
        }
    }
}

// The bundled Kubernetes plugin loads its frontend without its backend in the runIde sandbox
// and logs "No remote API found for KubernetesFloatingToolbarRemoteApi". Relay does not use it.
tasks.prepareSandbox {
    val disabledPlugins = listOf("com.intellij.kubernetes")
    doLast {
        val configDir = sandboxConfigDirectory.get().asFile
        configDir.mkdirs()
        configDir.resolve("disabled_plugins.txt").writeText(disabledPlugins.joinToString("\n", postfix = "\n"))
    }
}

# UniversalCraft

A full Java interop library that wraps Minecraft classes which allows you to write code for multiple versions at the same time. Built using ReplayMod's [Preprocessor](https://github.com/ReplayMod/preprocessor).


It also features a "standalone" edition, which can run GUIs without Minecraft so long as they only depend on
UniversalCraft and not Minecraft directly.
This can allow for a faster development loop (no need to wait a minute for Minecraft to start),
automated testing without having to bootstrap a full Minecraft environment,
and even development of completely standalone applications using the same toolkit (e.g. [Elementa]) as one is already
familiar with from Minecraft development.
See the `standalone/example/` folder for a fully functional example.

## Dependency

It's recommended that you include [Essential](link eventually) instead of adding it yourself.

In your repository block, add:

Groovy
```groovy
maven {
    url = "https://repo.essential.gg/repository/maven-public"
}
```
Kotlin
```kotlin
maven(url = "https://repo.essential.gg/repository/maven-public")
```

To use the latest builds, use the following dependency format, use the build reference to find the correct replacements:

<details><summary>Forge</summary>

```kotlin
implementation("gg.essential:universalcraft-$mcVersion-$mcPlatform:$buildNumber")
```
</details>
<details><summary>Fabric</summary>

Groovy
```groovy
modImplementation(include("gg.essential:universalcraft-$mcVersion-$mcPlatform:$buildNumber"))
```
Kotlin
```kotlin
modImplementation(include("gg.essential:universalcraft-$mcVersion-$mcPlatform:$buildNumber")!!)
```
</details>

### Build Reference
<!--
Script to generate the Build Reference table:
```bash
sed -n '/"1.8.9-forge"/,/)/p' settings.gradle.kts | sed '$d' | tr -d '", ' | tac | while read -r platform; do
    version=$(echo "$platform" | cut -d'-' -f1)
    loader=$(echo "$platform" | cut -d'-' -f2)
    echo "<tr><td>$version</td><td>$loader</td><td><img alt=\"$platform\" src=\"https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-$platform/maven-metadata.xml\"></td></tr>"
done
```
-->
<details><summary>Build Reference</summary>
    <table>
      <tbody>
        <tr>
          <th>mcVersion</th>
          <th>mcPlatform</th>
          <th>buildNumber</th>
        </tr>
        <tr>
          <td>standalone</td>
          <td>N/A</td>
          <td>
            <img alt="standalone" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-standalone/maven-metadata.xml">
          </td>
        </tr>
        <tr><td>26.2</td><td>fabric</td><td><img alt="26.2-fabric" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-26.2-fabric/maven-metadata.xml"></td></tr>
        <tr><td>26.1</td><td>fabric</td><td><img alt="26.1-fabric" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-26.1-fabric/maven-metadata.xml"></td></tr>
        <tr><td>1.21.11</td><td>fabric</td><td><img alt="1.21.11-fabric" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.21.11-fabric/maven-metadata.xml"></td></tr>
        <tr><td>1.21.9</td><td>fabric</td><td><img alt="1.21.9-fabric" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.21.9-fabric/maven-metadata.xml"></td></tr>
        <tr><td>1.21.7</td><td>neoforge</td><td><img alt="1.21.7-neoforge" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.21.7-neoforge/maven-metadata.xml"></td></tr>
        <tr><td>1.21.7</td><td>forge</td><td><img alt="1.21.7-forge" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.21.7-forge/maven-metadata.xml"></td></tr>
        <tr><td>1.21.7</td><td>fabric</td><td><img alt="1.21.7-fabric" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.21.7-fabric/maven-metadata.xml"></td></tr>
        <tr><td>1.21.6</td><td>fabric</td><td><img alt="1.21.6-fabric" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.21.6-fabric/maven-metadata.xml"></td></tr>
        <tr><td>1.21.5</td><td>neoforge</td><td><img alt="1.21.5-neoforge" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.21.5-neoforge/maven-metadata.xml"></td></tr>
        <tr><td>1.21.5</td><td>forge</td><td><img alt="1.21.5-forge" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.21.5-forge/maven-metadata.xml"></td></tr>
        <tr><td>1.21.5</td><td>fabric</td><td><img alt="1.21.5-fabric" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.21.5-fabric/maven-metadata.xml"></td></tr>
        <tr><td>1.21.4</td><td>neoforge</td><td><img alt="1.21.4-neoforge" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.21.4-neoforge/maven-metadata.xml"></td></tr>
        <tr><td>1.21.4</td><td>forge</td><td><img alt="1.21.4-forge" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.21.4-forge/maven-metadata.xml"></td></tr>
        <tr><td>1.21.4</td><td>fabric</td><td><img alt="1.21.4-fabric" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.21.4-fabric/maven-metadata.xml"></td></tr>
        <tr><td>1.21.3</td><td>neoforge</td><td><img alt="1.21.3-neoforge" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.21.3-neoforge/maven-metadata.xml"></td></tr>
        <tr><td>1.21.3</td><td>forge</td><td><img alt="1.21.3-forge" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.21.3-forge/maven-metadata.xml"></td></tr>
        <tr><td>1.21.3</td><td>fabric</td><td><img alt="1.21.3-fabric" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.21.3-fabric/maven-metadata.xml"></td></tr>
        <tr><td>1.21</td><td>neoforge</td><td><img alt="1.21-neoforge" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.21-neoforge/maven-metadata.xml"></td></tr>
        <tr><td>1.21</td><td>forge</td><td><img alt="1.21-forge" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.21-forge/maven-metadata.xml"></td></tr>
        <tr><td>1.21</td><td>fabric</td><td><img alt="1.21-fabric" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.21-fabric/maven-metadata.xml"></td></tr>
        <tr><td>1.20.6</td><td>neoforge</td><td><img alt="1.20.6-neoforge" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.20.6-neoforge/maven-metadata.xml"></td></tr>
        <tr><td>1.20.6</td><td>forge</td><td><img alt="1.20.6-forge" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.20.6-forge/maven-metadata.xml"></td></tr>
        <tr><td>1.20.6</td><td>fabric</td><td><img alt="1.20.6-fabric" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.20.6-fabric/maven-metadata.xml"></td></tr>
        <tr><td>1.20.4</td><td>neoforge</td><td><img alt="1.20.4-neoforge" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.20.4-neoforge/maven-metadata.xml"></td></tr>
        <tr><td>1.20.4</td><td>forge</td><td><img alt="1.20.4-forge" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.20.4-forge/maven-metadata.xml"></td></tr>
        <tr><td>1.20.4</td><td>fabric</td><td><img alt="1.20.4-fabric" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.20.4-fabric/maven-metadata.xml"></td></tr>
        <tr><td>1.20.2</td><td>forge</td><td><img alt="1.20.2-forge" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.20.2-forge/maven-metadata.xml"></td></tr>
        <tr><td>1.20.2</td><td>fabric</td><td><img alt="1.20.2-fabric" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.20.2-fabric/maven-metadata.xml"></td></tr>
        <tr><td>1.20.1</td><td>forge</td><td><img alt="1.20.1-forge" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.20.1-forge/maven-metadata.xml"></td></tr>
        <tr><td>1.20.1</td><td>fabric</td><td><img alt="1.20.1-fabric" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.20.1-fabric/maven-metadata.xml"></td></tr>
        <tr><td>1.20</td><td>fabric</td><td><img alt="1.20-fabric" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.20-fabric/maven-metadata.xml"></td></tr>
        <tr><td>1.19.4</td><td>forge</td><td><img alt="1.19.4-forge" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.19.4-forge/maven-metadata.xml"></td></tr>
        <tr><td>1.19.4</td><td>fabric</td><td><img alt="1.19.4-fabric" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.19.4-fabric/maven-metadata.xml"></td></tr>
        <tr><td>1.19.3</td><td>forge</td><td><img alt="1.19.3-forge" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.19.3-forge/maven-metadata.xml"></td></tr>
        <tr><td>1.19.3</td><td>fabric</td><td><img alt="1.19.3-fabric" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.19.3-fabric/maven-metadata.xml"></td></tr>
        <tr><td>1.19.2</td><td>forge</td><td><img alt="1.19.2-forge" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.19.2-forge/maven-metadata.xml"></td></tr>
        <tr><td>1.19.2</td><td>fabric</td><td><img alt="1.19.2-fabric" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.19.2-fabric/maven-metadata.xml"></td></tr>
        <tr><td>1.19.1</td><td>fabric</td><td><img alt="1.19.1-fabric" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.19.1-fabric/maven-metadata.xml"></td></tr>
        <tr><td>1.19</td><td>fabric</td><td><img alt="1.19-fabric" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.19-fabric/maven-metadata.xml"></td></tr>
        <tr><td>1.18.1</td><td>forge</td><td><img alt="1.18.1-forge" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.18.1-forge/maven-metadata.xml"></td></tr>
        <tr><td>1.18.1</td><td>fabric</td><td><img alt="1.18.1-fabric" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.18.1-fabric/maven-metadata.xml"></td></tr>
        <tr><td>1.17.1</td><td>forge</td><td><img alt="1.17.1-forge" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.17.1-forge/maven-metadata.xml"></td></tr>
        <tr><td>1.17.1</td><td>fabric</td><td><img alt="1.17.1-fabric" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.17.1-fabric/maven-metadata.xml"></td></tr>
        <tr><td>1.16.2</td><td>fabric</td><td><img alt="1.16.2-fabric" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.16.2-fabric/maven-metadata.xml"></td></tr>
        <tr><td>1.16.2</td><td>forge</td><td><img alt="1.16.2-forge" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.16.2-forge/maven-metadata.xml"></td></tr>
        <tr><td>1.12.2</td><td>forge</td><td><img alt="1.12.2-forge" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.12.2-forge/maven-metadata.xml"></td></tr>
        <tr><td>1.8.9</td><td>forge</td><td><img alt="1.8.9-forge" src="https://img.shields.io/badge/dynamic/xml?color=A97BFF&label=%20&query=/metadata/versioning/versions/version[not(contains(text(),'%2B'))][last()]&url=https://repo.essential.gg/repository/maven-releases/gg/essential/universalcraft-1.8.9-forge/maven-metadata.xml"></td></tr>
      </tbody>
    </table>

</details>

<h2><span style="font-size:3em; color:red;">IMPORTANT!</span></h2>

If you are using forge, you must also relocate UC to avoid potential crashes with other mods. To do this, you will need to use the Shadow Gradle plugin.

<details><summary>Groovy Version</summary>

You can do this by either putting it in your plugins block:
```groovy
plugins {
    id "com.github.johnrengelman.shadow" version "$version"
}
```
or by including it in your buildscript's classpath and applying it:
```groovy
buildscript {
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        classpath "gradle.plugin.com.github.jengelman.gradle.plugins:shadow:$version"
    }
}

apply plugin: "com.github.johnrengelman.shadow"
```
You'll then want to relocate UC to your own package to avoid breaking other mods
```groovy
shadowJar {
    archiveClassifier.set(null)
    relocate("gg.essential.universal", "your.package.universal")
}
tasks.named("reobfJar").configure { dependsOn(tasks.named("shadowJar")) }
```

</details>

<details><summary>Kotlin Script Version</summary>

You can do this by either putting it in your plugins block:
```kotlin
plugins {
    id("com.github.johnrengelman.shadow") version "$version"
}
```
or by including it in your buildscript's classpath and applying it:
```kotlin
buildscript {
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        classpath("gradle.plugin.com.github.jengelman.gradle.plugins:shadow:$version")
    }
}

apply(plugin = "com.github.johnrengelman.shadow")
```
You'll then want to relocate UC to your own package to avoid breaking other mods
```kotlin
tasks.shadowJar {
    archiveClassifier.set(null)
    relocate("gg.essential.universal", "your.package.universal")
}
tasks.reobfJar { dependsOn(tasks.shadowJar) }
```

</details>

[Elementa]: https://github.com/EssentialGG/Elementa

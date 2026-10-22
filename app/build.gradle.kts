import org.gradle.api.tasks.Copy
import java.security.MessageDigest
import java.util.Properties
import java.io.File
import java.net.URL
import java.net.HttpURLConnection
import java.util.zip.ZipInputStream

plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val signingProps = Properties().apply {
    val propsFile = rootProject.file("keystore.properties")
    if (propsFile.exists()) {
        propsFile.inputStream().use(::load)
    }
}

fun signingValue(name: String): String? {
    val envValue = System.getenv(name)?.takeIf { it.isNotBlank() }
    if (envValue != null) return envValue
    val gradleValue = project.findProperty(name) as String?
    if (!gradleValue.isNullOrBlank()) return gradleValue
    return signingProps.getProperty(name)?.takeIf { it.isNotBlank() }
}

fun stringProperty(name: String, fallback: String): String {
    return (project.findProperty(name) as String?)?.takeIf { it.isNotBlank() } ?: fallback
}

val generatedChangelogAssetsDir = layout.buildDirectory.dir("generated/changelogAssets")
val syncBundledChangelog by tasks.registering(Copy::class) {
    from(rootProject.file("CHANGELOG.md"))
    into(generatedChangelogAssetsDir.map { it.dir("changelog") })
}
val stableVersionCode = stringProperty("APP_VERSION_CODE", "1").toInt()
val stableVersionName = stringProperty("APP_VERSION_NAME", "0.1.0")
val nightlyVersionCode = stringProperty("NIGHTLY_VERSION_CODE", stableVersionCode.toString()).toInt()
val nightlyVersionName = stringProperty("NIGHTLY_VERSION_NAME", stableVersionName)
val requestedReleaseChannel = stringProperty(
    "APP_RELEASE_CHANNEL",
    if (gradle.startParameter.taskNames.any { it.contains("Nightly", ignoreCase = true) }) {
        "nightly"
    } else {
        "stable"
    },
)
val isNightlyBuild = requestedReleaseChannel.equals("nightly", ignoreCase = true)
val selectedVersionCode = if (isNightlyBuild) nightlyVersionCode else stableVersionCode
val selectedVersionName = if (isNightlyBuild) nightlyVersionName else stableVersionName
val androidxCoreVersion = "1.18.0"

configurations.configureEach {
    resolutionStrategy.force(
        "androidx.core:core:$androidxCoreVersion",
        "androidx.core:core-ktx:$androidxCoreVersion",
    )
}

android {
    namespace = "com.localdownloader"
    compileSdk = 36

    val internalDebugStoreFile = signingValue("INTERNAL_DEBUG_STORE_FILE")
    val internalDebugStorePassword = signingValue("INTERNAL_DEBUG_STORE_PASSWORD")
    val internalDebugKeyAlias = signingValue("INTERNAL_DEBUG_KEY_ALIAS")
    val internalDebugKeyPassword = signingValue("INTERNAL_DEBUG_KEY_PASSWORD")
    val hasInternalDebugSigning = listOf(
        internalDebugStoreFile,
        internalDebugStorePassword,
        internalDebugKeyAlias,
        internalDebugKeyPassword,
    ).all { !it.isNullOrBlank() }

    val releaseStoreFile = signingValue("RELEASE_STORE_FILE")
    val releaseStorePassword = signingValue("RELEASE_STORE_PASSWORD")
    val releaseKeyAlias = signingValue("RELEASE_KEY_ALIAS")
    val releaseKeyPassword = signingValue("RELEASE_KEY_PASSWORD")
    val hasReleaseSigning = listOf(
        releaseStoreFile,
        releaseStorePassword,
        releaseKeyAlias,
        releaseKeyPassword,
    ).all { !it.isNullOrBlank() }

    defaultConfig {
        applicationId = "com.localdownloader"
        minSdk = 26
        targetSdk = 36
        versionCode = selectedVersionCode
        versionName = selectedVersionName
        buildConfigField("String", "APP_RELEASE_CHANNEL", "\"stable\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    flavorDimensions += "distribution"

    productFlavors {
        create("standard") {
            dimension = "distribution"
            buildConfigField("boolean", "YTDLP_AUTO_UPDATE_DEFAULT", "true")
        }
        create("repoSafe") {
            dimension = "distribution"
            buildConfigField("boolean", "YTDLP_AUTO_UPDATE_DEFAULT", "false")
        }
    }

    signingConfigs {
        create("internalDebugStable") {
            if (hasInternalDebugSigning) {
                storeFile = file(requireNotNull(internalDebugStoreFile))
                storePassword = internalDebugStorePassword
                keyAlias = internalDebugKeyAlias
                keyPassword = internalDebugKeyPassword
            }
        }
        create("releaseStable") {
            if (hasReleaseSigning) {
                storeFile = file(requireNotNull(releaseStoreFile))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("releaseStable")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            if (hasInternalDebugSigning) {
                signingConfig = signingConfigs.getByName("internalDebugStable")
            }
        }
        create("nightly") {
            initWith(getByName("debug"))
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            applicationIdSuffix = ".nightly"
            matchingFallbacks += listOf("debug")
            resValue("string", "app_name", "Nightly - $nightlyVersionName")
            buildConfigField("String", "APP_RELEASE_CHANNEL", "\"nightly\"")
            if (hasInternalDebugSigning) {
                signingConfig = signingConfigs.getByName("internalDebugStable")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols.add("**/libpython.zip.so")
            keepDebugSymbols.add("**/libffmpeg.zip.so")
        }
    }

    sourceSets.getByName("main").assets.directories.add(
        generatedChangelogAssetsDir.get().asFile.absolutePath,
    )
}

// yt-dlp is pinned to an explicit release and verified against that release's SHA2-256SUMS file.
// The CA bundle is verified against the SHA-256 that curl.se publishes next to it.
val pinnedYtDlpVersion = "2026.10.21"

fun sha256Hex(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { b -> "%02x".format(b) }
}

fun fetchText(url: String): String = URL(url).openStream().use { it.readBytes().toString(Charsets.UTF_8) }

fun fetchTo(url: String, target: File) {
    URL(url).openStream().use { input -> target.outputStream().use { input.copyTo(it) } }
}

val downloadYtDlpTask by tasks.registering {
    val outputDir = file("src/main/assets/ytdlp")
    val targetScript = File(outputDir, "yt-dlp")
    val targetCert = File(outputDir, "cert.pem")

    inputs.property("ytDlpVersion", pinnedYtDlpVersion)
    outputs.files(targetScript, targetCert)

    doLast {
        outputDir.mkdirs()
        val versionMarker = File(outputDir, ".yt-dlp-version")
        if (!targetScript.exists() || versionMarker.takeIf { it.exists() }?.readText()?.trim() != pinnedYtDlpVersion) {
            val base = "https://github.com/yt-dlp/yt-dlp/releases/download/$pinnedYtDlpVersion"
            val sums = fetchText("$base/SHA2-256SUMS")
            val expected = sums.lineSequence()
                .map { it.trim().split(Regex("\\s+")) }
                .firstOrNull { it.size >= 2 && it[1].removePrefix("*") == "yt-dlp" }
                ?.get(0)
                ?: throw GradleException("SHA2-256SUMS for yt-dlp $pinnedYtDlpVersion has no entry for 'yt-dlp'")
            println("Downloading yt-dlp $pinnedYtDlpVersion ...")
            fetchTo("$base/yt-dlp", targetScript)
            val actual = sha256Hex(targetScript)
            if (!actual.equals(expected, ignoreCase = true)) {
                targetScript.delete()
                throw GradleException("yt-dlp checksum mismatch: expected $expected but got $actual")
            }
            versionMarker.writeText(pinnedYtDlpVersion)
            println("yt-dlp $pinnedYtDlpVersion verified (sha256 $actual)")
        }
        if (!targetCert.exists()) {
            fetchTo("https://curl.se/ca/cacert.pem", targetCert)
            val expectedCert = fetchText("https://curl.se/ca/cacert.pem.sha256").trim().split(Regex("\\s+"))[0]
            val actualCert = sha256Hex(targetCert)
            if (!actualCert.equals(expectedCert, ignoreCase = true)) {
                targetCert.delete()
                throw GradleException("cacert.pem checksum mismatch: expected $expectedCert but got $actualCert")
            }
            println("cacert.pem verified (sha256 $actualCert)")
        }
    }
}

tasks.named("preBuild") {
    dependsOn(syncBundledChangelog)
    dependsOn(downloadYtDlpTask)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.05.00")

    implementation("androidx.core:core-ktx:$androidxCoreVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("androidx.media:media:1.8.0")
    implementation("androidx.media3:media3-exoplayer:1.10.1")
    implementation("androidx.media3:media3-ui:1.10.1")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")
    implementation("io.coil-kt:coil-svg:2.7.0")
    implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1")
    implementation("org.tukaani:xz:1.12")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    implementation("androidx.hilt:hilt-work:1.3.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")

    implementation("com.google.dagger:hilt-android:2.59.2")
    ksp("com.google.dagger:hilt-compiler:2.59.2")
    ksp("androidx.hilt:hilt-compiler:1.3.0")

    implementation("androidx.datastore:datastore-preferences:1.2.1")

    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.3.21")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.room:room-testing:2.8.4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

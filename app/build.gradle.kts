import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.ktlint)
}

ktlint {
  reporters {
    reporter(ReporterType.PLAIN)
  }
  additionalEditorconfig.set(
    mapOf("indent_size" to "2"),
  )
}

// Версия из тега: приоритет -PreleaseTag (из CI, напр. v1.0.0), иначе последний локальный тег.
// Снимаем ведущую "v". Фолбэк "1.0.0", если git недоступен (чистая распаковка без .git).
fun latestGitTag(): String? =
  try {
    val p =
      ProcessBuilder("git", "describe", "--tags", "--abbrev=0")
        .directory(projectDir)
        .redirectErrorStream(true)
        .start()
    val out = p.inputStream.bufferedReader().readText().trim()
    if (p.waitFor() == 0 && out.isNotEmpty()) out else null
  } catch (e: Exception) {
    null
  }

val rawTag =
  providers.gradleProperty("releaseTag").orNull?.takeIf { it.isNotBlank() }
    ?: latestGitTag()
    ?: "1.0.0"
val parts =
  rawTag.removePrefix("v")
    .split(".")
    .map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
val major = parts.getOrElse(0) { 0 }
val minor = parts.getOrElse(1) { 0 }
val patch = parts.getOrElse(2) { 0 }
val appVersionName = "$major.$minor.$patch"
val appVersionCode = major * 1_000_000 + minor * 1_000 + patch

// Build-time WebDAV defaults injected from CI environment variables (GitHub Actions secrets).
// Empty when unset, so local builds and unconfigured CI keep Save-in-default-mode a no-op.
private fun javaString(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

private fun webdavDefault(name: String): String = javaString(providers.environmentVariable(name).getOrElse(""))

// version.txt sits on the same WebDAV as profile storage, one path below the configured base URL.
private fun webdavVersionTxtUrl(): String {
  val base = providers.environmentVariable("WEBDAV_DEFAULT_BASE_URL").getOrElse("").trim()
  return javaString(if (base.isBlank()) "" else base.removeSuffix("/").plus("/version.txt"))
}

private fun webdavProfilesTxtUrl(): String {
  val base = providers.environmentVariable("WEBDAV_DEFAULT_BASE_URL").getOrElse("").trim()
  return javaString(if (base.isBlank()) "" else base.removeSuffix("/").plus("/profiles.txt"))
}

android {
  namespace = "com.thindie.rknzbl"
  compileSdk {
    version = release(36)
  }

  defaultConfig {
    applicationId = "com.thindie.rknzbl"
    minSdk = 24
    targetSdk = 36
    versionCode = appVersionCode
    versionName = appVersionName
    vectorDrawables.useSupportLibrary = true
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    buildConfigField("String", "WEBDAV_DEFAULT_BASE_URL", webdavProfilesTxtUrl())
    buildConfigField("String", "WEBDAV_DEFAULT_USERNAME", webdavDefault("WEBDAV_DEFAULT_USERNAME"))
    buildConfigField("String", "WEBDAV_DEFAULT_PASSWORD", webdavDefault("WEBDAV_DEFAULT_PASSWORD"))
    buildConfigField("String", "WEBDAV_VERSION_TXT_URL", webdavVersionTxtUrl())
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlin {
    compilerOptions {
      jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }

  packaging {
    jniLibs {
      useLegacyPackaging = true
    }
  }
}

dependencies {
  testImplementation("junit:junit:4.13.2")
  testImplementation("io.mockk:mockk:1.13.10")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
  testImplementation(platform(libs.androidx.compose.bom))
  testImplementation("androidx.compose.ui:ui-test-junit4")
  testImplementation("androidx.compose.ui:ui-test-android")
  testImplementation("org.jetbrains.kotlin:kotlin-test")

  implementation(project(":v2ray-engine"))
  implementation(project(":core"))
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.appcompat)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  debugImplementation(libs.androidx.compose.ui.tooling)

  implementation(libs.ktor.client.core)
  implementation(libs.ktor.client.cio)
  implementation(libs.ktor.client.auth)
  implementation(libs.androidx.work.runtime.ktx)
}

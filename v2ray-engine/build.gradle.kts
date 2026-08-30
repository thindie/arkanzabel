import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
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

android {
  namespace = "com.thindie.rknzbl.v2rayengine"
  compileSdk = 36

  defaultConfig {
    minSdk = 24
    consumerProguardFiles("consumer-rules.pro")
  }

  sourceSets {
    getByName("main") {
      // Same layout as typical v2rayNG drops: ABI folders + AAR/JAR alongside.
      jniLibs.srcDir("libs")
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
      )
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  packaging {
    jniLibs {
      useLegacyPackaging = true
    }
  }
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  // Drop `libv2ray` AAR (and optional JARs) into `v2ray-engine/libs/` — see `libs/README.md`.
  implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.tencent.mmkv)
  implementation(libs.google.gson)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.androidx.work.multiprocess)

  testImplementation("junit:junit:4.13.2")
  testImplementation("io.mockk:mockk:1.13.10")
  testImplementation("org.jetbrains.kotlin:kotlin-test")
}

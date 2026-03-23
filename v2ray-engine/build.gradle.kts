plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
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
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }

  packaging {
    jniLibs {
      useLegacyPackaging = true
    }
  }
}

kotlin {
  jvmToolchain(21)
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
  implementation(libs.square.okhttp)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.androidx.work.multiprocess)
}

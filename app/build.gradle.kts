import java.util.Base64

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

// Helper function to read secrets from system environment or .env file
fun getSecret(key: String, defaultValue: String = ""): String {
  val envVal = System.getenv(key)
  if (!envVal.isNullOrBlank()) return envVal

  val envFile = file("${rootDir}/.env")
  if (envFile.exists()) {
    try {
      val lines = envFile.readLines()
      for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("#") || !trimmed.contains("=")) continue
        val parts = trimmed.split("=", limit = 2)
        if (parts.size == 2 && parts[0].trim() == key) {
          return parts[1].trim().removeSurrounding("\"").removeSurrounding("'")
        }
      }
    } catch (e: Exception) {
      // Ignore
    }
  }
  return defaultValue
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.valiorsw.p5040"
    minSdk = 24
    targetSdk = 36
    versionCode = 4
    versionName = "1.1.2"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keyBase64 = System.getenv("KEY") ?: getSecret("KEY")
      val keyAliasVal = System.getenv("KEY_ALIAS") ?: getSecret("KEY_ALIAS")
      val keyPwVal = System.getenv("KEY_PW") ?: getSecret("KEY_PW")
      val keyPw2Val = System.getenv("KEY_PW2") ?: getSecret("KEY_PW2")

      if (!keyBase64.isNullOrBlank() && !keyAliasVal.isNullOrBlank()) {
        val keystoreFile = file("${rootDir}/release.keystore")
        try {
          val decoded = Base64.getDecoder().decode(keyBase64.trim().replace("\\s".toRegex(), ""))
          keystoreFile.writeBytes(decoded)
        } catch (ex: Exception) {
          throw GradleException("Failed to decode KEY base64: ${ex.message}")
        }
        storeFile = keystoreFile
        storePassword = if (!keyPw2Val.isNullOrBlank()) keyPw2Val else keyPwVal
        keyAlias = keyAliasVal
        keyPassword = keyPwVal
      } else {
        val keystorePath = getSecret("KEYSTORE_PATH", "${rootDir}/debug.keystore")
        val keystoreFile = file(keystorePath)
        val storePasswordVal = getSecret("STORE_PASSWORD", "android")
        val keyAliasValLocal = getSecret("KEY_ALIAS", "androiddebugkey")
        val keyPasswordVal = getSecret("KEY_PASSWORD", "android")

        if (keystoreFile.exists()) {
          storeFile = keystoreFile
          storePassword = storePasswordVal
          keyAlias = keyAliasValLocal
          keyPassword = keyPasswordVal
        } else {
          storeFile = file("${rootDir}/debug.keystore")
          storePassword = "android"
          keyAlias = "androiddebugkey"
          keyPassword = "android"
        }
      }
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  // implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.security.crypto)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

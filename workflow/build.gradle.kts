plugins {
  id(Plugins.BuildPlugins.androidLib)
  id(Plugins.BuildPlugins.kotlinAndroid)
  id(Plugins.BuildPlugins.mavenPublish)
  jacoco
}

afterEvaluate {
  publishing {
    publications {
      register("release", MavenPublication::class) {
        from(components["release"])
        artifactId = "engine"
        groupId = "com.google.android.fhir.workflow"
        version = "0.1.0-alpha01"
        // Also publish source code for developers' convenience
        artifact(
          tasks.create<Jar>("androidSourcesJar") {
            archiveClassifier.set("sources")
            from(android.sourceSets.getByName("main").java.srcDirs)
          }
        )
        pom {
          name.set("Android FHIR Workflow Library")
          licenses {
            license {
              name.set("The Apache License, Version 2.0")
              url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
          }
        }
      }
    }
  }
}

createJacocoTestReportTask()

android {
  compileSdk = Sdk.compileSdk
  buildToolsVersion = Plugins.Versions.buildTools

  defaultConfig {
    minSdk = Sdk.minSdk
    targetSdk = Sdk.targetSdk
    testInstrumentationRunner = Dependencies.androidJunitRunner
    // Need to specify this to prevent junit runner from going deep into our dependencies
    testInstrumentationRunnerArguments["package"] = "com.google.android.fhir.workflow"
    // Required when setting minSdkVersion to 20 or lower
    // See https://developer.android.com/studio/write/java8-support
    multiDexEnabled = true
  }

  sourceSets { getByName("test").apply { resources.setSrcDirs(listOf("testdata")) } }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
    }
  }

  compileOptions {
    // Flag to enable support for the new language APIs
    // See https://developer.android.com/studio/write/java8-support
    isCoreLibraryDesugaringEnabled = true
    // Sets Java compatibility to Java 8
    // See https://developer.android.com/studio/write/java8-support
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  packagingOptions {
    resources.excludes.addAll(
      listOf(
        "license.html",
        "META-INF/ASL-2.0.txt",
        "META-INF/sun-jaxb.episode",
        "META-INF/DEPENDENCIES",
        "META-INF/LGPL-3.0.txt",
        "readme.html"
      )
    )
  }

  // See https://developer.android.com/studio/write/java8-support
  kotlinOptions { jvmTarget = JavaVersion.VERSION_1_8.toString() }

  configureJacocoTestOptions()
}

configurations {
  all {
    exclude(module = "json")
    exclude(module = "xpp3")
    exclude(module = "hamcrest-all")
    exclude(module = "javax.activation")
  }
}

dependencies {
  androidTestImplementation(Dependencies.AndroidxTest.core)
  androidTestImplementation(Dependencies.AndroidxTest.extJunit)
  androidTestImplementation(Dependencies.AndroidxTest.extJunitKtx)
  androidTestImplementation(Dependencies.AndroidxTest.runner)
  androidTestImplementation(Dependencies.AndroidxTest.workTestingRuntimeKtx)
  androidTestImplementation(Dependencies.junit)
  androidTestImplementation(Dependencies.truth)

  api(Dependencies.HapiFhir.structuresR4) { exclude(module = "junit") }

  coreLibraryDesugaring(Dependencies.desugarJdkLibs)

  implementation(Dependencies.Kotlin.androidxCoreKtx)
  implementation(Dependencies.Kotlin.kotlinCoroutinesAndroid)
  implementation(Dependencies.Kotlin.kotlinCoroutinesCore)
  implementation(Dependencies.Kotlin.stdlib)
  implementation("com.github.java-json-tools:msg-simple:1.2")
  implementation("com.github.java-json-tools:jackson-coreutils:2.0")
  implementation("com.fasterxml.jackson.core:jackson-annotations:2.12.2")
  implementation("com.fasterxml.jackson.core:jackson-core:2.12.2")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.12.2")
  implementation("javax.xml.stream:stax-api:1.0-2")
  implementation("org.codehaus.woodstox:woodstox-core-asl:4.4.1")
  implementation("org.opencds.cqf.cql:evaluator:1.3.1-SNAPSHOT")
  implementation("org.opencds.cqf.cql:evaluator.builder:1.3.1-SNAPSHOT")
  implementation("org.opencds.cqf.cql:evaluator.dagger:1.3.1-SNAPSHOT")
  implementation("xerces:xercesImpl:2.11.0")
  implementation(project(":engine"))

  testImplementation(Dependencies.AndroidxTest.core)
  testImplementation(Dependencies.junit)
  testImplementation(Dependencies.robolectric)
  testImplementation(Dependencies.truth)
}

// dependencies {
//
//
// }

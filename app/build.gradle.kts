import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// 서명 키는 저장소 밖에 둔다. 경로와 비밀번호는 git이 무시하는 keystore.properties에 있다.
val keystore = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

android {
    namespace = "com.break8t.unknown"
    compileSdk = 36
    // CarPropertyManager 컴파일용 stub. 런타임은 기기의 car framework를 쓴다.
    useLibrary("android.car")

    defaultConfig {
        applicationId = "com.break8t.unknown"
        minSdk = 28
        // Pleos Connect Emulator 2.1.0 = API 34
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
    }
    signingConfigs {
        if (keystore.isNotEmpty()) create("release") {
            storeFile = file(keystore.getProperty("storeFile"))
            storePassword = keystore.getProperty("storePassword")
            keyAlias = keystore.getProperty("keyAlias")
            keyPassword = keystore.getProperty("keyPassword")
        }
    }
    buildTypes {
        release {
            // R8을 켜면 Pleos SDK와 car framework의 리플렉션 호출이 깨질 수 있어서 끈다.
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

/** 제출용 파일을 저장소의 apk/ 아래에 정해진 이름으로 만든다. `./gradlew submitApk` */
tasks.register<Copy>("submitApk") {
    dependsOn("assembleRelease")
    doFirst { check(keystore.isNotEmpty()) { "keystore.properties가 없어 release 서명을 할 수 없습니다" } }
    from(layout.buildDirectory.file("outputs/apk/release/app-release.apk"))
    into(rootProject.layout.projectDirectory.dir("apk"))
    rename { "break가고장난8톤트럭_정체불명_release.apk" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.08.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.3")
    implementation("ai.pleos.playground:Vehicle:2.3.2")
    // Vehicle이 runtime 스코프로만 노출해서 VehicleGear 등을 쓰려면 직접 넣어야 한다.
    implementation("ai.pleos.playground:vehicle-data:1.1.11")
    // 정체 레이드(경로 위 정체 목록)와 필드 드랍 경유지
    implementation("ai.pleos.playground:NaviHelper:3.0.5")
    implementation("ai.pleos.playground:navi-data:3.0.4")
    // 필드 드랍 지도. 에뮬레이터에서 OpenFreeMap 타일로 도로와 한글 지명이 뜨는 것을 확인했다.
    implementation("org.maplibre.gl:android-sdk:13.6.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

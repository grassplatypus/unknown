plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

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

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

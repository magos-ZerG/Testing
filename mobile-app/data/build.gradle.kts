import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.w3c.dom.Element
import java.math.RoundingMode
import javax.xml.parsers.DocumentBuilderFactory
import java.io.StringReader
import org.xml.sax.InputSource

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    jacoco
}

android {
    namespace = "com.z23u184.studymate.data"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(project(":domain"))

    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.monitor)
    implementation(libs.androidx.junit.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.koin.core)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit2.kotlinx.serialization.converter)

    implementation(libs.koin.android)

    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("io.mockk:mockk:1.14.11")
    testImplementation(platform("io.qameta.allure:allure-bom:2.35.5"))
    testImplementation("io.qameta.allure:allure-junit4")
    testImplementation(libs.kotlinx.coroutines.test)


    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    implementation(kotlin("test"))
}

jacoco {
    toolVersion = "0.8.13"
}

val allureResultsDir = layout.buildDirectory.dir("allure-results")

tasks.withType<Test>().configureEach {
    maxParallelForks = 1
    forkEvery = 0L
    systemProperty("allure.results.directory", allureResultsDir.get().asFile.absolutePath)

    System.getProperty("lab.random.seed")?.let { seed ->
        systemProperty("lab.random.seed", seed)
    }

    testLogging {
        events("passed", "failed", "skipped")
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

tasks.withType<Test>().configureEach {
    if (name == "testDebugUnitTest") {
        description = "Runs exactly 10 Lab 1 tests: 5 classical + 5 London-style."
        doFirst {
            delete(allureResultsDir)
        }
    }
}

val labCoverage = tasks.register<JacocoReport>("labCoverage") {
    group = "verification"
    description =
        "Runs all 10 Lab 1 tests and generates line + branch coverage for app + data + domain."

    dependsOn(
        "testDebugUnitTest",
        ":app:compileDebugKotlin",
        ":data:compileDebugKotlin",
        ":domain:classes",
    )

    executionData(
        fileTree(layout.buildDirectory) {
            include("jacoco/testDebugUnitTest.exec")
            include("**/testDebugUnitTest.exec")
            include("outputs/unit_test_code_coverage/**/*.exec")
        }
    )

    val appProject = project(":app")
    val dataProject = project(":data")
    val domainProject = project(":domain")

    val coverageExcludes = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.class",
        "**/Manifest.class",
        "**/Manifest$*.class",

        // Generated Room/KSP implementation classes.
        "**/*_Impl.class",
        "**/*_Impl$*.class",

        // Tests must not become production coverage targets.
        "**/*Test.class",
        "**/*Test$*.class",
    )

    val appKotlinClasses = appProject.fileTree(
        appProject.layout.buildDirectory
            .dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")
            .get()
            .asFile
    ) {
        coverageExcludes.forEach { exclude(it) }
    }

    val appJavaClasses = appProject.fileTree(
        appProject.layout.buildDirectory
            .dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")
            .get()
            .asFile
    ) {
        coverageExcludes.forEach { exclude(it) }
    }

    val dataKotlinClasses = dataProject.fileTree(
        dataProject.layout.buildDirectory
            .dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")
            .get()
            .asFile
    ) {
        coverageExcludes.forEach { exclude(it) }
    }

    val dataJavaClasses = dataProject.fileTree(
        dataProject.layout.buildDirectory
            .dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")
            .get()
            .asFile
    ) {
        coverageExcludes.forEach { exclude(it) }
    }

    val domainKotlinClasses = domainProject.fileTree(
        domainProject.layout.buildDirectory
            .dir("classes/kotlin/main")
            .get()
            .asFile
    ) {
        coverageExcludes.forEach { exclude(it) }
    }

    val domainJavaClasses = domainProject.fileTree(
        domainProject.layout.buildDirectory
            .dir("classes/java/main")
            .get()
            .asFile
    ) {
        coverageExcludes.forEach { exclude(it) }
    }

    sourceDirectories.setFrom(
        files(
            appProject.file("src/main/java"),
            appProject.file("src/main/kotlin"),

            dataProject.file("src/main/java"),
            dataProject.file("src/main/kotlin"),

            domainProject.file("src/main/java"),
            domainProject.file("src/main/kotlin"),
        )
    )

    classDirectories.setFrom(
        files(
            appKotlinClasses,
            appJavaClasses,

            dataKotlinClasses,
            dataJavaClasses,

            domainKotlinClasses,
            domainJavaClasses,
        )
    )

    reports {
        xml.required.set(true)
        xml.outputLocation.set(
            layout.buildDirectory.file(
                "reports/jacoco/lab/jacoco.xml"
            )
        )

        html.required.set(true)
        html.outputLocation.set(
            layout.buildDirectory.dir(
                "reports/jacoco/lab/html"
            )
        )

        csv.required.set(false)
    }

    doLast {
        val reportFile = layout.buildDirectory
            .file("reports/jacoco/lab/jacoco.xml")
            .get()
            .asFile

        check(reportFile.isFile) {
            "JaCoCo XML report not found: $reportFile"
        }

        val documentBuilder =
            DocumentBuilderFactory.newInstance().apply {
                setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                    false,
                )
                setFeature(
                    "http://xml.org/sax/features/validation",
                    false,
                )
            }.newDocumentBuilder()

        documentBuilder.setEntityResolver { _, _ ->
            InputSource(StringReader(""))
        }

        val document = documentBuilder.parse(reportFile)
        val root = document.documentElement

        val rootCounters = buildList<Element> {
            for (index in 0 until root.childNodes.length) {
                val node = root.childNodes.item(index)

                if (
                    node is Element &&
                    node.tagName == "counter"
                ) {
                    add(node)
                }
            }
        }

        fun printCoverage(
            type: String,
            label: String,
        ) {
            val counter =
                rootCounters.single {
                    it.getAttribute("type") == type
                }

            val missed =
                counter.getAttribute("missed").toLong()

            val covered =
                counter.getAttribute("covered").toLong()

            val total = missed + covered

            val percent =
                if (total == 0L) {
                    100.toBigDecimal()
                } else {
                    covered.toBigDecimal()
                        .multiply(100.toBigDecimal())
                        .divide(
                            total.toBigDecimal(),
                            2,
                            RoundingMode.HALF_UP,
                        )
                }

            println(
                "$label coverage: $percent% ($covered/$total)"
            )
        }

        println("Coverage target: app + data + domain")
        printCoverage("LINE", "Line")
        printCoverage("BRANCH", "Branch")

        println(
            "HTML report: ${
                layout.buildDirectory
                    .dir("reports/jacoco/lab/html")
                    .get()
                    .asFile
            }"
        )

        println("XML report:  $reportFile")
    }
}
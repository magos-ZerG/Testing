package com.z23u184.studymate.data.lab

import io.qameta.allure.Allure
import io.qameta.allure.junit4.AllureJunit4
import org.junit.runner.notification.RunNotifier
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.InitializationError
import org.robolectric.RobolectricTestRunner
import org.robolectric.internal.bytecode.Sandbox
import java.lang.reflect.Method
import kotlin.random.Random

private fun resolveSeed(className: String): Long {
    val baseSeed = System.getProperty("lab.random.seed")?.toLongOrNull() ?: System.nanoTime()
    return baseSeed xor className.hashCode().toLong()
}

private fun randomized(
    methods: List<FrameworkMethod>,
    seed: Long,
): MutableList<FrameworkMethod> = methods.shuffled(Random(seed)).toMutableList()

class RandomOrderRunner @Throws(InitializationError::class) constructor(
    private val labTestClass: Class<*>,
) : BlockJUnit4ClassRunner(labTestClass) {
    private val seed = resolveSeed(labTestClass.name)

    override fun getChildren(): MutableList<FrameworkMethod> =
        randomized(super.getChildren(), seed)

    override fun run(notifier: RunNotifier) {
        println("[lab] random order seed=$seed class=${labTestClass.name}")
        notifier.addListener(AllureJunit4())
        super.run(notifier)
    }
}

class RandomRobolectricTestRunner @Throws(InitializationError::class) constructor(
    private val labTestClass: Class<*>,
) : RobolectricTestRunner(labTestClass) {

    private val seed = resolveSeed(labTestClass.name)

    override fun getChildren(): MutableList<FrameworkMethod> =
        randomized(super.getChildren(), seed)

    override fun run(notifier: RunNotifier) {
        println("[lab] random order seed=$seed class=${labTestClass.name}")

        notifier.addListener(AllureJunit4())

        super.run(notifier)
    }

    override fun beforeTest(
        sandbox: Sandbox,
        method: FrameworkMethod,
        bootstrappedMethod: Method,
    ) {
        super.beforeTest(
            sandbox,
            method,
            bootstrappedMethod,
        )

        Allure.getLifecycle().updateTestCase { testResult ->
            testResult.start = System.currentTimeMillis()
        }
    }
}

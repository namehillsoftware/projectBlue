package com.lasthopesoftware

import android.os.Looper
import org.junit.runners.model.InitializationError
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.internal.SandboxTestRunner
import java.util.*

open class AndroidContextRunner(testClass: Class<*>) : RobolectricTestRunner(testClass) {
	private val testRunners = HashMap<Class<*>, HelperTestRunner>()

	@Synchronized
	override fun getHelperTestRunner(bootstrappedTestClass: Class<*>): SandboxTestRunner.HelperTestRunner {
		return try {
			if (!testRunners.containsKey(bootstrappedTestClass)) testRunners[bootstrappedTestClass] =
				HelperTestRunner(bootstrappedTestClass)
			testRunners[bootstrappedTestClass]!!
		} catch (initializationError: InitializationError) {
			throw RuntimeException(initializationError)
		}
	}

	protected class HelperTestRunner(bootstrappedTestClass: Class<*>?) :
		RobolectricTestRunner.HelperTestRunner(bootstrappedTestClass) {
		private var isCheckedForBeforeMethod = false
		@Throws(Exception::class)
		public override fun createTest(): Any {
			val test = super.createTest()
			// Note that JUnit4 will call this createTest() multiple times for each
			// test method, so we need to ensure to call "beforeClassSetup" only once.
			if (!isCheckedForBeforeMethod) {
				val beforeMethod = test.javaClass.getMethod("before")
				beforeMethod.invoke(test)
				isCheckedForBeforeMethod = true
				Looper.getMainLooper()?.let { shadowOf(it).idle() }
			}
			return test
		}
	}
}

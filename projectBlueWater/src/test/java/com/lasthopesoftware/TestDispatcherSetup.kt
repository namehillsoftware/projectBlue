package com.lasthopesoftware

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
object TestDispatcherSetup {
	fun setupTestDispatcher(): TestDispatcher {
		val testDispatcher = StandardTestDispatcher()
		Dispatchers.setMain(testDispatcher)
		return testDispatcher
	}
}

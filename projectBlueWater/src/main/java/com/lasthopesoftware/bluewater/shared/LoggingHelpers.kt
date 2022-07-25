package com.lasthopesoftware.bluewater.shared

import org.slf4j.LoggerFactory

inline fun <reified T> lazyLogger() = lazy { LoggerFactory.getLogger(T::class.java) }

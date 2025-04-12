package com.lasthopesoftware

import org.junit.runner.RunWith

@RunWith(AndroidContextRunner::class)
abstract class AndroidContext {
    abstract fun before()
}

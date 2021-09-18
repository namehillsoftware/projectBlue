package com.lasthopesoftware.resources.executors

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class CachedSingleThreadExecutor(name: String) :
    ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, LinkedBlockingQueue(), NamedThreadFactory(name))

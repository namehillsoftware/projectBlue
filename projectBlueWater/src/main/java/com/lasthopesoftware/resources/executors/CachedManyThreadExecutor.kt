package com.lasthopesoftware.resources.executors

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class CachedManyThreadExecutor(name: String, maximumThreads: Int, keepAliveTime: Long, keepAliveUnit: TimeUnit?) :
    ThreadPoolExecutor(0, maximumThreads, keepAliveTime, keepAliveUnit, LinkedBlockingQueue(), PrefixedThreadFactory(name))

package com.lasthopesoftware.resources.executors

import org.joda.time.Duration
import java.util.concurrent.Executors.defaultThreadFactory
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class CachedManyThreadExecutor(name: String, maximumThreads: Int, keepAliveTime: Duration) :
    ThreadPoolExecutor(0, maximumThreads, keepAliveTime.millis, TimeUnit.MILLISECONDS, SynchronousQueue(), PrefixedThreadFactory(name, defaultThreadFactory()))

package com.lasthopesoftware.bluewater.repository

import com.lasthopesoftware.resources.executors.CachedSingleThreadExecutor
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise

private val databaseExecutor by lazy { CachedSingleThreadExecutor() }

class DatabasePromise<T>(messageWriter: MessageWriter<T>)
	: QueuedPromise<T>(messageWriter, databaseExecutor)

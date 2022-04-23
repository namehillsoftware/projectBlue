package com.lasthopesoftware.bluewater.repository

import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise

class DatabasePromise<T>(messageWriter: MessageWriter<T>) : QueuedPromise<T>(messageWriter, ThreadPools.database)

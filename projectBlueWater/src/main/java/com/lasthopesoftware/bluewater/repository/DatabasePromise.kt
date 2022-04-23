package com.lasthopesoftware.bluewater.repository

import com.lasthopesoftware.resources.executors.ThreadPools
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise

@Suppress("FunctionName")
inline fun <T, reified Table : Entity> DatabaseTablePromise(messageWriter: MessageWriter<T>) = QueuedPromise(messageWriter, ThreadPools.databaseTableExecutor<Table>())

class DatabasePromise<T>(messageWriter: MessageWriter<T>) : QueuedPromise<T>(messageWriter, ThreadPools.database)

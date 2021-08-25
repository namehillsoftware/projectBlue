package com.lasthopesoftware.bluewater.repository

import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper.Companion.databaseExecutor
import com.namehillsoftware.artful.Artful
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise

fun <T> Artful.promiseFirst(cls: Class<T>): Promise<T> =
	QueuedPromise(MessageWriter { fetchFirst(cls) }, databaseExecutor())

fun Artful.promiseExecution(): Promise<Long> =
	QueuedPromise(MessageWriter { execute() }, databaseExecutor())

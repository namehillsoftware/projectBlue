package com.lasthopesoftware.bluewater.repository

import com.lasthopesoftware.bluewater.repository.RepositoryAccessHelper.Companion.databaseExecutor
import com.namehillsoftware.artful.Artful
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise

inline fun <reified T> Artful.fetchFirst(): T =
	fetchFirst(T::class.java)

inline fun <reified T> Promise<Artful>.promiseFirst(): Promise<T> =
	then { it.fetchFirst(T::class.java) }

inline fun <reified T> Artful.promiseFirst(): Promise<T> =
	QueuedPromise(MessageWriter { fetchFirst(T::class.java) }, databaseExecutor())

fun Promise<Artful>.promiseExecution(): Promise<Long> =
	then { it.execute() }

fun Artful.promiseExecution(): Promise<Long> =
	QueuedPromise(MessageWriter { execute() }, databaseExecutor())

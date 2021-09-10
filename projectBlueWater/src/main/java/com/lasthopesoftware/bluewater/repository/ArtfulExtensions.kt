package com.lasthopesoftware.bluewater.repository

import com.namehillsoftware.artful.Artful
import com.namehillsoftware.handoff.promises.Promise

inline fun <reified T> Artful.fetchFirst(): T = fetchFirst(T::class.java)

inline fun <reified T> Artful.fetch(): List<T> = fetch(T::class.java)

inline fun <reified T> Promise<Artful>.promiseFirst(): Promise<T> =
	then { it.fetchFirst(T::class.java) }

inline fun <reified T> Artful.promiseFirst(): Promise<T> =
	DatabasePromise { fetchFirst(T::class.java) }

fun Promise<Artful>.promiseExecution(): Promise<Long> =
	then { it.execute() }

fun Artful.promiseExecution(): Promise<Long> =
	DatabasePromise { execute() }

package com.lasthopesoftware.bluewater.repository

import android.database.sqlite.SQLiteDatabase
import com.namehillsoftware.artful.Artful
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise

class PromisingArtful(database: SQLiteDatabase, sqlQuery: String) : Artful(database, sqlQuery) {
	inline fun <reified T> promiseFirst(): Promise<T> =
		QueuedPromise(MessageWriter { fetchFirst(T::class.java) }, RepositoryAccessHelper.databaseExecutor())

	fun promiseExecution(): Promise<Long> =
		QueuedPromise(MessageWriter { execute() }, RepositoryAccessHelper.databaseExecutor())

	override fun addParameter(parameter: String, value: Int): PromisingArtful {
		super.addParameter(parameter, value)
		return this
	}

	override fun <E : Enum<E>> addParameter(parameter: String, value: Enum<E>): PromisingArtful {
		super.addParameter(parameter, value)
		return this
	}

	override fun addParameter(parameter: String, value: Any): PromisingArtful {
		super.addParameter(parameter, value)
		return this
	}

	override fun addParameter(parameter: String, value: Boolean): PromisingArtful {
		super.addParameter(parameter, value)
		return this
	}
}

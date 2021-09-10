package com.lasthopesoftware.bluewater.repository

import android.database.sqlite.SQLiteDatabase
import com.namehillsoftware.artful.Artful
import com.namehillsoftware.handoff.promises.Promise

class PromisingArtful(database: SQLiteDatabase, sqlQuery: String) : Artful(database, sqlQuery) {
	inline fun <reified T> promiseFirst(): Promise<T> =
		DatabasePromise { fetchFirst(T::class.java) }

	fun promiseExecution(): Promise<Long> =
		DatabasePromise { execute() }

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

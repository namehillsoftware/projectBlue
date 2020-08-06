package com.lasthopesoftware.bluewater.repository

import android.database.sqlite.SQLiteDatabase
import java.io.Closeable
import java.util.concurrent.locks.ReadWriteLock

class CloseableNonExclusiveTransaction internal constructor(private val sqLiteDatabase: SQLiteDatabase, databaseSynchronization: ReadWriteLock) : Closeable, ITransactionSuccessSetter {

	private val readLock = databaseSynchronization.readLock()

	init {
		readLock.lock()
		sqLiteDatabase.beginTransactionNonExclusive()
	}

	override fun setTransactionSuccessful() {
		sqLiteDatabase.setTransactionSuccessful()
	}

	override fun close() {
		sqLiteDatabase.endTransaction()
		readLock.unlock()
	}
}

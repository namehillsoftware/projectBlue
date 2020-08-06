package com.lasthopesoftware.bluewater.repository

import android.database.sqlite.SQLiteDatabase
import java.io.Closeable
import java.util.concurrent.locks.ReadWriteLock

class CloseableTransaction internal constructor(private val sqLiteDatabase: SQLiteDatabase, databaseSynchronization: ReadWriteLock) : Closeable, ITransactionSuccessSetter {

	private val writeLock = databaseSynchronization.writeLock()

	init {
		writeLock.lock()
		sqLiteDatabase.beginTransaction()
	}

	override fun close() {
		sqLiteDatabase.endTransaction()
		writeLock.unlock()
	}

	override fun setTransactionSuccessful() {
		sqLiteDatabase.setTransactionSuccessful()
	}
}

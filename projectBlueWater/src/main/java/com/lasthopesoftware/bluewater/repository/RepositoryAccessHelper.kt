package com.lasthopesoftware.bluewater.repository

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.repository.CachedFile
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityCreator
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityUpdater
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFileEntityCreator
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFileEntityUpdater
import com.lasthopesoftware.resources.executors.CachedSingleThreadExecutor
import com.namehillsoftware.artful.Artful
import java.io.Closeable
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantReadWriteLock

class RepositoryAccessHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION), Closeable {

	companion object {
		@JvmStatic
		fun databaseExecutor(): Executor {
			return databaseExecutor.value
		}

		private val databaseSynchronization = lazy { ReentrantReadWriteLock() }
		private val databaseExecutor = lazy { CachedSingleThreadExecutor() }
		private const val DATABASE_VERSION = 8
		private const val DATABASE_NAME = "sessions_db"
		private val entityCreators = lazy { arrayOf(LibraryEntityCreator, StoredFileEntityCreator, StoredItem(), CachedFile()) }
		private val entityUpdaters = lazy { arrayOf(LibraryEntityUpdater, StoredFileEntityUpdater, StoredItem(), CachedFile()) }
	}

	private val sqliteDb = lazy { this.writableDatabase }

	fun mapSql(sqlQuery: String?): Artful = Artful(sqliteDb.value, sqlQuery)

	override fun onCreate(db: SQLiteDatabase) {
		for (entityCreator in entityCreators.value) entityCreator.onCreate(db)
	}

	override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
		for (entityUpdater in entityUpdaters.value) entityUpdater.onUpdate(db, oldVersion, newVersion)
	}

	fun beginTransaction(): CloseableTransaction = CloseableTransaction(sqliteDb.value, databaseSynchronization.value)

	fun beginNonExclusiveTransaction(): CloseableNonExclusiveTransaction =
		CloseableNonExclusiveTransaction(sqliteDb.value, databaseSynchronization.value)

	override fun close() {
		super.close()
		if (sqliteDb.isInitialized()) sqliteDb.value.close()
	}
}

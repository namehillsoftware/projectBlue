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
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsCreator
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsMigrator
import com.lasthopesoftware.bluewater.settings.repository.ApplicationSettingsUpdater
import com.lasthopesoftware.resources.executors.CachedSingleThreadExecutor
import com.namehillsoftware.artful.Artful
import com.namehillsoftware.handoff.promises.Promise
import com.namehillsoftware.handoff.promises.queued.MessageWriter
import com.namehillsoftware.handoff.promises.queued.QueuedPromise
import java.io.Closeable
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantReadWriteLock

class RepositoryAccessHelper(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION), Closeable {

	companion object {
		@JvmStatic
		fun databaseExecutor(): Executor = databaseExecutor

		private val databaseSynchronization by lazy { ReentrantReadWriteLock() }
		private val databaseExecutor by lazy { CachedSingleThreadExecutor() }
		private const val DATABASE_VERSION = 9
		private const val DATABASE_NAME = "sessions_db"
	}

	private val applicationSettingsMigrator by lazy { ApplicationSettingsMigrator(context) }

	private val entityCreators = lazy { arrayOf(LibraryEntityCreator, StoredFileEntityCreator, StoredItem(), CachedFile(), ApplicationSettingsCreator(applicationSettingsMigrator)) }
	private val entityUpdaters = lazy { arrayOf(LibraryEntityUpdater, StoredFileEntityUpdater, StoredItem(), CachedFile(), ApplicationSettingsUpdater(applicationSettingsMigrator)) }

	private val sqliteDb = lazy { writableDatabase }

	fun mapSql(sqlQuery: String): Artful = Artful(sqliteDb.value, sqlQuery)

	fun promiseSqlMapper(sqlQuery: String): Promise<PromisingArtful> =
		QueuedPromise(MessageWriter { PromisingArtful(sqliteDb.value, sqlQuery) }, databaseExecutor)

	override fun onCreate(db: SQLiteDatabase) {
		for (entityCreator in entityCreators.value) entityCreator.onCreate(db)
	}

	override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
		for (entityUpdater in entityUpdaters.value) entityUpdater.onUpdate(db, oldVersion, newVersion)
	}

	fun beginTransaction(): CloseableTransaction = CloseableTransaction(sqliteDb.value, databaseSynchronization)

	fun beginNonExclusiveTransaction(): CloseableNonExclusiveTransaction =
		CloseableNonExclusiveTransaction(sqliteDb.value, databaseSynchronization)

	override fun close() {
		super.close()
		if (sqliteDb.isInitialized()) sqliteDb.value.close()
	}
}

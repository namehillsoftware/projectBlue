package com.lasthopesoftware.bluewater.client.stored.library.items.files.repository

import android.content.ContentUris
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import androidx.annotation.Keep
import com.lasthopesoftware.bluewater.repository.IEntityUpdater
import com.lasthopesoftware.bluewater.repository.fetch
import com.lasthopesoftware.resources.uri.MediaCollections
import com.namehillsoftware.querydroid.SqLiteAssistants
import com.namehillsoftware.querydroid.SqLiteCommand
import org.slf4j.LoggerFactory
import java.io.File

object StoredFileEntityUpdater : IEntityUpdater {
    override fun onUpdate(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 13) recreateTableSchema(db)
    }

	private val logger = LoggerFactory.getLogger(StoredFileEntityUpdater::class.java)

	private const val checkIfStoredFilesExists =
		"SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='${StoredFileEntityInformation.tableName}'"

	private fun recreateTableSchema(db: SQLiteDatabase) {
		val artful = SqLiteCommand(db, checkIfStoredFilesExists)
		val storedFileCheckResults = artful.execute()
		if (storedFileCheckResults == 0L) {
			db.execSQL(StoredFileEntityInformation.createTableSql)
			return
		}
		val storedFilesTempTableName = StoredFileEntityInformation.tableName + "Temp"
		try {
			db.execSQL("DROP TABLE `$storedFilesTempTableName`")
		} catch (se: SQLException) {
			logger.warn("There was an error while dropping the temp table", se)
		}
		val createTempTableSql = StoredFileEntityInformation.createTableSql
			.replaceFirst(
				StoredFileEntityInformation.tableName,
				storedFilesTempTableName
			)
		logger.warn("Creating temp table with SQL: $createTempTableSql")
		db.execSQL(createTempTableSql)
		try {
			val oldStoredFiles = SqLiteCommand(db, "SELECT * FROM " + StoredFileEntityInformation.tableName).fetch<Version12StoredFile>()

			for (oldStoredFile in oldStoredFiles) {
				val newUriString = oldStoredFile
					.storedMediaId
					.takeIf { it > 0 }
					?.let { ContentUris.withAppendedId(MediaCollections.ExternalAudio, it.toLong()).toString() }
					?: oldStoredFile.path?.let(::File)?.toURI()?.toString()

				if (newUriString.isNullOrEmpty()) continue

				SqLiteAssistants.insertValue(
					db,
					storedFilesTempTableName,
					StoredFile()
						.setServiceId(oldStoredFile.serviceId)
						.setLibraryId(oldStoredFile.libraryId)
						.setIsDownloadComplete(oldStoredFile.isDownloadComplete)
						.setIsOwner(oldStoredFile.isOwner)
						.setUri(newUriString)
				)
			}
		} catch (sqlException: SQLException) {
			logger.error("There was an error moving the data!", sqlException)
			throw sqlException
		}
		db.execSQL("DROP TABLE `${StoredFileEntityInformation.tableName}`")
		db.execSQL("ALTER TABLE `$storedFilesTempTableName` RENAME TO `${StoredFileEntityInformation.tableName}`")
	}

	@Keep
	class Version12StoredFile {
		var libraryId = 0
		var storedMediaId = 0
		var serviceId = 0
		var isDownloadComplete = false
		var path: String? = null
		var isOwner = false
	}
}

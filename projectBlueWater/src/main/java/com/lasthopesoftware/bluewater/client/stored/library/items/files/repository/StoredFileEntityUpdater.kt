package com.lasthopesoftware.bluewater.client.stored.library.items.files.repository

import android.content.ContentUris
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import androidx.annotation.Keep
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFileEntityInformation.tableName
import com.lasthopesoftware.bluewater.repository.IEntityUpdater
import com.lasthopesoftware.bluewater.repository.InsertBuilder
import com.lasthopesoftware.bluewater.repository.fetch
import com.lasthopesoftware.bluewater.shared.lazyLogger
import com.lasthopesoftware.resources.uri.MediaCollections
import com.namehillsoftware.querydroid.SqLiteAssistants
import com.namehillsoftware.querydroid.SqLiteCommand
import java.io.File

object StoredFileEntityUpdater : IEntityUpdater {
	private val logger by lazyLogger<StoredFileEntityUpdater>()

    override fun onUpdate(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 13) recreateTableSchema(db)

		if (oldVersion < 20) {
			val tempTableName = tableName + "Temp"
			db.execSQL("DROP TABLE IF EXISTS `$tempTableName`")

			val createTempTableSql = StoredFileEntityInformation.createTableSql
				.replaceFirst(
					tableName,
					tempTableName
				)

			db.execSQL(createTempTableSql)

			val storedFiles = SqLiteCommand(db, "SELECT * FROM $tableName").fetch<Version13StoredFile>()

			val storedFilesInsertStatement = storedFilesInsertStatement(tempTableName)
			for (storedFile in storedFiles) {
				val newStoredFile = storedFile.toStoredFile()

				SqLiteCommand(db, storedFilesInsertStatement)
					.addParameter("id", newStoredFile.id)
					.addParameter(StoredFileEntityInformation.libraryIdColumnName, newStoredFile.libraryId)
					.addParameter(StoredFileEntityInformation.serviceIdColumnName, newStoredFile.serviceId)
					.addParameter(StoredFileEntityInformation.uriColumnName, newStoredFile.uri)
					.addParameter(StoredFileEntityInformation.isDownloadCompleteColumnName, newStoredFile.isDownloadComplete)
					.addParameter(StoredFileEntityInformation.isOwnerColumnName, newStoredFile.isOwner)
					.execute()
			}

			db.execSQL("DROP TABLE `$tableName`")
			db.execSQL("ALTER TABLE `$tempTableName` RENAME TO `$tableName`")
		}
    }

	private const val checkIfStoredFilesExists =
		"SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='$tableName'"

	private fun recreateTableSchema(db: SQLiteDatabase) {
		val artful = SqLiteCommand(db, checkIfStoredFilesExists)
		val storedFileCheckResults = artful.execute()
		if (storedFileCheckResults == 0L) {
			db.execSQL(StoredFileEntityInformation.createTableSql)
			return
		}
		val storedFilesTempTableName = tableName + "Temp"
		try {
			db.execSQL("DROP TABLE `$storedFilesTempTableName`")
		} catch (se: SQLException) {
			logger.warn("There was an error while dropping the temp table", se)
		}
		val createTempTableSql = StoredFileEntityInformation.createTableSql
			.replaceFirst(
				tableName,
				storedFilesTempTableName
			)
		logger.warn("Creating temp table with SQL: $createTempTableSql")
		db.execSQL(createTempTableSql)
		try {
			val oldStoredFiles = SqLiteCommand(db, "SELECT * FROM " + tableName).fetch<Version12StoredFile>()

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
						.setServiceId(oldStoredFile.serviceId.toString())
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
		db.execSQL("DROP TABLE `$tableName`")
		db.execSQL("ALTER TABLE `$storedFilesTempTableName` RENAME TO `$tableName`")
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

	@Keep
	class Version13StoredFile {
		var id = 0
		var libraryId = 0
		var serviceId = 0
		var isDownloadComplete = false
		var uri: String? = null
		var isOwner = false

		fun toStoredFile(): StoredFile = StoredFile()
			.setId(id)
			.setLibraryId(libraryId)
			.setServiceId(serviceId.toString())
			.setIsDownloadComplete(isDownloadComplete)
			.setIsOwner(isOwner)
			.setUri(uri)
	}

	private fun storedFilesInsertStatement(tableName: String) = InsertBuilder
		.fromTable(tableName)
		.addColumn("id")
		.addColumn(StoredFileEntityInformation.libraryIdColumnName)
		.addColumn(StoredFileEntityInformation.serviceIdColumnName)
		.addColumn(StoredFileEntityInformation.uriColumnName)
		.addColumn(StoredFileEntityInformation.isDownloadCompleteColumnName)
		.addColumn(StoredFileEntityInformation.isOwnerColumnName)
		.build()
}

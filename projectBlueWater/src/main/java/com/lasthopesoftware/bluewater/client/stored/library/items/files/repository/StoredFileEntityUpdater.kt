package com.lasthopesoftware.bluewater.client.stored.library.items.files.repository

import android.content.ContentUris
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import androidx.annotation.Keep
import com.lasthopesoftware.bluewater.repository.IEntityUpdater
import com.lasthopesoftware.bluewater.repository.InsertBuilder
import com.lasthopesoftware.bluewater.repository.fetch
import com.lasthopesoftware.resources.uri.MediaCollections
import com.namehillsoftware.artful.Artful
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
		val artful = Artful(db, checkIfStoredFilesExists)
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
			val oldStoredFiles = Artful(db, "SELECT * FROM " + StoredFileEntityInformation.tableName).fetch<OldStoredFile>()

			val insertQuery = InsertBuilder.fromTable(storedFilesTempTableName)
				.addColumn(StoredFileEntityInformation.isDownloadCompleteColumnName)
				.addColumn(StoredFileEntityInformation.isOwnerColumnName)
				.addColumn(StoredFileEntityInformation.libraryIdColumnName)
				.addColumn(StoredFileEntityInformation.uriColumnName)
				.addColumn(StoredFileEntityInformation.serviceIdColumnName)
				.build()

			for (oldStoredFile in oldStoredFiles) {
				val newUriString = oldStoredFile
					.storedMediaId
					.takeIf { it > 0 }
					?.let { ContentUris.withAppendedId(MediaCollections.ExternalAudio, it.toLong()).toString() }
					?: oldStoredFile.path?.let(::File)?.toURI()?.toString()

				if (newUriString.isNullOrEmpty()) continue

				Artful(db, insertQuery)
					.addParameter(
						StoredFileEntityInformation.isDownloadCompleteColumnName,
						oldStoredFile.isDownloadComplete
					)
					.addParameter(StoredFileEntityInformation.isOwnerColumnName, oldStoredFile.isOwner)
					.addParameter(StoredFileEntityInformation.libraryIdColumnName, oldStoredFile.libraryId)
					.addParameter(StoredFileEntityInformation.uriColumnName, newUriString)
					.addParameter(StoredFileEntityInformation.serviceIdColumnName, oldStoredFile.serviceId)
					.execute()
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

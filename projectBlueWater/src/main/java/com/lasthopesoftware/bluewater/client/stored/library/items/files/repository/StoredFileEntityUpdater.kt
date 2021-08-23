package com.lasthopesoftware.bluewater.client.stored.library.items.files.repository

import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import com.lasthopesoftware.bluewater.repository.IEntityUpdater
import com.namehillsoftware.artful.Artful
import org.slf4j.LoggerFactory

object StoredFileEntityUpdater : IEntityUpdater {
    override fun onUpdate(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion <= 5) recreateTableSchema(db)
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
				StoredFileEntityInformation.tableName.toRegex(),
				storedFilesTempTableName
			)
		logger.warn("Creating temp table with SQL: $createTempTableSql")
		db.execSQL(createTempTableSql)
		try {
			val insertIntoTempTable = """INSERT INTO `$storedFilesTempTableName`
					(`id`,
					`${StoredFileEntityInformation.isDownloadCompleteColumnName}`,
					`${StoredFileEntityInformation.isOwnerColumnName}`,
					`${StoredFileEntityInformation.libraryIdColumnName}`,
					`${StoredFileEntityInformation.pathColumnName}`,
					`${StoredFileEntityInformation.serviceIdColumnName}`,
					`${StoredFileEntityInformation.storedMediaIdColumnName}`)
					SELECT
					`id`,
					`${StoredFileEntityInformation.isDownloadCompleteColumnName}`,
					`${StoredFileEntityInformation.isOwnerColumnName}`,
					`${StoredFileEntityInformation.libraryIdColumnName}`,
					`${StoredFileEntityInformation.pathColumnName}`,
					`${StoredFileEntityInformation.serviceIdColumnName}`,
					`${StoredFileEntityInformation.storedMediaIdColumnName}`
					FROM ${StoredFileEntityInformation.tableName}"""
			db.execSQL(insertIntoTempTable)
		} catch (sqlException: SQLException) {
			logger.error("There was an error moving the data!", sqlException)
			throw sqlException
		}
		db.execSQL("DROP TABLE `${StoredFileEntityInformation.tableName}`")
		db.execSQL("ALTER TABLE `$storedFilesTempTableName` RENAME TO `${StoredFileEntityInformation.tableName}`")
	}
}

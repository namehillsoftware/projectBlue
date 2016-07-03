package com.lasthopesoftware.bluewater.servers.library.items.media.files.stored.repository;

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.lasthopesoftware.bluewater.repository.IEntityUpdater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by david on 6/25/16.
 */
public class StoredFileEntityUpdater implements IEntityUpdater {

	private static final Logger logger = LoggerFactory.getLogger(StoredFileEntityUpdater.class);

	@Override
	public void onUpdate(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < 5) {
			db.execSQL(StoredFileEntityInformation.createTableSql);
			return;
		}

		if (oldVersion < 6) {
			final String storedFilesTempTableName = StoredFileEntityInformation.tableName + "Temp";

			try {
				db.execSQL("DROP TABLE `" + storedFilesTempTableName + "`");
			} catch (SQLException se) {
				logger.warn("There was an error while dropping the temp table", se);
			}

			final String createTempTableSql =
					StoredFileEntityInformation
							.createTableSql
							.replaceFirst(StoredFileEntityInformation.tableName, storedFilesTempTableName);
			logger.warn("Creating temp table with SQL: " + createTempTableSql);
			db.execSQL(createTempTableSql);

			try {
				final String insertIntoTempTable =
						"INSERT INTO `" + storedFilesTempTableName + "` " +
								"(`id`, " +
								"`" + StoredFileEntityInformation.isDownloadCompleteColumnName + "`, " +
								"`" + StoredFileEntityInformation.isOwnerColumnName + "`, " +
								"`" + StoredFileEntityInformation.libraryIdColumnName + "`, " +
								"`" + StoredFileEntityInformation.pathColumnName + "`, " +
								"`" + StoredFileEntityInformation.serviceIdColumnName + "`, " +
								"`" + StoredFileEntityInformation.storedMediaIdColumnName + "`) " +
								"SELECT " +
								"`id`, " +
								"`" + StoredFileEntityInformation.isDownloadCompleteColumnName + "`, " +
								"`" + StoredFileEntityInformation.isOwnerColumnName + "`, " +
								"`" + StoredFileEntityInformation.libraryIdColumnName + "`, " +
								"`" + StoredFileEntityInformation.pathColumnName + "`, " +
								"`" + StoredFileEntityInformation.serviceIdColumnName + "`, " +
								"`" + StoredFileEntityInformation.storedMediaIdColumnName + "` " +
								"FROM " + StoredFileEntityInformation.tableName;

				db.execSQL(insertIntoTempTable);
			} catch (SQLException sqlException) {
				logger.error("There was an error moving the data!", sqlException);
				throw sqlException;
			}

			db.execSQL("DROP TABLE `" + StoredFileEntityInformation.tableName + "`");
			db.execSQL("ALTER TABLE `" + storedFilesTempTableName + "` RENAME TO `" + StoredFileEntityInformation.tableName + "`");
		}
	}
}

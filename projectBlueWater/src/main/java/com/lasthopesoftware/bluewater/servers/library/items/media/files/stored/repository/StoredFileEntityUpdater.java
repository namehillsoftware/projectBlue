package com.lasthopesoftware.bluewater.servers.library.items.media.files.stored.repository;

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.lasthopesoftware.bluewater.repository.IEntityUpdater;

import org.slf4j.LoggerFactory;

/**
 * Created by david on 6/25/16.
 */
public class StoredFileEntityUpdater implements IEntityUpdater {

	@Override
	public void onUpdate(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < 6) {
			db.execSQL(StoredFileEntityInformation.createTableSql);
			return;
		}

		final String createTempTableSql =
				StoredFileEntityInformation
						.createTableSql
						.replaceFirst("`StoredFiles`", "`StoredFilesTemp`");

		db.execSQL(createTempTableSql);
		final String insertIntoTempTable =
				"INSERT INTO `StoredFilesTemp` (`id`, `isDownloadComplete`, `isOwner`, `libraryId`, `path`, `serviceId`, `storedMediaId`) " +
				"SELECT `id`, `isDownloadComplete`, `isOwner`, `libraryId`, `path`, `serviceId`, `storedMediaId` FROM StoredFilesTemp";

		try {
			db.execSQL(insertIntoTempTable);
		} catch (SQLException sqlException) {
			LoggerFactory.getLogger(StoredFile.class).error("There was an error moving the data! Rolling back.", sqlException);
			throw sqlException;
		}

		db.execSQL("DROP TABLE `StoredFiles`");
		db.execSQL("ALTER TABLE `StoredFilesTemp` RENAME TO `StoredFiles`");
	}
}

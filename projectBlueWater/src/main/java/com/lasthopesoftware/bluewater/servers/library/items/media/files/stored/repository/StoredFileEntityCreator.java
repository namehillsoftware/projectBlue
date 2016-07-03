package com.lasthopesoftware.bluewater.servers.library.items.media.files.stored.repository;

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.lasthopesoftware.bluewater.repository.IEntityCreator;
import com.lasthopesoftware.bluewater.repository.IEntityUpdater;

import org.slf4j.LoggerFactory;

/**
 * Created by david on 6/25/16.
 */
public class StoredFileEntityCreator implements IEntityCreator {

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(StoredFileEntityInformation.createTableSql);
	}
}

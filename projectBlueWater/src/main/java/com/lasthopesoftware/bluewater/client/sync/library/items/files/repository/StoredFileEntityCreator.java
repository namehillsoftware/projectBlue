package com.lasthopesoftware.bluewater.client.sync.library.items.files.repository;

import android.database.sqlite.SQLiteDatabase;
import com.lasthopesoftware.bluewater.repository.IEntityCreator;

/**
 * Created by david on 6/25/16.
 */
public class StoredFileEntityCreator implements IEntityCreator {

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(StoredFileEntityInformation.createTableSql);
	}
}

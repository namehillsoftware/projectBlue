package com.lasthopesoftware.bluewater.repository;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by david on 12/17/15.
 */
public interface IEntityCreator {
	void onCreate(SQLiteDatabase db);
}

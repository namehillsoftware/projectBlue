package com.lasthopesoftware.bluewater.repository;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by david on 12/17/15.
 */
public interface IRepository {
	void onCreate(SQLiteDatabase db);
	void onUpdate(SQLiteDatabase db, int oldVersion, int newVersion);
}

package com.lasthopesoftware.bluewater.repository;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by david on 6/25/16.
 */
public interface IEntityUpdater {
	void onUpdate(SQLiteDatabase db, int oldVersion, int newVersion);
}

package com.lasthopesoftware.bluewater.data.sqlite.access;

import java.sql.SQLException;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.data.sqlite.objects.SavedTrack;
import com.lasthopesoftware.bluewater.data.sqlite.objects.View;

public class DatabaseHandler extends OrmLiteSqliteOpenHelper  {

	private static int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "sessions_db";
	
	@SuppressWarnings("rawtypes")
	private static Class[] tables = { Library.class, View.class, SavedTrack.class };
	
	public DatabaseHandler(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void onCreate(SQLiteDatabase db, ConnectionSource conn) {
		for (Class table : tables) {
			try {
				TableUtils.createTable(conn, table);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void onUpgrade(SQLiteDatabase db, ConnectionSource conn, int arg2, int arg3) {
		for (Class table : tables) {
			try {
				TableUtils.dropTable(conn, table, true);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		onCreate(db, conn);
	}
}

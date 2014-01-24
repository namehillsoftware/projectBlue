package com.lasthopesoftware.bluewater.data.sqlite.access;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.lasthopesoftware.bluewater.data.sqlite.objects.ISqliteDefinition;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.data.sqlite.objects.View;

public class DatabaseHandler extends SQLiteOpenHelper {

	private static int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "library_db";
	
	private static ISqliteDefinition[] tables = { new Library(), new View() };
	
	public DatabaseHandler(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		for (ISqliteDefinition table : tables)
			db.execSQL(getCreateTableSql(table));
	}
	
	private String getCreateTableSql(ISqliteDefinition definition) {
		String returnString = "CREATE TABLE " + definition.getSqlName() + "(";
		
		for (String colDef : definition.getSqlColumnDefintions())
			returnString += colDef + ",";
		
		returnString = returnString.substring(0, returnString.length() - 1) + ")";
		
		return returnString;
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int arg1, int arg2) {
		for (ISqliteDefinition table : tables)
			db.execSQL("DROP TABLE IF EXISTS " + table.getSqlName());
		
		onCreate(db);
	}

}

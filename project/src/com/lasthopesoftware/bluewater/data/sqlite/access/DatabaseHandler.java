package com.lasthopesoftware.bluewater.data.sqlite.access;

import java.sql.SQLException;

import org.slf4j.LoggerFactory;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.DatabaseTableConfigUtil;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;
import com.j256.ormlite.table.TableUtils;
import com.lasthopesoftware.bluewater.data.sqlite.objects.Library;
import com.lasthopesoftware.bluewater.data.sqlite.objects.StoredFile;
import com.lasthopesoftware.bluewater.data.sqlite.objects.StoredList;

public class DatabaseHandler extends OrmLiteSqliteOpenHelper  {

	private static int DATABASE_VERSION = 3;
	private static final String DATABASE_NAME = "sessions_db";
	
	private static Class<?>[] version2Tables = { Library.class };
	private static Class<?>[] version3Tables = { StoredFile.class, StoredList.class };
	private static Class<?>[][] allTables = { version2Tables, version3Tables };
	
	public DatabaseHandler(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db, ConnectionSource conn) {
		for (Class<?>[] tableArray : allTables) createTables(conn, tableArray);
	}
	
	private void createTables(ConnectionSource conn, Class<?>[] tableClasses) {
		for (Class<?> table : tableClasses) {
			try {
				TableUtils.createTable(conn, table);
			} catch (SQLException e) {
				LoggerFactory.getLogger(DatabaseHandler.class).error(e.toString(), e);
			}
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, ConnectionSource conn, int oldVersion, int newVersion) {
		if (oldVersion < 2) {
			for (Class<?> table : version2Tables) {
				try {
					TableUtils.dropTable(conn, table, true);
				} catch (SQLException e) {
					LoggerFactory.getLogger(DatabaseHandler.class).error(e.toString(), e);
				}
			}
			createTables(conn, version2Tables);
		}
		
		if (oldVersion < 3)
			createTables(conn, version3Tables);
	}
	
	public <D extends Dao<T, ?>, T> D getAccessObject(Class<T> c) throws SQLException  {
		// lookup the dao, possibly invoking the cached database config
        Dao<T, ?> dao = DaoManager.lookupDao(connectionSource, c);
        if (dao == null) {
            // try to use our new reflection magic
            DatabaseTableConfig<T> tableConfig = DatabaseTableConfigUtil.fromClass(connectionSource, c);
            if (tableConfig == null) {
	            /**
	             * TODO: we have to do this to get to see if they are using the deprecated annotations like
	             * {@link DatabaseFieldSimple}.
	             */
                dao = (Dao<T, ?>) DaoManager.createDao(connectionSource, c);
            } else {
                dao = (Dao<T, ?>) DaoManager.createDao(connectionSource, tableConfig);
            }
        }

        @SuppressWarnings("unchecked")
        D castDao = (D) dao;
        return castDao;
	}
	
	@Override
	public void close() {
		super.close();
	}
}

package com.lasthopesoftware.bluewater.data.sqlite.access;

import java.sql.SQLException;

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

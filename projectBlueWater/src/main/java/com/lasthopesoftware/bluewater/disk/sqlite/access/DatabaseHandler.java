package com.lasthopesoftware.bluewater.disk.sqlite.access;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.DatabaseTableConfigUtil;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;
import com.j256.ormlite.table.TableUtils;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.cache.store.CachedFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.store.StoredFile;
import com.lasthopesoftware.bluewater.servers.library.items.store.StoredList;
import com.lasthopesoftware.bluewater.servers.store.Library;

import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseHandler extends OrmLiteSqliteOpenHelper  {
	public static final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
	
	private static int DATABASE_VERSION = 5;
	private static final String DATABASE_NAME = "sessions_db";
	
	private final static Class<?>[] version2Tables = { Library.class };
	private final static Class<?>[] version3Tables = { StoredFile.class, StoredList.class };
	private final static Class<?>[] version4Tables = { CachedFile.class };
	private final static Class<?>[][] allTables = { version2Tables, version3Tables, version4Tables };
	
	public DatabaseHandler(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db, ConnectionSource conn) {
		for (Class<?>[] tableArray : allTables) createTables(conn, tableArray);
	}
	
	private static void createTables(ConnectionSource conn, Class<?>... tableClasses) {
		for (Class<?> table : tableClasses) {
			try {
				TableUtils.createTable(conn, table);
			} catch (SQLException e) {
				LoggerFactory.getLogger(DatabaseHandler.class).error(e.toString(), e);
			}
		}
	}

	private static void recreateTables(ConnectionSource conn, Class<?>... tableClasses) {
		for (Class<?> table : tableClasses) {
			try {
				TableUtils.dropTable(conn, table, true);
			} catch (SQLException e) {
				LoggerFactory.getLogger(DatabaseHandler.class).error(e.toString(), e);
			}
		}
		createTables(conn, tableClasses);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, ConnectionSource conn, int oldVersion, int newVersion) {
		if (oldVersion < 2)
			recreateTables(conn, version2Tables);

		if (oldVersion < 4)
			createTables(conn, version4Tables);

		if (oldVersion < 5) {
			recreateTables(conn, version3Tables);
			try {
				final Dao<Library, Integer> libraryDao = getAccessObject(Library.class);
				libraryDao.executeRaw("ALTER TABLE `LIBRARIES` add column `customSyncedFilesPath` VARCHAR;");
				libraryDao.executeRaw("ALTER TABLE `LIBRARIES` add column `syncedFileLocation` VARCHAR DEFAULT 'INTERNAL';");
				libraryDao.executeRaw("ALTER TABLE `LIBRARIES` add column `isUsingExistingFiles` BOOLEAN DEFAULT 0;");
			} catch (SQLException e) {
				LoggerFactory.getLogger(DatabaseHandler.class).error("Error adding column syncedFilesPath to library table", e);
			}
		}
	}
	
	public <D extends Dao<T, ?>, T> D getAccessObject(Class<T> c) throws SQLException {
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
				dao = DaoManager.createDao(connectionSource, c);
			} else {
				dao = DaoManager.createDao(connectionSource, tableConfig);
			}
		}

		@SuppressWarnings("unchecked")
		D castDao = (D) dao;
		return castDao;
	}

	@Override
	public void close() {
		super.close();

		DaoManager.clearCache();
	}
}

package com.lasthopesoftware.bluewater.disk.sqlite.access;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.misc.SqlExceptionUtil;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTable;
import com.j256.ormlite.table.DatabaseTableConfig;
import com.j256.ormlite.table.TableUtils;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.cache.repository.CachedFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.repository.StoredFile;
import com.lasthopesoftware.bluewater.servers.library.items.repository.StoredItem;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseHandler {
	public static final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

	private static final ThreadLocal<SessionsDbHelper> sessionsDbInstance = new ThreadLocal<>();

	public DatabaseHandler(Context context) {
		if (sessionsDbInstance.get() == null)
			sessionsDbInstance.set(new SessionsDbHelper(context));
	}

	public static DatabaseHandler getInstance(Context context) {
		return new DatabaseHandler(context);
	}
	
	public <D extends Dao<T, ?>, T> D getAccessObject(Class<T> c) throws SQLException {
		return sessionsDbInstance.get().getDao(c);
	}

	private static class SessionsDbHelper extends OrmLiteSqliteOpenHelper {

		private static int DATABASE_VERSION = 5;
		private static final String DATABASE_NAME = "sessions_db";

		private final static Class<?>[] version2Tables = { Library.class };
		private final static Class<?>[] version3Tables = { StoredFile.class, StoredItem.class };
		private final static Class<?>[] version4Tables = { CachedFile.class };
		private final static Class<?>[][] allTables = { version2Tables, version3Tables, version4Tables };

		private final static Logger mLogger = LoggerFactory.getLogger(SessionsDbHelper.class);

		private static Map<Class<?>, DatabaseTableConfig<?>> configMap = new ConcurrentHashMap<>();

		public SessionsDbHelper (Context context) {
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
					mLogger.error(e.toString(), e);
				}
			}
		}

		private static void recreateTables(ConnectionSource conn, Class<?>... tableClasses) {
			for (Class<?> table : tableClasses) {
				try {
					TableUtils.dropTable(conn, table, true);
				} catch (SQLException e) {
					mLogger.error(e.toString(), e);
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
					final Dao<Library, Integer> libraryDao = getDao(Library.class);
					libraryDao.executeRaw("ALTER TABLE `LIBRARIES` add column `customSyncedFilesPath` VARCHAR;");
					libraryDao.executeRaw("ALTER TABLE `LIBRARIES` add column `syncedFileLocation` VARCHAR DEFAULT 'INTERNAL';");
					libraryDao.executeRaw("ALTER TABLE `LIBRARIES` add column `isUsingExistingFiles` BOOLEAN DEFAULT 0;");
					libraryDao.executeRaw("ALTER TABLE `LIBRARIES` add column `isSyncLocalConnectionsOnly` BOOLEAN DEFAULT 0;");
					libraryDao.executeRaw("DROP TABLE `StoredLists`;");
				} catch (SQLException e) {
					mLogger.error("Error adding column syncedFilesPath to library table", e);
				}
			}
		}

		@Override
		public <D extends Dao<T, ?>, T> D getDao(Class<T> clazz) throws SQLException {
			if (!configMap.containsKey(clazz))
				configMap.put(clazz, connectionSource.getDatabaseType().extractDatabaseTableConfig(connectionSource, clazz));

			final DatabaseTableConfig<T> databaseTableConfig = (DatabaseTableConfig<T>)configMap.get(clazz);

			Object[] arguments = new Object[] { connectionSource, databaseTableConfig };
			Constructor<?> constructor = databaseTableConfig.getConstructor();
			if (constructor == null) {
				final Class<?> daoClass = databaseTableConfig.getDataClass().getAnnotation(DatabaseTable.class).daoClass();
				throw new SQLException(
						"Could not find public constructor with ConnectionSource, DatabaseTableConfig parameters in class "
								+ daoClass);
			}
			try {
				return (D) constructor.newInstance(arguments);
			} catch (Exception e) {
				final Class<?> daoClass = databaseTableConfig.getDataClass().getAnnotation(DatabaseTable.class).daoClass();
				throw SqlExceptionUtil.create("Could not call the constructor in class " + daoClass, e);
			}
		}
	}
}

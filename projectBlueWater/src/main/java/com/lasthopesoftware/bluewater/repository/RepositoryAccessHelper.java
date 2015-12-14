package com.lasthopesoftware.bluewater.repository;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.cache.repository.CachedFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.local.sync.repository.StoredFile;
import com.lasthopesoftware.bluewater.servers.library.items.repository.StoredItem;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.lasthopesoftware.sql.SqlMapper;
import com.lasthopesoftware.threading.Lazy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Sql2o;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RepositoryAccessHelper extends SQLiteOpenHelper {
	public static final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

	private static final int DATABASE_VERSION = 5;
	private static final String DATABASE_NAME = "sessions_db";

	private final static Class<?>[] version2Tables = { Library.class };
	private final static Class<?>[] version3Tables = { StoredFile.class, StoredItem.class };
	private final static Class<?>[] version4Tables = { CachedFile.class };
	private final static Class<?>[][] allTables = { version2Tables, version3Tables, version4Tables };

	private final static Lazy<Logger> localLogger = new Lazy<>(new Callable<Logger>() {
		@Override
		public Logger call() throws Exception {
			return LoggerFactory.getLogger(RepositoryAccessHelper.class);
		}
	});

	private final Context context;

	private final Lazy<SQLiteDatabase> sqliteDb = new Lazy<>(new Callable<SQLiteDatabase>() {
		@Override
		public SQLiteDatabase call() throws Exception {
			return context.openOrCreateDatabase(DATABASE_NAME, 0, null);
		}
	});

	private static Sql2o sql2oInstance;

	public RepositoryAccessHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);

		this.context = context;

		if (sql2oInstance != null) return;

		final SQLiteDatabase sqliteDB = sqliteDb.getObject();
		final String dbUrl = "jdbc:sqlite:" + sqliteDB.getPath();
		final String dbUser = "";

		sql2oInstance = new Sql2o(dbUrl, dbUser, "", new SqlDroidQuirks());
	}
//
//	@Override
//	public void onCreate(SQLiteDatabase db, ConnectionSource conn) {
//		for (Class<?>[] tableArray : allTables) createTables(conn, tableArray);
//	}

//	private static void createTables(ConnectionSource conn, Class<?>... tableClasses) {
//		for (Class<?> table : tableClasses) {
//			try {
//				TableUtils.createTable(conn, table);
//			} catch (SQLException e) {
//				localLogger.getObject().error(e.toString(), e);
//			}
//		}
//	}
//
//	private static void recreateTables(ConnectionSource conn, Class<?>... tableClasses) {
//		for (Class<?> table : tableClasses) {
//			try {
//				TableUtils.dropTable(conn, table, true);
//			} catch (SQLException e) {
//				localLogger.getObject().error(e.toString(), e);
//			}
//		}
//		createTables(conn, tableClasses);
//	}
////
//	@Override
//	public void onUpgrade(SQLiteDatabase db, ConnectionSource conn, int oldVersion, int newVersion) {
//		if (oldVersion < 2)
//			recreateTables(conn, version2Tables);
//
//		if (oldVersion < 4)
//			createTables(conn, version4Tables);
//
//		if (oldVersion < 5) {
//			recreateTables(conn, version3Tables);
//			try {
//				final Dao<Library, Integer> libraryDao = getDao(Library.class);
//				libraryDao.executeRaw("ALTER TABLE `LIBRARIES` add column `customSyncedFilesPath` VARCHAR;");
//				libraryDao.executeRaw("ALTER TABLE `LIBRARIES` add column `syncedFileLocation` VARCHAR DEFAULT 'INTERNAL';");
//				libraryDao.executeRaw("ALTER TABLE `LIBRARIES` add column `isUsingExistingFiles` BOOLEAN DEFAULT 0;");
//				libraryDao.executeRaw("ALTER TABLE `LIBRARIES` add column `isSyncLocalConnectionsOnly` BOOLEAN DEFAULT 0;");
//				libraryDao.executeRaw("ALTER TABLE `LIBRARIES` add column `selectedViewType` VARCHAR;");
//				libraryDao.executeRaw("DROP TABLE `StoredLists`;");
//			} catch (SQLException e) {
//				localLogger.getObject().error("Error adding column syncedFilesPath to library table", e);
//			}
//		}
//	}

	public org.sql2o.Connection getConnection() {
		return sql2oInstance != null ? sql2oInstance.open() : null;
	}

	public SQLiteDatabase getDatabase() {
		return sqliteDb.getObject();
	}

	public  SqlMapper mapSql(String sqlQuery) {
		return new SqlMapper(sqliteDb.getObject(), sqlQuery);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
//		sqliteDb.getObject().
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

	@Override
	public synchronized void close() {
		super.close();

		if (sqliteDb.isInitialized())
			sqliteDb.getObject().close();
	}
}

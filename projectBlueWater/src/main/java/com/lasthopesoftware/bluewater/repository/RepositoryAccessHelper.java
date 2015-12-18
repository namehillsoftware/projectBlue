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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RepositoryAccessHelper extends SQLiteOpenHelper {
	public static final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

	private static final int DATABASE_VERSION = 5;
	private static final String DATABASE_NAME = "sessions_db";

	private final static Lazy<IRepository[]> repositories = new Lazy<IRepository[]>(new Callable<IRepository[]>() {
		@Override
		public IRepository[] call() throws Exception {
			return new IRepository[]{new Library(), new StoredFile(), new StoredItem(), new CachedFile() };
		}
	});

	private final static Lazy<Logger> localLogger = new Lazy<>(new Callable<Logger>() {
		@Override
		public Logger call() throws Exception {
			return LoggerFactory.getLogger(RepositoryAccessHelper.class);
		}
	});

	private final Lazy<SQLiteDatabase> sqliteDb = new Lazy<>(new Callable<SQLiteDatabase>() {
		@Override
		public SQLiteDatabase call() throws Exception {
			return getWritableDatabase();
		}
	});

	public RepositoryAccessHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public  SqlMapper mapSql(String sqlQuery) {
		return new SqlMapper(getWritableDatabase(), sqlQuery);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		for (IRepository repository : repositories.getObject())
			repository.onCreate(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		for (IRepository repository : repositories.getObject())
			repository.onUpdate(db, oldVersion, newVersion);
	}

	@Override
	public synchronized void close() {
		super.close();

		if (sqliteDb.isInitialized())
			sqliteDb.getObject().close();
	}
}

package com.lasthopesoftware.bluewater.repository;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.lasthopesoftware.bluewater.servers.library.items.media.files.cached.repository.CachedFile;
import com.lasthopesoftware.bluewater.servers.library.items.media.files.stored.repository.StoredFile;
import com.lasthopesoftware.bluewater.servers.library.items.stored.StoredItem;
import com.lasthopesoftware.bluewater.servers.library.repository.Library;
import com.vedsoft.futures.Lazy;
import com.vedsoft.objectified.Objectified;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RepositoryAccessHelper extends SQLiteOpenHelper {
	public static final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

	private static final int DATABASE_VERSION = 5;
	private static final String DATABASE_NAME = "sessions_db";

	private final static Lazy<IRepository[]> repositories = new Lazy<IRepository[]>() {
		@Override
		protected IRepository[] initialize() {
			return new IRepository[]{new Library(), new StoredFile(), new StoredItem(), new CachedFile() };
		}
	};

	private final static Lazy<Logger> localLogger = new Lazy<Logger>() {
		@Override
		protected Logger initialize() {
			return LoggerFactory.getLogger(RepositoryAccessHelper.class);
		}
	};

	private final Lazy<SQLiteDatabase> sqliteDb = new Lazy<SQLiteDatabase>() {
		@Override
		protected SQLiteDatabase initialize() {
			return getWritableDatabase();
		}
	};

	public RepositoryAccessHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public Objectified objectifySql(String sqlQuery) {
		return new Objectified(getWritableDatabase(), sqlQuery);
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

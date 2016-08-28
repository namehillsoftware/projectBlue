package com.lasthopesoftware.bluewater.repository;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.lasthopesoftware.bluewater.client.library.items.media.files.cached.repository.CachedFile;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFileEntityCreator;
import com.lasthopesoftware.bluewater.client.library.items.media.files.stored.repository.StoredFileEntityUpdater;
import com.lasthopesoftware.bluewater.client.library.items.stored.StoredItem;
import com.lasthopesoftware.bluewater.client.library.repository.Library;
import com.vedsoft.lazyj.Lazy;
import com.vedsoft.objective.droid.ObjectiveDroid;

import java.io.Closeable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RepositoryAccessHelper extends SQLiteOpenHelper implements Closeable {
	public static final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

	private static final int DATABASE_VERSION = 6;
	private static final String DATABASE_NAME = "sessions_db";

	private final static Lazy<IEntityCreator[]> entityCreators = new Lazy<>(() -> new IEntityCreator[]{new Library(), new StoredFileEntityCreator(), new StoredItem(), new CachedFile()});
	private final static Lazy<IEntityUpdater[]> entityUpdaters = new Lazy<>(() -> new IEntityUpdater[]{new Library(), new StoredFileEntityUpdater(), new StoredItem(), new CachedFile()});

	private final Lazy<SQLiteDatabase> sqliteDb = new Lazy<>(this::getWritableDatabase);

	public RepositoryAccessHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public ObjectiveDroid mapSql(String sqlQuery) {
		return new ObjectiveDroid(sqliteDb.getObject(), sqlQuery);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		for (IEntityCreator entityCreator : entityCreators.getObject())
			entityCreator.onCreate(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		for (IEntityUpdater entityUpdater : entityUpdaters.getObject())
			entityUpdater.onUpdate(db, oldVersion, newVersion);
	}

	public CloseableTransaction beginTransaction() {
		return new CloseableTransaction(sqliteDb.getObject());
	}

	public CloseableNonExclusiveTransaction beginNonExclusiveTransaction() {
		return new CloseableNonExclusiveTransaction(sqliteDb.getObject());
	}

	@Override
	public void close() {
		super.close();

		if (sqliteDb.isInitialized())
			sqliteDb.getObject().close();
	}
}

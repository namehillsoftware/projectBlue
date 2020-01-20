package com.lasthopesoftware.bluewater.repository;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.lasthopesoftware.bluewater.client.browsing.items.media.files.cached.repository.CachedFile;
import com.lasthopesoftware.bluewater.client.browsing.library.repository.Library;
import com.lasthopesoftware.bluewater.client.stored.library.items.StoredItem;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFileEntityCreator;
import com.lasthopesoftware.bluewater.client.stored.library.items.files.repository.StoredFileEntityUpdater;
import com.lasthopesoftware.resources.executors.CachedSingleThreadExecutor;
import com.namehillsoftware.artful.Artful;
import com.namehillsoftware.lazyj.CreateAndHold;
import com.namehillsoftware.lazyj.Lazy;

import java.io.Closeable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

public class RepositoryAccessHelper extends SQLiteOpenHelper implements Closeable {
	public static Executor databaseExecutor() {
		return databaseExecutor.getObject();
	}

	private static final CreateAndHold<ExecutorService> databaseExecutor = new Lazy<>(CachedSingleThreadExecutor::new);

	private static final int DATABASE_VERSION = 7;
	private static final String DATABASE_NAME = "sessions_db";

	private final static Lazy<IEntityCreator[]> entityCreators = new Lazy<>(() -> new IEntityCreator[]{new Library(), new StoredFileEntityCreator(), new StoredItem(), new CachedFile()});
	private final static Lazy<IEntityUpdater[]> entityUpdaters = new Lazy<>(() -> new IEntityUpdater[]{new Library(), new StoredFileEntityUpdater(), new StoredItem(), new CachedFile()});

	private final Lazy<SQLiteDatabase> sqliteDb = new Lazy<>(this::getWritableDatabase);

	public RepositoryAccessHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public Artful mapSql(String sqlQuery) {
		return new Artful(sqliteDb.getObject(), sqlQuery);
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

		if (sqliteDb.isCreated())
			sqliteDb.getObject().close();
	}
}

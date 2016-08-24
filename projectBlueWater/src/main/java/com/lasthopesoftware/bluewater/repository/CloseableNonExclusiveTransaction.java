package com.lasthopesoftware.bluewater.repository;

import android.database.sqlite.SQLiteDatabase;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by david on 8/24/16.
 */
public class CloseableNonExclusiveTransaction implements Closeable {

    private final SQLiteDatabase sqLiteDatabase;

    public CloseableNonExclusiveTransaction(SQLiteDatabase sqLiteDatabase) {
        this.sqLiteDatabase = sqLiteDatabase;
        this.sqLiteDatabase.beginTransactionNonExclusive();
    }

    @Override
    public void close() throws IOException {
        this.sqLiteDatabase.endTransaction();
    }
}

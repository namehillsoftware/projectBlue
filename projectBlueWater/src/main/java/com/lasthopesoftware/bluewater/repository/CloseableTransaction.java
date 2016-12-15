package com.lasthopesoftware.bluewater.repository;

import android.database.sqlite.SQLiteDatabase;

import java.io.Closeable;

/**
 * Created by david on 8/24/16.
 */
public class CloseableTransaction implements Closeable, ITransactionSuccessSetter {

    private final SQLiteDatabase sqLiteDatabase;

    CloseableTransaction(SQLiteDatabase sqLiteDatabase) {
        this.sqLiteDatabase = sqLiteDatabase;
        this.sqLiteDatabase.beginTransaction();
    }

    @Override
    public void close() {
        this.sqLiteDatabase.endTransaction();
    }

    @Override
    public void setTransactionSuccessful() {
        this.sqLiteDatabase.setTransactionSuccessful();
    }
}

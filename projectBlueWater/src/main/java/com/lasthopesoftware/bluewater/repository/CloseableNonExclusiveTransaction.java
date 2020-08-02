package com.lasthopesoftware.bluewater.repository;

import android.database.sqlite.SQLiteDatabase;

import java.io.Closeable;

public class CloseableNonExclusiveTransaction implements Closeable, ITransactionSuccessSetter {

    private final SQLiteDatabase sqLiteDatabase;

    CloseableNonExclusiveTransaction(SQLiteDatabase sqLiteDatabase) {
        this.sqLiteDatabase = sqLiteDatabase;
        this.sqLiteDatabase.beginTransactionNonExclusive();
    }

    public void setTransactionSuccessful() {
        this.sqLiteDatabase.setTransactionSuccessful();
    }

    @Override
    public void close() {
        this.sqLiteDatabase.endTransaction();
    }
}

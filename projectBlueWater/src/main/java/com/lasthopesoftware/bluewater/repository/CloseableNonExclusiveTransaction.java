package com.lasthopesoftware.bluewater.repository;

import android.database.sqlite.SQLiteDatabase;

import java.io.Closeable;
import java.io.IOException;

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
    public void close() throws IOException {
        this.sqLiteDatabase.endTransaction();
    }
}

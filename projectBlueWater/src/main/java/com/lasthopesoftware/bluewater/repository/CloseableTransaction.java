package com.lasthopesoftware.bluewater.repository;

import android.annotation.TargetApi;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by david on 8/24/16.
 */
public class CloseableTransaction implements Closeable, ITransactionSuccessSetter {

    private final SQLiteDatabase sqLiteDatabase;

    public CloseableTransaction(SQLiteDatabase sqLiteDatabase) {
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

package com.lasthopesoftware.bluewater.client.stored.library.items.files.repository

import android.database.sqlite.SQLiteDatabase
import com.lasthopesoftware.bluewater.repository.IEntityCreator

object StoredFileEntityCreator : IEntityCreator {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(StoredFileEntityInformation.createTableSql)
    }
}

package com.lasthopesoftware.bluewater.repository

import android.database.sqlite.SQLiteDatabase

interface IEntityCreator {
    fun onCreate(db: SQLiteDatabase)
}

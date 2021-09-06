package com.lasthopesoftware.bluewater.repository

import android.database.sqlite.SQLiteDatabase

/**
 * Created by david on 6/25/16.
 */
interface IEntityUpdater {
    fun onUpdate(db: SQLiteDatabase, oldVersion: Int, newVersion: Int)
}

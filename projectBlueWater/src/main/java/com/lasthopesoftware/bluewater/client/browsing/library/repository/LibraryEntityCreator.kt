package com.lasthopesoftware.bluewater.client.browsing.library.repository

import android.database.sqlite.SQLiteDatabase
import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryEntityInformation.createTableSql
import com.lasthopesoftware.bluewater.repository.IEntityCreator

object LibraryEntityCreator : IEntityCreator {
	override fun onCreate(db: SQLiteDatabase) = db.execSQL(createTableSql)
}

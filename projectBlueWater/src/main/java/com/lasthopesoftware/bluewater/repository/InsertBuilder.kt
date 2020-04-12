package com.lasthopesoftware.bluewater.repository

import java.util.*

/**
 * Created by david on 12/14/15.
 */
class InsertBuilder private constructor(private val tableName: String) {
	private val columns = ArrayList<String>()
	private var shouldReplace = false

	fun withReplacement(): InsertBuilder {
		shouldReplace = true
		return this
	}

	fun addColumn(column: String): InsertBuilder {
		columns.add(column)
		return this
	}

	fun build(): String {
		val sqlStringBuilder = StringBuilder("INSERT ");

		if (shouldReplace) sqlStringBuilder.append(" OR REPLACE ")

		sqlStringBuilder.append(" INTO $tableName (");

		for (column in columns) {
			sqlStringBuilder.append(column)
			if (column !== columns[columns.size - 1]) sqlStringBuilder.append(", ")
		}

		sqlStringBuilder.append(") VALUES (")
		for (column in columns) {
			sqlStringBuilder.append('@').append(column)
			if (column !== columns[columns.size - 1]) sqlStringBuilder.append(", ")
		}

		return sqlStringBuilder.append(')').toString()
	}

	companion object {
		@kotlin.jvm.JvmStatic
		fun fromTable(tableName: String): InsertBuilder {
			return InsertBuilder(tableName)
		}
	}
}

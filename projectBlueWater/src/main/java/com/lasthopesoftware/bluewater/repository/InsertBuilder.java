package com.lasthopesoftware.bluewater.repository;

import java.util.ArrayList;

/**
 * Created by david on 12/14/15.
 */
public class InsertBuilder {
	private final StringBuilder sqlStringBuilder;
	private final ArrayList<String> columns = new ArrayList<>();

	public static InsertBuilder fromTable(String tableName) {
		return new InsertBuilder(tableName);
	}

	private InsertBuilder(String tableName) {
		sqlStringBuilder = new StringBuilder("INSERT INTO " + tableName + " (");
	}

	public InsertBuilder addColumn(String column) {
		columns.add(column);

		return this;
	}

	public String build() {
		for (String column : columns) {
			sqlStringBuilder.append(column);
			if (column != columns.get(columns.size() - 1))
				sqlStringBuilder.append(", ");
		}

		sqlStringBuilder.append(") VALUES (");

		for (String column : columns) {
			sqlStringBuilder.append(':').append(column);
			if (column != columns.get(columns.size() - 1))
				sqlStringBuilder.append(", ");
		}

		return sqlStringBuilder.append(')').toString();
	}
}

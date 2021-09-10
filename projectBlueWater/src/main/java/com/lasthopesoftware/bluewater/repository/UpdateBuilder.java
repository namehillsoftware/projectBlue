package com.lasthopesoftware.bluewater.repository;

import java.util.ArrayList;

/**
 * Created by david on 12/14/15.
 */
public class UpdateBuilder {
	private final StringBuilder sqlStringBuilder;
	private final ArrayList<String> setters = new ArrayList<>();
	private String filter = "";

	public static UpdateBuilder fromTable(String tableName) {
		return new UpdateBuilder(tableName);
	}

	private UpdateBuilder(String tableName) {
		sqlStringBuilder = new StringBuilder("UPDATE " + tableName + " SET ");
	}

	public UpdateBuilder addSetter(String columnName) {
		setters.add(columnName);
		return this;
	}

	public UpdateBuilder setFilter(String filter) {
		this.filter = filter;
		return this;
	}

	public String buildQuery() {
		for (String setter : setters) {
			sqlStringBuilder.append(setter).append(" = @").append(setter);
			if (setter != setters.get(setters.size()  - 1))
				sqlStringBuilder.append(", ");
		}

		return sqlStringBuilder.append(' ').append(filter).toString();
	}
}

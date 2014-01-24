package com.lasthopesoftware.bluewater.data.sqlite.objects;

public interface ISqliteDefinition {
	String getTableName();
	String[] getTableColumns();
	String[] getTableColumnDefintions();
}

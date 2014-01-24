package com.lasthopesoftware.bluewater.data.sqlite.objects;

public interface ISqliteDefinition {
	String getSqlName();
	String[] getSqlColumns();
	String[] getSqlColumnDefintions();
}

package com.lasthopesoftware.bluewater.repository;

import org.sql2o.converters.Converter;
import org.sql2o.quirks.NoQuirks;

import java.util.Map;

/**
 * Created by david on 12/13/15.
 */
public class SqlDroidQuirks extends NoQuirks {
	public SqlDroidQuirks() {
		super();
	}

	public SqlDroidQuirks(Map<Class, Converter> converters) {
		super(converters);
	}

	@Override
	public boolean returnGeneratedKeysByDefault() {
		return false;
	}
}

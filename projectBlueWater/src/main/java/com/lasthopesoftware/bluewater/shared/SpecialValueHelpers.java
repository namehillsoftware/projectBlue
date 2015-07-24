package com.lasthopesoftware.bluewater.shared;

/**
 * Created by david on 7/24/15.
 */
public class SpecialValueHelpers {
	public static String buildMagicPropertyName(Class c, String propertyName) {
		return c.getCanonicalName() + "." + propertyName;
	}
}

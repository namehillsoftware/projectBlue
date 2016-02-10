package com.lasthopesoftware.bluewater.shared;

/**
 * Created by david on 7/24/15.
 */
public class MagicPropertyBuilder {
	public static String buildMagicPropertyName(Class c, String propertyName) {
		return buildMagicPropertyName(c.getCanonicalName(), propertyName);
	}

	private final String canonicalName;

	public MagicPropertyBuilder(Class c) {
		this.canonicalName = c.getCanonicalName();
	}

	public String buildProperty(String propertyName) {
		return buildMagicPropertyName(canonicalName, propertyName);
	}

	private static String buildMagicPropertyName(String prefix, String propertyName) {
		return prefix + "." + propertyName;
	}
}

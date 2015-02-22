package com.lasthopesoftware.bluewater.shared;


public class XmlParsingHelpers {
	
	public static void HandleBadXml(StringBuilder currentSb, char[] ch, int start, int length) {
		if (ch.length > 0)
			currentSb.append(ch, start, length);
	}
}

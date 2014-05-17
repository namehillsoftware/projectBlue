package com.lasthopesoftware.bluewater.data.service.objects;

import java.util.List;

public class FileUtils {
	
	public static StringBuilder HandleBadXml(StringBuilder currentSb, char[] ch, int start, int length) {
		if (ch.length > 0) {
			if (currentSb == null) currentSb = new StringBuilder();
			if (ch[0] != '&') {
				String newValue = new String(ch, start, length);
				if (currentSb.length() < 1 || currentSb.lastIndexOf("&") != currentSb.length() - 1)
					currentSb = new StringBuilder(newValue);
				else
					currentSb.append(newValue);
			}
			else {
				currentSb.append(ch[0]);
			}
		}
		return currentSb;
	}
	
	public static void SetSiblings(List<File> files) {
		for (int i = 0; i < files.size(); i++) {
			SetSiblings(i, files);
		}
	}
	
	public static void SetSiblings(int position, List<File> files) {
		if (position > 0 && files.size() > 1) files.get(position).setPreviousFile(files.get(position - 1));
		if (position < files.size() - 1) files.get(position).setNextFile(files.get(position + 1));
	}
}

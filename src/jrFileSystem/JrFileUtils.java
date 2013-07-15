package jrFileSystem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JrFileUtils {
	public static <T extends JrObject> List<T> transformListing(Class<T> c, HashMap<String, String> listing) {
		List<T> returnList = new ArrayList<T>(listing.size());
		try {
			//for (int i = 0; i < listing.size(); i++) {
			for (Map.Entry<String, String> item : listing.entrySet()) {
				//Map.Entry<String, String> item = listing.entrySet().iterator().
				T newItem = c.newInstance();
				newItem.setKey(Integer.parseInt(item.getValue()));
				newItem.setValue(item.getKey());
				returnList.add(newItem);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return returnList;
	}
	
	public static <T extends JrObject> T createListing(Class<T> c) {
		T newItem = null;
		try {
			newItem = c.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return newItem;
	}
	
	public static String InputStreamToString(InputStream is) throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		StringBuilder total = new StringBuilder();
		String line;
		while ((line = r.readLine()) != null) {
		    total.append(line);
		}
		return total.toString();
	}
	
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
	
	public static void SetSiblings(List<JrFile> files) {
		for (int i = 0; i < files.size(); i++) {
			SetSiblings(i, files);
		}
	}
	
	public static void SetSiblings(int position, List<JrFile> files) {
		if (position > 0 && files.size() > 1) files.get(position).setPreviousFile(files.get(position - 1));
		if (position < files.size() - 1) files.get(position).setNextFile(files.get(position + 1));
	}
}

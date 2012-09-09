package jrFileSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class jrListing {
	public Integer key;
	public String value;
	
	public jrListing(int key, String value) {
		this.key = key;
		this.value = value;
	}
	
	public jrListing(String value) {
		this.key = null;
		this.value = value;
	}
	
	public jrListing() {
	}
	
	public static <T extends jrListing> List<T> transformListing(Class<T> c, Map<String, String> listing) {
		List<T> returnList = new ArrayList<T>();
		try {
			for (Map.Entry<String, String> item : listing.entrySet()) {
				T newItem = c.newInstance();
				newItem.key = Integer.parseInt(item.getValue());
				newItem.value = item.getKey();
				returnList.add(newItem);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return returnList;
	}
}

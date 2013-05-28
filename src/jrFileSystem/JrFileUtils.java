package jrFileSystem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.AsyncTask;

public class JrFileUtils {
	public static <T extends JrListing> List<T> transformListing(Class<T> c, HashMap<String, String> listing) {
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
	
	public static <T extends JrListing> T createListing(Class<T> c) {
		T newItem = null;
		try {
			newItem = c.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return newItem;
	}
	
	public static class SortJrListAsync<T extends JrListing> extends AsyncTask<List<T>, Void, List<T>> {
	
		private int partition(List<T> list, int left, int right) {
			int i = left, j = right;
			T tmp;
			String pivot = stripArticles(list.get((left + right) / 2).getValue());
	
			while (i <= j) {
				while (stripArticles(list.get(i).getValue()).compareTo(pivot) < 0)
					i++;
				while (stripArticles(list.get(j).getValue()).compareTo(pivot) > 0)
					j--;
				if (i <= j) {
					tmp = list.get(i);
					list.set(i, list.get(j));
					list.set(j, tmp);
					i++;
					j--;
				}
			}
	
			return i;
		}
		
		private String stripArticles(String input) {
			String lowerCaseInput = input.toLowerCase(); 
			if (lowerCaseInput.startsWith("a "))
				return input.substring(2);
			if (lowerCaseInput.startsWith("an "))
				return input.substring(3);
			if (lowerCaseInput.startsWith("the "))
				return input.substring(4);
			return input;
		}
		
		private void quickSort(List<T> list) {
			if (list.size() > 0)
				quickSort(list, 0, list.size() - 1);
		}
		
		private void quickSort(List<T> list, int left, int right) {
			int index = partition(list, left, right);
		      if (left < index - 1)
		            quickSort(list, left, index - 1);
		      if (index < right)
		            quickSort(list, index, right);
		}

		@Override
		protected List<T> doInBackground(List<T>... params) {
			quickSort(params[0]);
			return params[0];
		}

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
}

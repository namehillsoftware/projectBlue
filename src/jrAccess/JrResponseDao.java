package jrAccess;

import java.util.HashMap;
import java.util.Map;

public class JrResponseDao {

	private boolean status;
	private Map<String, String> items = new HashMap<String, String>();
	
	public JrResponseDao(String status) {
		this.status = status != null && status.equalsIgnoreCase("OK");
	}

	/**
	 * @return the status
	 */
	public boolean isStatus() {
		return status;
	}	

	/**
	 * @return the items
	 */
	public Map<String, String> getItems() {
		return items;
	}

	/**
	 * @param items the items to set
	 */
	public void setItems(Map<String, String> items) {
		this.items = items;
	}
	
}

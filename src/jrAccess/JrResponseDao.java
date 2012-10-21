package jrAccess;

import java.util.HashMap;
import java.util.Map;

public class JrResponseDao {

	private boolean status;
	public HashMap<String, String> items = new HashMap<String, String>();
	
	public JrResponseDao(String status) {
		this.status = status != null && status.equalsIgnoreCase("OK");
	}

	/**
	 * @return the status
	 */
	public boolean isStatus() {
		return status;
	}	
}

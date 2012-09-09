package jrFileSystem;

import java.util.List;

import jrAccess.GetJrResponse;
import jrAccess.JrSession;

public class jrCategory extends jrListing {
	public List<jrCategoryItem> CategoryItems;
	
	public jrCategory(int key, String value) {
		super(key, value);
		// TODO Auto-generated constructor stub
		setItems();
	}
	
//	public jrPage(String value) {
//		super(value);
//		// TODO Auto-generated constructor stub
//		setCategories();
//	}
	
	public jrCategory() {
		super();
		setItems();
	}
	
	public void setItems() {
		if (JrSession.accessDao == null) return;
		
		try {
			CategoryItems = jrListing.transformListing(jrCategoryItem.class, (new GetJrResponse()).execute(new String[] { JrSession.accessDao.getValidUrl(), "Browse/Children", "ID=" + String.valueOf(this.key) }).get().getItems());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

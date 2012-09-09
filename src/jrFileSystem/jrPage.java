package jrFileSystem;

import java.util.List;

import jrAccess.GetJrResponse;
import jrAccess.JrSession;

public class jrPage extends jrListing {
	public List<jrCategory> Categories;
	
	public jrPage(int key, String value) {
		super(key, value);
		// TODO Auto-generated constructor stub
		setCategories();
	}
	
//	public jrPage(String value) {
//		super(value);
//		// TODO Auto-generated constructor stub
//		setCategories();
//	}
	
	public jrPage() {
		super();
		setCategories();
	}
	
	public void setCategories() {
		if (JrSession.accessDao == null) return;
		
		try {
			Categories = jrListing.transformListing(jrCategory.class, (new GetJrResponse()).execute(new String[] { JrSession.accessDao.getValidUrl(), "Browse/Children", "ID=" + String.valueOf(this.key) }).get().getItems());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

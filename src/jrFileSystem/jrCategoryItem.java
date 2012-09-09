package jrFileSystem;

import java.util.List;

import jrAccess.GetJrResponse;
import jrAccess.JrSession;

public class jrCategoryItem extends jrListing {
	public List<jrFile> Files;
	
	public jrCategoryItem(int key, String value) {
		super(key, value);
		// TODO Auto-generated constructor stub
		setFiles();
	}
	
//	public jrPage(String value) {
//		super(value);
//		// TODO Auto-generated constructor stub
//		setCategories();
//	}
	
	public jrCategoryItem() {
		super();		
	}
	
	public void setFiles() {
		if (JrSession.accessDao == null) return;
		
		try {
			Files = jrListing.transformListing(jrFile.class, (new GetJrResponse()).execute(new String[] { JrSession.accessDao.getValidUrl(), "Browse/Children", "ID=" + String.valueOf(this.key) }).get().getItems());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

package jrFileSystem;

import java.util.ArrayList;
import java.util.List;

import jrAccess.GetJrResponse;
import jrAccess.JrSession;

public class jrPage extends jrListing {
	private List<jrCategory> mCategories;
	
	public jrPage(int key, String value) {
		super(key, value);
		// TODO Auto-generated constructor stub
	}
	
//	public jrPage(String value) {
//		super(value);
//		// TODO Auto-generated constructor stub
//		setCategories();
//	}
	
	public jrPage() {
		super();
	}
	
	public List<jrCategory> getCategories() {
		if (mCategories == null) {
			mCategories = new ArrayList<jrCategory>();
			
			if (JrSession.accessDao == null) return mCategories;
			
			try {
				mCategories = jrListing.transformListing(jrCategory.class, (new GetJrResponse()).execute(new String[] { JrSession.accessDao.getValidUrl(), "Browse/Children", "ID=" + String.valueOf(this.key), "Skip=1" }).get().getItems());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return mCategories;
	}
}

package jrFileSystem;

import java.util.ArrayList;
import java.util.List;

import jrAccess.GetJrResponse;
import jrAccess.JrSession;

public class jrCategory extends jrListing {
	private List<jrItem> mCategoryItems;
	
	public jrCategory(int key, String value) {
		super(key, value);
	}
	
//	public jrPage(String value) {
//		super(value);
//		// TODO Auto-generated constructor stub
//		setCategories();
//	}
	
	public jrCategory() {
		super();
	}
	
	public List<jrItem> getCategoryItems() {
		if (mCategoryItems == null) {
			mCategoryItems = new ArrayList<jrItem>();
			
			if (JrSession.accessDao == null) return mCategoryItems;
			
			try {
				mCategoryItems = jrListing.transformListing(jrItem.class, (new GetJrResponse()).execute(new String[] { JrSession.accessDao.getValidUrl(), "Browse/Children", "ID=" + String.valueOf(this.key) }).get().getItems());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return mCategoryItems;
	}

}

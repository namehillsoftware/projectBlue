package jrFileSystem;

import java.util.ArrayList;
import java.util.List;

import jrAccess.GetJrResponse;
import jrAccess.JrSession;

public class jrItem extends jrListing {
	public List<jrItem> mSubItems;
	
	public jrItem(int key, String value) {
		super(key, value);
		// TODO Auto-generated constructor stub
	}
	
//	public jrPage(String value) {
//		super(value);
//		// TODO Auto-generated constructor stub
//		setCategories();
//	}
	
	public jrItem() {
		super();		
	}
	
	public List<jrItem> getSubItems() {
		mSubItems = new ArrayList<jrItem>();
		
		if (mSubItems.size() < 1 && JrSession.accessDao != null) {
			try {
				mSubItems = jrListing.transformListing(jrItem.class, (new GetJrResponse()).execute(new String[] { JrSession.accessDao.getValidUrl(), "Browse/Children", "ID=" + String.valueOf(this.key) }).get().getItems());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return mSubItems;
	}
}

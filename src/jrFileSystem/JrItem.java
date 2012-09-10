package jrFileSystem;

import java.util.ArrayList;
import java.util.List;

import jrAccess.GetJrResponse;
import jrAccess.JrSession;

public class JrItem extends JrListing {
	public List<JrListing> mSubItems;
	
	public JrItem(int key, String value) {
		super(key, value);
		// TODO Auto-generated constructor stub
	}
	
//	public jrPage(String value) {
//		super(value);
//		// TODO Auto-generated constructor stub
//		setCategories();
//	}
	
	public JrItem() {
		super();		
	}
	
	public List<JrListing> getSubItems() {
		
		if (mSubItems == null) {
			mSubItems = new ArrayList<JrListing>();
			if (JrSession.accessDao == null) return mSubItems;
			try {
				List<JrItem> tempSubItems = JrFileUtils.transformListing(JrItem.class, (new GetJrResponse()).execute(new String[] { JrSession.accessDao.getValidUrl(), "Browse/Children", JrSession.accessDao.getToken(), "ID=" + String.valueOf(this.key), "Skip=1" }).get().getItems());
				mSubItems.addAll(tempSubItems);
				
				if (mSubItems.isEmpty()) {
					List<JrFile> tempFiles = JrFileUtils.transformListing(JrFile.class, (new GetJrResponse()).execute(new String[] { JrSession.accessDao.getValidUrl(), "Browse/Files", JrSession.accessDao.getToken(), "ID=" + String.valueOf(this.key), "Action=Serialize"}).get().getItems());
					mSubItems.addAll(tempFiles);
				}
				
				JrFileUtils.sortSubItems(mSubItems);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return mSubItems;
	}
}

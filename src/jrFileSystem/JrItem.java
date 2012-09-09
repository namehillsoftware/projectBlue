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
				List<JrItem> tempSubItems = JrFileUtils.transformListing(JrItem.class, (new GetJrResponse()).execute(new String[] { JrSession.accessDao.getValidUrl(), "Browse/Children", "ID=" + String.valueOf(this.key), "Skip=1" }).get().getItems());
				mSubItems.addAll(tempSubItems);
				
				boolean itemAlreadyExists = false;
				List<JrFile> tempFiles = JrFileUtils.transformListing(JrFile.class, (new GetJrResponse()).execute(new String[] { JrSession.accessDao.getValidUrl(), "Browse/Children", "ID=" + String.valueOf(this.key)}).get().getItems());
				for (JrFile file : tempFiles) {
					itemAlreadyExists = false;
					
					for (JrItem item : tempSubItems) {
						if (item.value.equals(file.value)) {
							itemAlreadyExists = true;
							break; 
						}
					}
					
					if (!itemAlreadyExists) mSubItems.add(file);
				}
				
				JrFileUtils.sortList(mSubItems);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return mSubItems;
	}
}

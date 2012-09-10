package jrFileSystem;

import java.util.ArrayList;
import java.util.List;

import jrAccess.GetJrResponse;
import jrAccess.JrSession;

public class JrPage extends JrListing {
	private List<JrCategory> mCategories;
	
	public JrPage(int key, String value) {
		super(key, value);
		// TODO Auto-generated constructor stub
	}
	
//	public jrPage(String value) {
//		super(value);
//		// TODO Auto-generated constructor stub
//		setCategories();
//	}
	
	public JrPage() {
		super();
	}
	
	public List<JrCategory> getCategories() {
		if (mCategories == null) {
			mCategories = new ArrayList<JrCategory>();
			
			if (JrSession.accessDao == null) return mCategories;
			
			try {
				mCategories = JrFileUtils.transformListing(JrCategory.class, (new GetJrResponse()).execute(new String[] { JrSession.accessDao.getValidUrl(), "Browse/Children", JrSession.accessDao.getToken(), "ID=" + String.valueOf(this.key), "Skip=1" }).get().getItems());
				
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return mCategories;
	}
}

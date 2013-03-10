package jrFileSystem;

import java.util.ArrayList;
import java.util.List;

import jrAccess.JrStdXmlResponse;
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
	
	public String getUrl() {
		return JrSession.accessDao.getJrUrl("Browse/Children", "ID=" + String.valueOf(this.mKey), "Skip=1");
	}
	
	public List<JrCategory> getCategories() {
		if (mCategories == null) {
			mCategories = new ArrayList<JrCategory>();
			
			if (JrSession.accessDao == null) return mCategories;
			
			try {
				mCategories = JrFileUtils.transformListing(JrCategory.class, (new JrStdXmlResponse()).execute("Browse/Children", "ID=" + String.valueOf(this.mKey), "Skip=1").get().items);
				
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return mCategories;
	}
}

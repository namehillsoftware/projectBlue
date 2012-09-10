package jrFileSystem;

import java.util.ArrayList;
import java.util.List;
import jrAccess.GetJrResponse;
import jrAccess.JrSession;



public class JrFileSystem extends JrListing {
	private List<JrPage> mPages;
	
//	public jrFileSystem(int key, String value) {
//		super(key, value);
//		// TODO Auto-generated constructor stub
//		setPages();
//	}
	
//	public jrFileSystem(String value) {
//		super(value);
//		// TODO Auto-generated constructor stub
//		setPages();
//	}
	
	public JrFileSystem() {
		super();
//		setPages();
	}
	
	public List<JrPage> getPages() {
		if (mPages == null) {
			mPages = new ArrayList<JrPage>();
			if (JrSession.accessDao == null) return mPages;

			try {
				mPages = JrFileUtils.transformListing(JrPage.class, (new GetJrResponse()).execute(new String[] { JrSession.accessDao.getValidUrl(), "Browse/Children", JrSession.accessDao.getToken() }).get().getItems());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return mPages;
	}
	
}


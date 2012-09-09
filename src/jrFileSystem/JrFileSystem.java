package jrFileSystem;

import java.util.List;
import jrAccess.GetJrResponse;
import jrAccess.JrSession;



public class JrFileSystem extends JrListing {
	public List<JrPage> Pages;
	
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
		setPages();
	}
	
	public void setPages() {
		if (JrSession.accessDao == null) return;
		
		try {
			Pages = JrFileUtils.transformListing(JrPage.class, (new GetJrResponse()).execute(new String[] { JrSession.accessDao.getValidUrl(), "Browse/Children" }).get().getItems());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}


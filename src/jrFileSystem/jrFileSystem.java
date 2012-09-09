package jrFileSystem;

import java.util.List;
import jrAccess.GetJrResponse;
import jrAccess.JrSession;



public class jrFileSystem extends jrListing {
	public List<jrPage> Pages;
	
//	public jrFileSystem(int key, String value) {
//		super(key, value);
//		// TODO Auto-generated constructor stub
//		setPages();
//	}
	
	public jrFileSystem(String value) {
		super(value);
		// TODO Auto-generated constructor stub
		setPages();
	}
	
	public jrFileSystem() {
		super();
		setPages();
	}
	
	public void setPages() {
		if (JrSession.accessDao == null) return;
		
		try {
			Pages = jrListing.transformListing(jrPage.class, (new GetJrResponse()).execute(new String[] { JrSession.accessDao.getValidUrl(), "Browse/Children" }).get().getItems());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}


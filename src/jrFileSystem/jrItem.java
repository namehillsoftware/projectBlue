package jrFileSystem;

import java.util.ArrayList;
import java.util.List;

import jrAccess.GetJrResponse;
import jrAccess.JrSession;

public class jrItem extends jrListing {
	public List<jrFile> mFiles;
	
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
	
	public List<jrFile> getFiles() {
		mFiles = new ArrayList<jrFile>();
		
		if (mFiles.size() < 1 && JrSession.accessDao != null) {
			try {
				mFiles = jrListing.transformListing(jrFile.class, (new GetJrResponse()).execute(new String[] { JrSession.accessDao.getValidUrl(), "Browse/Children", "ID=" + String.valueOf(this.key) }).get().getItems());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return mFiles;
	}
}

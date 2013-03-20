package jrFileSystem;

import java.util.ArrayList;
import java.util.List;

import jrAccess.JrFileXmlResponse;
import jrAccess.JrFsResponse;
import jrAccess.JrSession;

public class JrItem extends JrListing implements IJrItem<JrItem> {
	private ArrayList<JrItem> mSubItems;
	private ArrayList<JrFile> mFiles;
	
	public JrItem(int key, String value) {
		super(key, value);
		// TODO Auto-generated constructor stub
	}
	
	public JrItem() {
		super();		
	}
	
	public String getUrl() {
		return JrSession.accessDao.getJrUrl("Browse/Children", "ID=" + String.valueOf(this.getKey()), "Skip=1");
	}
	
	@Override
	public ArrayList<JrItem> getSubItems() {
		if (mSubItems != null) return mSubItems;
		
		mSubItems = new ArrayList<JrItem>();
		if (JrSession.accessDao == null) return mSubItems;
		try {
			List<JrItem> tempSubItems = (new JrFsResponse<JrItem>(JrItem.class)).execute( "Browse/Children", "ID=" + String.valueOf(this.getKey()), "Skip=1").get();
			mSubItems.addAll(tempSubItems);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return mSubItems;
	}
	
	@Override
	public ArrayList<JrFile> getFiles() {
		if (mFiles != null) return mFiles;
		
		mFiles = new ArrayList<JrFile>();
		try {
			List<JrFile> tempFiles = (new JrFileXmlResponse()).execute("Browse/Files", "ID=" + String.valueOf(this.getKey())).get(); 
			mFiles.addAll(tempFiles);
			for (JrFile file : mFiles) file.setSiblings(mFiles);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return mFiles;
	}
	
}

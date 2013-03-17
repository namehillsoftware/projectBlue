package jrFileSystem;

import java.util.ArrayList;
import java.util.List;

import jrAccess.JrFileXmlResponse;
import jrAccess.JrFsResponse;
import jrAccess.JrSession;

public class JrItem extends JrListing {
	private List<JrItem> mSubItems;
	private ArrayList<JrFile> mFiles;
	
	public JrItem(int key, String value) {
		super(key, value);
		// TODO Auto-generated constructor stub
	}
	
	public JrItem() {
		super();		
	}
	
	public String getUrl() {
		return JrSession.accessDao.getJrUrl("Browse/Children", "ID=" + String.valueOf(this.mKey), "Skip=1");
	}
	
	public List<JrItem> getSubItems() {
		if (mSubItems != null) return mSubItems;
		
		mSubItems = new ArrayList<JrItem>();
		if (JrSession.accessDao == null) return mSubItems;
		try {
			List<JrItem> tempSubItems = (new JrFsResponse<JrItem>(JrItem.class)).execute( "Browse/Children", "ID=" + String.valueOf(this.mKey), "Skip=1").get();
			mSubItems.addAll(tempSubItems);
			//mSubItems = (List<JrListing>) (new JrFileUtils.SortJrListAsync().execute(new List[] { mSubItems }).get());
			if (mSubItems.isEmpty()) mSubItems.add(new JrItem(this.mKey, getFiles().get(0).getAlbum()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return mSubItems;
	}
	
	public ArrayList<JrFile> getFiles() {
		if (mFiles != null) return mFiles;
		
		mFiles = new ArrayList<JrFile>();
		try {
			List<JrFile> tempFiles = (new JrFileXmlResponse()).execute("Browse/Files", "ID=" + String.valueOf(this.mKey)).get(); 
			mFiles.addAll(tempFiles);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return mFiles;
	}
	
}

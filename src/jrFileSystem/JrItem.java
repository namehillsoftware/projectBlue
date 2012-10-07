package jrFileSystem;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import jrAccess.GetJrNonXmlResponse;
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
					BufferedReader fileResult = new BufferedReader(new InputStreamReader((new GetJrNonXmlResponse()).execute(new String[] { JrSession.accessDao.getValidUrl(), "Browse/Files", JrSession.accessDao.getToken(), "ID=" + String.valueOf(this.key), "Action=Serialize"}).get()));
					mSubItems = parseFileList(fileResult.toString());
				}
				
				JrFileUtils.sortSubItems(mSubItems);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return mSubItems;
	}
	
	private List<JrListing> parseFileList(String semiColonFileString) {
		List<JrListing> returnFiles = new ArrayList<JrListing>();
		String[] fileArray = semiColonFileString.split(";");
		
		for (int i = 0; i < fileArray.length; i++)
			returnFiles.add(new JrFile(Integer.parseInt(fileArray[i])));
		
		return returnFiles;
	}
}

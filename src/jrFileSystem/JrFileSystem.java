package jrFileSystem;

import java.util.ArrayList;
import java.util.List;

import jrAccess.JrStdXmlResponse;
import jrAccess.JrSession;



public class JrFileSystem extends JrListing implements IJrItem<JrItem> {
	private ArrayList<JrItem> mPages;
	
	public JrFileSystem() {
		super();
//		setPages();
	}
	
	public String getUrl() {
		return JrSession.accessDao.getJrUrl("Browse/Children");
	}
	
	@Override
	public ArrayList<JrItem> getSubItems() {
		if (mPages == null) {
			mPages = new ArrayList<JrItem>();
			if (JrSession.accessDao == null) return mPages;
			
			List<JrItem> tempItems;
			try {
				tempItems = JrFileUtils.transformListing(JrItem.class, (new JrStdXmlResponse()).execute("Browse/Children").get().items);
				mPages = new ArrayList<JrItem>(tempItems.size());
				mPages.addAll(tempItems);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return mPages;
	}

	@Override
	public ArrayList<JrFile> getFiles() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<JrFile> getFiles(int option) {
		// TODO Auto-generated method stub
		return null;
	}
	
}


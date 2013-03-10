package jrFileSystem;

import java.util.ArrayList;
import java.util.List;
import jrAccess.JrFsResponse;
import jrAccess.JrSession;

public class JrCategory extends JrListing {
	private List<JrItem> mCategoryItems;
	private List<JrItem> mSortedCategoryItems;
	
	public JrCategory(int key, String value) {
		super(key, value);
	}
	
//	public jrPage(String value) {
//		super(value);
//		// TODO Auto-generated constructor stub
//		setCategories();
//	}
	
	public JrCategory() {
		super();
	}
	
	public String getUrl() {
		return JrSession.accessDao.getJrUrl("Browse/Children", "ID=" + String.valueOf(this.mKey));
	}
	
	public List<JrItem> getCategoryItems() {
		if (mCategoryItems == null) {
			mCategoryItems = new ArrayList<JrItem>(0);
			
			if (JrSession.accessDao == null) return mCategoryItems;
			
			try {
				mCategoryItems = (List<JrItem>) (new JrFsResponse<JrItem>(JrItem.class)).execute("Browse/Children", "ID=" + String.valueOf(this.mKey)).get();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return mCategoryItems;
	}
	
	public List<JrItem> getSortedCategoryItems() {
		if (mSortedCategoryItems == null) {
			try {
				mSortedCategoryItems = (new JrFileUtils.SortJrListAsync<JrItem>().execute(getCategoryItems()).get());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return mSortedCategoryItems;
	}

}

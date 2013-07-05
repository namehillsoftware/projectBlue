package jrFileSystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jrFileSystem.IJrDataTask.OnCompleteListener;
import jrFileSystem.IJrDataTask.OnConnectListener;
import jrFileSystem.IJrDataTask.OnErrorListener;
import jrFileSystem.IJrDataTask.OnStartListener;
import android.annotation.SuppressLint;

@SuppressLint("UseSparseArrays")
public class JrPlaylist extends JrItemAsyncBase<JrPlaylist> implements IJrItem<JrPlaylist>, IJrFilesContainer {
	private HashMap<Integer, JrPlaylist> mSubItems;
	private JrPlaylist mParent = null;
	private String mPath;
	private String mGroup;
	private JrFiles mJrFiles;
	
	public JrPlaylist() {
		super();
	}
	
	public JrPlaylist(int key) {
		setKey(key);
	}

	public void setParent(JrPlaylist parent) {
		mParent = parent;
	}
	
	public JrPlaylist getParent() {
		return mParent;
	}
	
	@Override
	public ArrayList<JrPlaylist> getSubItems() {
		if (mSubItems == null) mSubItems = new HashMap<Integer, JrPlaylist>();
		ArrayList<JrPlaylist> returnList = new ArrayList<JrPlaylist>(mSubItems.size());
		returnList.addAll(mSubItems.values());
		return returnList;
	}
	
	public void addPlaylist(JrPlaylist playlist) {
		if (mSubItems == null) mSubItems = new HashMap<Integer, JrPlaylist>();
		playlist.setParent(this);
		mSubItems.put(playlist.getKey(), playlist);
	}
	
//	@Override
	// Get a new list each time with playlists since 
	// they can often change dynamically
//	public ArrayList<JrFile> getFiles() {
//		ArrayList<JrFile> returnFiles = new ArrayList<JrFile>();
//		try {
//			List<JrFile> tempFiles = getNewFilesTask().execute("Playlist/Files", "Playlist=" + String.valueOf(this.getKey()), "Fields=Key,Name").get();
//			returnFiles = new ArrayList<JrFile>(tempFiles.size());
//			for (int i = 0; i < tempFiles.size(); i++) {
//				JrFileUtils.SetSiblings(i, tempFiles);
//				returnFiles.add(tempFiles.get(i));
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} 
//			
//		return returnFiles;
//	}
	
//	public ArrayList<JrFile> getFiles(int option) {
//		ArrayList<JrFile> returnFiles = new ArrayList<JrFile>();
//		try {
//			List<JrFile> tempFiles = getNewFilesTask().execute("Playlist/Files", "Playlist=" + String.valueOf(this.getKey()), "Shuffle=1", "Fields=Key,Name").get();
//			returnFiles = new ArrayList<JrFile>(tempFiles.size());
//			for (int i = 0; i < tempFiles.size(); i++) {
//				JrFileUtils.SetSiblings(i, tempFiles);
//				returnFiles.add(tempFiles.get(i));
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} 
//			
//		return returnFiles;
//	}

	/**
	 * @return the mPath
	 */
	public String getPath() {
		return mPath;
	}

	/**
	 * @param mPath the mPath to set
	 */
	public void setPath(String mPath) {
		this.mPath = mPath;
	}

	/**
	 * @return the mGroup
	 */
	public String getGroup() {
		return mGroup;
	}

	/**
	 * @param mGroup the mGroup to set
	 */
	public void setGroup(String mGroup) {
		this.mGroup = mGroup;
	}

	@Override
	public void getSubItemsAsync() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setOnItemsCompleteListener(OnCompleteListener<List<JrPlaylist>> listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setOnItemsStartListener(OnStartListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setOnItemsErrorListener(OnErrorListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected OnConnectListener<List<JrPlaylist>> getOnItemConnectListener() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected List<OnCompleteListener<List<JrPlaylist>>> getOnItemsCompleteListeners() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected List<OnStartListener> getOnItemsStartListeners() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected List<OnErrorListener> getOnItemsErrorListeners() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String[] getSubItemParams() {
		return new String[] { "Playlist/Files", "Playlist=" + String.valueOf(this.getKey()) };
	}

	@Override
	public IJrItemFiles getJrFiles() {
		if (mJrFiles == null) mJrFiles = new JrFiles("Playlist/Files", "Playlist=" + String.valueOf(this.getKey()));
		return mJrFiles;
	}
}

package jrFileSystem;

import android.annotation.SuppressLint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import jrAccess.JrFileXmlResponse;
import jrAccess.JrSession;

@SuppressLint("UseSparseArrays")
public class JrPlaylist extends JrListing implements IJrItem<JrPlaylist> {
	private HashMap<Integer, JrPlaylist> mSubItems;
	private JrPlaylist mParent = null;
	private String mPath;
	private String mGroup;
	
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
	
	@Override
	// Get a new list each time with playlists since 
	// they can often change dynamically
	public ArrayList<JrFile> getFiles() {
		ArrayList<JrFile> returnFiles = new ArrayList<JrFile>();
		try {
			List<JrFile> tempFiles = (new JrFileXmlResponse()).execute("Playlist/Files", "Playlist=" + String.valueOf(this.getKey())).get();
			returnFiles = new ArrayList<JrFile>(tempFiles.size());
			returnFiles.addAll(tempFiles);
			for (JrFile file : returnFiles) file.setSiblings(returnFiles);
		} catch (Exception e) {
			e.printStackTrace();
		} 
			
		return returnFiles;
	}
	
	public ArrayList<JrFile> getFiles(int option) {
		ArrayList<JrFile> returnFiles = new ArrayList<JrFile>();
		try {
			List<JrFile> tempFiles = (new JrFileXmlResponse()).execute("Playlist/Files", "Playlist=" + String.valueOf(this.getKey()), "Shuffle=1").get();
			returnFiles = new ArrayList<JrFile>(tempFiles.size());
			returnFiles.addAll(tempFiles);
			for (JrFile file : returnFiles) file.setSiblings(returnFiles);
		} catch (Exception e) {
			e.printStackTrace();
		} 
			
		return returnFiles;
	}

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
	public String getUrl() {
		return JrSession.accessDao.getJrUrl("Playlist/Files", "Playlist=" + String.valueOf(this.getKey()));
	}
}

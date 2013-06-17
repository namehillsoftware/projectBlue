package jrFileSystem;

import java.util.ArrayList;

public interface IJrItem<T extends JrListing> {
	public static int GET_SHUFFLED = 1;
//	ArrayList<T> getSubItems();
	ArrayList<JrFile> getFiles();
	ArrayList<JrFile> getFiles(int option);
	Integer getKey();
	void setKey(Integer mKey);
	String getSubItemUrl();
	String[] getSubItemParams();
	String getValue();
	void setValue(String mValue);		
}

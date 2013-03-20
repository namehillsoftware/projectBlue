package jrFileSystem;

import java.util.ArrayList;

public interface IJrItem<T extends JrListing> {
	ArrayList<T> getSubItems();
	ArrayList<JrFile> getFiles();
	Integer getKey();
	void setKey(Integer mKey);
	String getUrl();
	String getValue();
	void setValue(String mValue);		
}

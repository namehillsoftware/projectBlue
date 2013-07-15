package jrFileSystem;

import java.util.ArrayList;

public interface IJrItem<T extends JrObject> {
	ArrayList<T> getSubItems();
	Integer getKey();
	void setKey(Integer mKey);
	String getValue();
	void setValue(String mValue);		
}

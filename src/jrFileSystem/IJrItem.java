package jrFileSystem;

import java.util.ArrayList;

public interface IJrItem {
	ArrayList<JrItem> getSubItems();
	ArrayList<JrFile> getFiles();
}

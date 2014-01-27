package com.lasthopesoftware.bluewater.data.sqlite.objects;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "LIBRARY_VIEWS")
public class LibraryView {

	@DatabaseField(generatedId = true)
	private Integer id;
	@DatabaseField(foreign = true, foreignAutoCreate = true)
	private Library library;
	@DatabaseField(foreign = true, foreignAutoCreate = true)
	private View view;
	
	/**
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(Integer id) {
		this.id = id;
	}
	/**
	 * @return the library
	 */
	public Library getLibrary() {
		return library;
	}
	/**
	 * @param library the library to set
	 */
	public void setLibrary(Library library) {
		this.library = library;
	}
	/**
	 * @return the view
	 */
	public View getView() {
		return view;
	}
	/**
	 * @param view the view to set
	 */
	public void setView(View view) {
		this.view = view;
	}
}

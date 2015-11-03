package com.lasthopesoftware.bluewater.servers.library.items.list.menus.changes.handlers;

import com.lasthopesoftware.bluewater.servers.library.items.list.menus.changes.OnAllMenusHidden;
import com.lasthopesoftware.bluewater.servers.library.items.list.menus.changes.OnAnyMenuShown;
import com.lasthopesoftware.bluewater.servers.library.items.menu.OnViewChangedListener;

/**
 * Created by david on 11/2/15.
 */
public interface IItemListMenuChangeHandler extends OnViewChangedListener, OnAllMenusHidden, OnAnyMenuShown {
    void onAllMenusHidden();
    void onAnyMenuShown();
}

package com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.handlers

import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.OnAllMenusHidden
import com.lasthopesoftware.bluewater.client.browsing.items.list.menus.changes.OnAnyMenuShown
import com.lasthopesoftware.bluewater.client.browsing.items.menu.OnViewChangedListener

/**
 * Created by david on 11/2/15.
 */
interface IItemListMenuChangeHandler : OnViewChangedListener, OnAllMenusHidden, OnAnyMenuShown

package com.lasthopesoftware.bluewater.client.browsing.library.items.access;

import com.lasthopesoftware.bluewater.client.browsing.library.items.Item;
import com.namehillsoftware.handoff.promises.Promise;

import java.util.List;

public interface ProvideItems {

	Promise<List<Item>> promiseItems(int itemKey);
}

package com.lasthopesoftware.promises;


public interface ProducePromise<Input, PromisedResult> {
	Promise<PromisedResult> producePromise(Input input);
}

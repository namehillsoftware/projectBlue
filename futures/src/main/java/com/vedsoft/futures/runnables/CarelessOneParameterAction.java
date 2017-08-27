package com.vedsoft.futures.runnables;


public interface CarelessOneParameterAction<Parameter> {
	void runWith(Parameter parameter) throws Throwable;
}

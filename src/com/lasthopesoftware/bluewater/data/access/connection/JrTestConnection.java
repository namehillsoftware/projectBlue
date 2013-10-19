package com.lasthopesoftware.bluewater.data.access.connection;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import com.lasthopesoftware.bluewater.data.access.JrResponse;

public class JrTestConnection implements Callable<Boolean> {
	
	private static int testTimoutTime = 30;
	private static int stdTimeoutTime = 30000;
	
	@Override
	public Boolean call() throws Exception {
		Boolean result = Boolean.FALSE;
		
		try {
			JrConnection conn = new JrConnection("Alive");
	    	conn.setConnectTimeout(stdTimeoutTime);
			JrResponse responseDao = JrResponse.fromInputStream(conn.getInputStream());
	    	
	    	result = responseDao != null && responseDao.isStatus() ? Boolean.TRUE : Boolean.FALSE;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public static boolean doTest() {
		try {
			FutureTask<Boolean> statusTask = new FutureTask<Boolean>(new JrTestConnection());
			Thread statusThread = new Thread(statusTask);
			statusThread.setName("Checking connection status");
			statusThread.setPriority(Thread.MIN_PRIORITY);
			statusThread.start();
			return statusTask.get().booleanValue();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
}

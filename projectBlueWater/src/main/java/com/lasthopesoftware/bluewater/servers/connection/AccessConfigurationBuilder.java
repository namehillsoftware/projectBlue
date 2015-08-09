package com.lasthopesoftware.bluewater.servers.connection;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;

import com.lasthopesoftware.bluewater.shared.StandardRequest;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTask;

import org.apache.commons.io.IOUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import xmlwise.XmlElement;
import xmlwise.Xmlwise;

/**
 * Created by david on 8/8/15.
 */
public class AccessConfigurationBuilder {
	public static void buildConfiguration(Context context, String accessString, String authCode, boolean isLocalOnly, ISimpleTask.OnCompleteListener<Integer, Void, Boolean> onBuildComplete) {
		buildConfiguration(context, accessString, authCode, isLocalOnly, stdTimeoutTime, onBuildComplete);
	}

	public static void buildConfiguration(Context context, String accessString, String authCode, boolean isLocalOnly,int timeout, final ISimpleTask.OnCompleteListener<Integer, Void, Boolean> onBuildComplete) throws NullPointerException {
		if (accessString == null)
			throw new NullPointerException("The access string cannot be null.");

		mAccessString = accessString;
		mIsLocalOnly = isLocalOnly;
		synchronized (syncObj) {
			mAuthCode = authCode;
			if (timeout <= 0) timeout = stdTimeoutTime;
			try {

				final NetworkInfo networkInfo = getActiveNetworkInfo(context);
				if (networkInfo == null || !networkInfo.isConnected()) {
					executeReturnFalseTask(onBuildComplete);
					return;
				}

				buildAccessConfiguration(mAccessString, mIsLocalOnly, timeout, new ISimpleTask.OnCompleteListener<String, Void, AccessConfiguration>() {

					@Override
					public void onComplete(ISimpleTask<String, Void, AccessConfiguration> owner, AccessConfiguration result) {
						synchronized (syncObj) {
							mAccessConfiguration = result;
						}

						if (mAccessConfiguration == null) {
							executeReturnFalseTask(onBuildComplete);
							return;
						}

						ConnectionTester.doTest(onBuildComplete);
					}
				});
			} catch (NullPointerException ne) {
				throw ne;
			}
		}
	}

	private static void executeReturnFalseTask(ISimpleTask.OnCompleteListener<Integer, Void, Boolean> onReturnFalseListener) {
		final SimpleTask<Integer, Void, Boolean> returnFalseTask = new SimpleTask<>(new ISimpleTask.OnExecuteListener<Integer, Void, Boolean>() {

			@Override
			public Boolean onExecute(ISimpleTask<Integer, Void, Boolean> owner, Integer... params) throws Exception {
				return Boolean.FALSE;
			}

		});

		returnFalseTask.addOnCompleteListener(onReturnFalseListener);
		returnFalseTask.execute(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static void refreshConfiguration(Context context, ISimpleTask.OnCompleteListener<Integer, Void, Boolean> onRefreshComplete) throws NullPointerException  {
		refreshConfiguration(context, -1, onRefreshComplete);
	}

	public static void refreshConfiguration(final Context context, final int timeout, final ISimpleTask.OnCompleteListener<Integer, Void, Boolean> onRefreshComplete) throws NullPointerException  {
		if (mAccessConfiguration == null) {
			if (mAccessString == null || mAccessString.isEmpty())
				throw new NullPointerException("The static access string has been lost. Please reset the connection session.");

			buildConfiguration(context, mAccessString, mAuthCode, mIsLocalOnly, timeout, onRefreshComplete);
			return;
		}

		final ISimpleTask.OnCompleteListener<Integer, Void, Boolean> mTestConnectionCompleteListener = new ISimpleTask.OnCompleteListener<Integer, Void, Boolean>() {

			@Override
			public void onComplete(ISimpleTask<Integer, Void, Boolean> owner, Boolean result) {
				if (result == Boolean.TRUE) {
					onRefreshComplete.onComplete(owner, result);
					return;
				}

				buildConfiguration(context, mAccessString, mAuthCode, mIsLocalOnly, timeout, onRefreshComplete);
			}

		};

		if (timeout > 0)
			ConnectionTester.doTest(timeout, mTestConnectionCompleteListener);
		else
			ConnectionTester.doTest(mTestConnectionCompleteListener);
	}


	private static void buildAccessConfiguration(String accessString, final boolean isLocalOnly, final int timeout, ISimpleTask.OnCompleteListener<String, Void, AccessConfiguration> onGetAccessComplete) throws NullPointerException {
		if (accessString == null)
			throw new NullPointerException("The access string cannot be null");

		final SimpleTask<String, Void, AccessConfiguration> mediaCenterAccessTask = new SimpleTask<>(new ISimpleTask.OnExecuteListener<String, Void, AccessConfiguration>() {

			@Override
			public AccessConfiguration onExecute(ISimpleTask<String, Void, AccessConfiguration> owner, String... params) throws Exception {
				try {
					final AccessConfiguration accessDao = new AccessConfiguration();
					String accessString = params[0];
					if (accessString.contains(".")) {
						if (!accessString.contains(":")) accessString += ":80";
						if (!accessString.startsWith("http://")) accessString = "http://" + accessString;
					}

					if (UrlValidator.getInstance().isValid(accessString)) {
						final Uri jrUrl = Uri.parse(accessString);
						accessDao.setRemoteIp(jrUrl.getHost());
						accessDao.setPort(jrUrl.getPort());
						accessDao.setStatus(true);

						return accessDao;
					}

					final HttpURLConnection conn = (HttpURLConnection)(new URL("http://webplay.jriver.com/libraryserver/lookup?id=" + accessString)).openConnection();

					conn.setConnectTimeout(timeout);
					try {
						final InputStream is = conn.getInputStream();
						try {
							final XmlElement xml = Xmlwise.createXml(IOUtils.toString(is));
							accessDao.setStatus(xml.getAttribute("Status").equalsIgnoreCase("OK"));
							accessDao.setPort(Integer.parseInt(xml.getUnique("port").getValue()));
							accessDao.setRemoteIp(xml.getUnique("ip").getValue());
							accessDao.setLocalOnly(isLocalOnly);
							for (String localIp : xml.getUnique("localiplist").getValue().split(","))
								accessDao.getLocalIps().add(localIp);
							for (String macAddress : xml.getUnique("macaddresslist").getValue().split(","))
								accessDao.getMacAddresses().add(macAddress);
						} finally {
							is.close();
						}
					} finally {
						conn.disconnect();
					}

					return accessDao;
				} catch (IOException i) {
					mLogger.error(i.getMessage());
				} catch (Exception e) {
					mLogger.warn(e.toString());
				}

				return null;
			}
		});

		if (onGetAccessComplete != null)
			mediaCenterAccessTask.addOnCompleteListener(onGetAccessComplete);

		mediaCenterAccessTask.execute(AsyncTask.THREAD_POOL_EXECUTOR, accessString);
	}

	private static class ConnectionTester {

		private static final Logger mLogger = LoggerFactory.getLogger(ConnectionTester.class);

		public static void doTest(ISimpleTask.OnCompleteListener<Integer, Void, Boolean> onTestComplete) {
			doTest(stdTimeoutTime, onTestComplete);
		}

		public static void doTest(int timeout, ISimpleTask.OnCompleteListener<Integer, Void, Boolean> onTestComplete) {
			final SimpleTask<Integer, Void, Boolean> connectionTestTask = new SimpleTask<>(new ISimpleTask.OnExecuteListener<Integer, Void, Boolean>() {

				@Override
				public Boolean onExecute(ISimpleTask<Integer, Void, Boolean> owner, Integer... params) throws Exception {
					Boolean result = Boolean.FALSE;

					final HttpURLConnection conn = getActiveConnection("Alive");
					if (conn == null) return result;

					try {
						conn.setConnectTimeout(params[0]);
						final InputStream is = conn.getInputStream();
						try {
							final StandardRequest responseDao = StandardRequest.fromInputStream(is);

							result = responseDao != null && responseDao.isStatus();
						} finally {
							is.close();
						}
					} catch (IOException | IllegalArgumentException e) {
						mLogger.warn(e.getMessage());
					} finally {
						conn.disconnect();
					}

					return result;
				}

			});

			if (onTestComplete != null)
				connectionTestTask.addOnCompleteListener(onTestComplete);

			connectionTestTask.execute(AsyncTask.THREAD_POOL_EXECUTOR, timeout);
		}
	}
}

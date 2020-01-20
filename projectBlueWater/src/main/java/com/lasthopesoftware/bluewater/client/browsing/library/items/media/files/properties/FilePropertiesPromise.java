package com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.properties;

import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.ServiceFile;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.properties.repository.FilePropertiesContainer;
import com.lasthopesoftware.bluewater.client.browsing.library.items.media.files.properties.repository.IFilePropertiesContainerRepository;
import com.lasthopesoftware.bluewater.client.connection.IConnectionProvider;
import com.lasthopesoftware.bluewater.shared.UrlKeyHolder;
import com.lasthopesoftware.resources.scheduling.ParsingScheduler;
import com.namehillsoftware.handoff.promises.Promise;
import com.namehillsoftware.handoff.promises.propagation.CancellationProxy;
import com.namehillsoftware.handoff.promises.queued.QueuedPromise;
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellableMessageWriter;
import com.namehillsoftware.handoff.promises.queued.cancellation.CancellationToken;
import com.namehillsoftware.handoff.promises.response.PromisedResponse;
import com.namehillsoftware.handoff.promises.response.VoidResponse;

import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Response;
import okhttp3.ResponseBody;
import xmlwise.XmlElement;
import xmlwise.XmlParseException;
import xmlwise.Xmlwise;

final class FilePropertiesPromise extends Promise<Map<String, String>> {

	FilePropertiesPromise(IConnectionProvider connectionProvider, IFilePropertiesContainerRepository filePropertiesContainerProvider, ServiceFile serviceFile, Integer serverRevision) {

		final CancellationProxy cancellationProxy = new CancellationProxy();
		respondToCancellation(cancellationProxy);

		final Promise<Response> filePropertiesResponse = connectionProvider.promiseResponse("File/GetInfo", "File=" + serviceFile.getKey());
		cancellationProxy.doCancel(filePropertiesResponse);

		final Promise<Map<String, String>> promisedProperties = filePropertiesResponse
			.eventually(new FilePropertiesWriter(connectionProvider, filePropertiesContainerProvider, serviceFile, serverRevision));

		cancellationProxy.doCancel(promisedProperties);

		promisedProperties.then(new VoidResponse<>(this::resolve), new VoidResponse<>(this::reject));
	}

	private static final class FilePropertiesWriter implements PromisedResponse<Response, Map<String, String>>, CancellableMessageWriter<Map<String, String>> {

		private final IConnectionProvider connectionProvider;
		private final ServiceFile serviceFile;
		private final Integer serverRevision;
		private final IFilePropertiesContainerRepository filePropertiesContainerProvider;

		private Response response;

		FilePropertiesWriter(IConnectionProvider connectionProvider, IFilePropertiesContainerRepository filePropertiesContainerProvider, ServiceFile serviceFile, Integer serverRevision) {
			this.connectionProvider = connectionProvider;
			this.serviceFile = serviceFile;
			this.serverRevision = serverRevision;
			this.filePropertiesContainerProvider = filePropertiesContainerProvider;
		}

		@Override
		public Map<String, String> prepareMessage(CancellationToken cancellationToken) throws Throwable {
			if (cancellationToken.isCancelled()) return new HashMap<>();

			final ResponseBody body = response.body();
			if (body == null) return new HashMap<>();

			try {
				final XmlElement xml = Xmlwise.createXml(body.string());
				final XmlElement parent = xml.get(0);

				final HashMap<String, String> returnProperties = new HashMap<>(parent.size());
				for (final XmlElement el : parent)
					returnProperties.put(el.getAttribute("Name"), el.getValue());

				final UrlKeyHolder<ServiceFile> urlKeyHolder = new UrlKeyHolder<>(connectionProvider.getUrlProvider().getBaseUrl(), serviceFile);
				filePropertiesContainerProvider.putFilePropertiesContainer(urlKeyHolder, new FilePropertiesContainer(serverRevision, returnProperties));

				return returnProperties;
			} catch (IOException | XmlParseException e) {
				LoggerFactory.getLogger(SessionFilePropertiesProvider.class).error(e.toString(), e);
				throw e;
			} finally {
				body.close();
			}
		}

		@Override
		public Promise<Map<String, String>> promiseResponse(Response response) {
			this.response = response;
			return new QueuedPromise<>(this, ParsingScheduler.instance().getScheduler());
		}
	}
}

package com.lasthopesoftware.bluewater.client.connection.waking.GivenALibrary;

import com.lasthopesoftware.bluewater.client.browsing.library.repository.LibraryId;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.LookupServers;
import com.lasthopesoftware.bluewater.client.connection.builder.lookup.ServerInfo;
import com.lasthopesoftware.bluewater.client.connection.waking.AlarmConfiguration;
import com.lasthopesoftware.bluewater.client.connection.waking.MachineAddress;
import com.lasthopesoftware.bluewater.client.connection.waking.PokeServer;
import com.lasthopesoftware.bluewater.client.connection.waking.ServerAlarm;
import com.lasthopesoftware.bluewater.shared.promises.extensions.FuturePromise;
import com.namehillsoftware.handoff.promises.Promise;

import org.joda.time.Duration;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import kotlin.Unit;

import static org.assertj.core.api.Assertions.assertThat;

public class WhenWakingALibraryServer {

	private static final MachineAddress[] expectedPokedMachineAddresses = new MachineAddress[] {
		new MachineAddress("local-address", "AB-E0-9F-24-F5"),
		new MachineAddress("local-address", "99-53-7F-2C-A1"),
		new MachineAddress("second-local-address", "AB-E0-9F-24-F5"),
		new MachineAddress("second-local-address", "99-53-7F-2C-A1"),
		new MachineAddress("remote-address", "AB-E0-9F-24-F5"),
		new MachineAddress("remote-address", "99-53-7F-2C-A1")
	};

	private static final List<MachineAddress> pokedMachineAddresses = new ArrayList<>();

	@BeforeClass
	public static void before() throws ExecutionException, InterruptedException {
		final LookupServers servers = libraryId ->
			new Promise<>(new ServerInfo(
				5001,
				5002,
				"remote-address",
				Arrays.asList("local-address", "second-local-address"),
				Arrays.asList("AB-E0-9F-24-F5", "99-53-7F-2C-A1"),
				null));

		final PokeServer pokeServer = (machineAddress, times, duration) -> {
			if (times == 10 && Duration.standardHours(10).equals(duration)) {
				pokedMachineAddresses.add(machineAddress);
			}
			return new Promise<>(Unit.INSTANCE);
		};

		final ServerAlarm serverAlarm = new ServerAlarm(
			servers,
			pokeServer,
			new AlarmConfiguration(10, Duration.standardHours(10)));
		new FuturePromise<>(serverAlarm.awakeLibraryServer(new LibraryId(14))).get();
	}

	@Test
	public void thenTheMachineIsAlertedAtAllEndpoints() {
		assertThat(pokedMachineAddresses).containsExactlyInAnyOrder(expectedPokedMachineAddresses);
	}
}

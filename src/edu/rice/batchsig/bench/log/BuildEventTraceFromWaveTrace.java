package edu.rice.batchsig.bench.log;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;

import edu.rice.batchsig.bench.log.LogonLogoffEvent.State;

public class BuildEventTraceFromWaveTrace {
	final static int SERVER_COUNT = 4;
	public BuildEventTraceFromWaveTrace () {
		//input = new BufferedReader(new InputStreamReader(System.in));
		for (int i = 0 ; i < SERVER_COUNT ; i++) {
			sender_servers[i] = new Object();
			recipient_servers[i] = new Object();
		}
	}
	
	class WaveEvent {
		final String wave;
		final String user;
		final int timestamp;
		WaveEvent(String wave, String user, int timestamp) {
			this.wave = wave;
			this.user = user;
			this.timestamp = timestamp;
		}
	}
	
	void parse(BufferedReader input) throws IOException {
		ArrayList<WaveEvent> events = new ArrayList<WaveEvent>();
		String line;
		int progress = 0;
		while ((line = input.readLine()) != null) {
			if (progress++ % 1000000 == 0)
				System.err.println("Parsing "+(progress-1));
			Iterator<String> it = Splitter.on(',').split(line).iterator();
			String wave = it.next().intern();
			String user = it.next().intern();
			int timestamp = Integer.parseInt(it.next());
			events.add(new WaveEvent(wave,user,timestamp));
		}	
		System.err.println("Done reading");

		build(events);
	}
	HashSet<String> allWaves = new HashSet<String>();
	IntSet allUsers = new IntOpenHashSet();
	HashMap<String,IntSet> usersForWave = new HashMap<String,IntSet>();
	EventTrace allEvents;
	Object recipient_servers[] = new Object[SERVER_COUNT];
	Object sender_servers[] = new Object[SERVER_COUNT];

	void build(ArrayList<WaveEvent> events) {
		int progress = 0;
		HashSet<String> usersTmp = new HashSet<String>();
		HashMap<String,Integer> usersMap = new HashMap<String,Integer>();
		for (WaveEvent e : events) {
			allWaves.add(e.wave);
			usersTmp.add(e.user);
		}

		int n = 10000; // User numbers start at 10000
		for (String s : usersTmp) {
			allUsers.add(n);
			usersMap.put(s,n++);
		}
		for (WaveEvent e : events) {
			allWaves.add(e.wave);
		}
			
		for (String wave : allWaves) {
			usersForWave.put(wave, new IntOpenHashSet());
		}
		for (WaveEvent e : events) {
			usersForWave.get(e.wave).add(usersMap.get(e.user));
		}
		ArrayList<MessageEvent> all = new ArrayList<MessageEvent>();
		for (WaveEvent e : events) {
			//System.out.println("Users: "+usersForWave.get(nowOne.wave).size());
			for (Integer recipient_user : usersForWave.get(e.wave)) {
				if (progress++ % 1000000 == 0)
					System.err.println("Processing "+(progress-1));
				all.add(new MessageEvent(
						Math.abs(e.user.hashCode())%SERVER_COUNT,
						Math.abs(recipient_user.hashCode())%SERVER_COUNT,
						usersMap.get(e.user),
						recipient_user,e.timestamp,-1));
			}
		}
		allEvents = new EventTrace(all);
	}

	static final long MIN = 60*1000;
	static final State LOGOFF = LogonLogoffEvent.State.LOGOFF;
	static final State LOGON = LogonLogoffEvent.State.LOGON;

	ArrayList<LogonLogoffEvent> determineLogins() {
		ArrayList<LogonLogoffEvent> logsMap = new ArrayList<LogonLogoffEvent>();
		HashMap<Integer,Long> lastMap = new HashMap<Integer,Long>();
		HashMap<Integer,Boolean> onlineMap = new HashMap<Integer,Boolean>();

		for (Integer user : allUsers) {
			//System.err.println("User_CreatingSet "+user);
			lastMap.put(user, -1000000000L);
			onlineMap.put(user, false);
		}
		long nowOn=-2000000000;

		for (MessageEvent event : allEvents) {
			Integer user = (Integer) event.sender_user;
			long lastOn = lastMap.get(user);
			nowOn = event.getTimestamp();
			boolean isOnline = onlineMap.get(user);

			long delta = (nowOn-lastOn)/1000;

			if (isOnline) {
				if (delta > 5*60) {
					isOnline = false;
					logsMap.add(new LogonLogoffEvent(user,lastOn+1*MIN,LOGOFF));
				} 
			} else {
				logsMap.add(new LogonLogoffEvent(user,Math.max(0,nowOn-1*MIN),LOGON));
				isOnline = true;
			}

			onlineMap.put(user, isOnline);
			lastMap.put(user, nowOn);
		}
		for (int onuser : onlineMap.keySet()) 
			logsMap.add(new LogonLogoffEvent(onuser,nowOn+1*MIN,LOGOFF));
		return logsMap;
	}
}

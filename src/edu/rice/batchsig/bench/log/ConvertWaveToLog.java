package edu.rice.batchsig.bench.log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

/*
 * zcat ../wave_data/first_hour.gz | head -3000000 | java  -verbose:gc -XX:+UseCompressedOops -Xmx2000m  -cp lib/bb.jar:lib/bcprov.jar:lib/jsci-core.jar:lib/mt-13.jar:bin/:/usr/share/java/protobuf.jar:/usr/share/java/commons-cli.jar:/home/crosby/source/Misc/guava-libraries-svn/build/dist/guava-unknown/guava-unknown.jar:/home/crosby/source/jars/fastutil-5.1.5.jar   edu.rice.batchsig.bench.log.ConvertWaveToLog 
 * 
 * 
 */


public class ConvertWaveToLog {

	/**
	 * @param args
	 * @throws IOException 
	 */
	static String prefix="data.wave";
	
	public static void main(String[] args) throws IOException {
		BuildEventTraceFromWaveTrace events = new BuildEventTraceFromWaveTrace();
		events.parse(new BufferedReader(new InputStreamReader(System.in)));
		writeLogonOffs(events);
		EventBase.sort(events.allEvents.log);
		events.allEvents.write(new FileOutputStream(prefix+".events"));
	}
	public static void writeLogonOffs (BuildEventTraceFromWaveTrace events) throws IOException {
		FileOutputStream output = new FileOutputStream(prefix+".onoff");
		Writer out=new BufferedWriter(new OutputStreamWriter(output));
		for (LogonLogoffEvent e : events.determineLogins())
			e.writeTo(out);
		out.flush();

	}
	
}

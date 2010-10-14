package edu.rice.batchsig.bench.log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import com.google.common.base.Splitter;
import com.google.protobuf.CodedOutputStream;

import edu.rice.batchsig.Message;
import edu.rice.batchsig.bench.OutgoingMessage;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;

public class LogonLogoffEvent extends EventBase {
	enum State {LOGON, LOGOFF};
	State state;

	LogonLogoffEvent(Integer user, long timestamp, State state) {
		super(timestamp,user);
		this.state = state;
	}
	
	public State getState() {
		return state;
	}

	public void writeTo(Writer out) throws IOException {
		out.write(Long.toString(timestamp)); out.write(',');
		if (state == State.LOGON) {
			out.write("+,");
		} else {
			out.write("-,");
		}
		out.write(Integer.toString(recipient_user));
		out.write('\n');
	}
	public static class Iter extends IterBase<LogonLogoffEvent> {
		public Iter(FileInputStream input) {
			super(input);
		}
		
		@Override
		public LogonLogoffEvent readOne() throws IOException {
			int recipient_user; // Usernames on those hosts.
			long timestamp;
			State state;
			String line = input.readLine();
			Iterator<String> it = Splitter.on(',').split(line).iterator();

			timestamp = Long.parseLong(it.next());
			
			char next = it.next().charAt(0);
			if (next == '+') {
				state = State.LOGON;
			} else if (next == '-') {
				state = State.LOGOFF;
			} else {
				throw new Error("???");
			}
			recipient_user = Integer.parseInt(it.next());
			return new LogonLogoffEvent(recipient_user, timestamp, state);
		}
	}
}

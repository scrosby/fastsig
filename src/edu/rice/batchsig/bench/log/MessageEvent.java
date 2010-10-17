package edu.rice.batchsig.bench.log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;

import com.google.common.base.Splitter;
import com.google.protobuf.CodedOutputStream;

import edu.rice.batchsig.Message;
import edu.rice.batchsig.bench.OutgoingMessage;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;

public class MessageEvent extends EventBase {
	final static int DEFAULT_BYTESIZE = 5;
	static int counter = 0;
	
	int sender_host, recipient_host; // Hosts or servers.
	int sender_user, recipient_user; // Usernames on those hosts.
	int size;
	
	MessageEvent(int sender_host, int recipient_host, int sender_user, int recipient_user, long timestamp, int size) {
		super(timestamp,recipient_user);
		this.sender_host = sender_host;
		this.recipient_host = recipient_host;
		this.sender_user = sender_user;
		this.recipient_user = recipient_user;
		this.size = size;
	}
	
	
	public Object getRecipientHost() {
		return recipient_host;
	}
	public Object getSenderHost() {
		return sender_host;
	}
	
	public Object getSenderUser() {
		return sender_user;
	}

	public OutgoingMessage asOutgoingMessage(CodedOutputStream target) {
		counter++;
		byte[] contents = new byte[size];
		contents[0] = (byte)(counter%64);
		contents[1] = (byte)((counter/64)%64);
		contents[2] = (byte)((counter/64/64)%64);
		contents[3] = (byte)((counter/64/64/64)%64);

		OutgoingMessage msg = new OutgoingMessage(target,contents, getRecipientHost(), recipient_user);
		msg.setVirtualClock(timestamp);
		return msg;
	}


	public void writeTo(Writer out) throws IOException {
		out.write(Long.toString(timestamp));  out.write(',');
		//System.out.println(sender_host + " XyX " + recipient_host);
		out.write(Integer.toString(sender_host)); out.write(',');
		out.write(Integer.toString(recipient_host)); out.write(',');
		out.write(Integer.toString(sender_user)); out.write(',');
		out.write(Integer.toString(recipient_user));
		out.write('\n');
	}
	
	/** reader iterator over an event log */
	public static class Iter extends IterBase<MessageEvent> {
		public Iter(FileInputStream fileinput) {
			super(fileinput);
		}
		
		@Override
		public MessageEvent readOne() throws IOException {
			int sender_host, recipient_host; // Hosts or servers.
			int sender_user, recipient_user; // Usernames on those hosts.
			long timestamp;
			String line = input.readLine();
			if (line == null)
				return null;
			
			Iterator<String> it = Splitter.on(',').split(line).iterator();

			timestamp = Long.parseLong(it.next());
			sender_host = Integer.parseInt(it.next());
			recipient_host = Integer.parseInt(it.next());
			sender_user = Integer.parseInt(it.next());
			recipient_user = Integer.parseInt(it.next());
			return new MessageEvent(sender_host,recipient_host,sender_user,recipient_user,timestamp,DEFAULT_BYTESIZE);
		}
	}
}

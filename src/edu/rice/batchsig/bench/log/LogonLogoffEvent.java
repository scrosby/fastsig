package edu.rice.batchsig.bench.log;

import com.google.protobuf.CodedOutputStream;

import edu.rice.batchsig.Message;
import edu.rice.batchsig.bench.OutgoingMessage;
import edu.rice.historytree.generated.Serialization.TreeSigBlob;

public class LogonLogoffEvent extends EventBase {
	Integer user;
	enum State {LOGON, LOGOFF};
	State state;

	LogonLogoffEvent(Integer user, long timestamp, State state) {
		super(timestamp);
		this.user = user;
		this.state = state;
	}
	
	public Integer getUser() {
		return user;
	}
	public State getState() {
		return state;
	}
}

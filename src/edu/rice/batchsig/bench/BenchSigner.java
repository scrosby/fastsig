/**
 * Copyright 2010 Rice University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Scott A. Crosby <scrosby@cs.rice.edu>
 *
 */

package edu.rice.batchsig.bench;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;



import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.protobuf.CodedOutputStream;

import edu.rice.batchsig.HistoryQueue;
import edu.rice.batchsig.MerkleQueue;
import edu.rice.batchsig.QueueBase;
import edu.rice.batchsig.SignaturePrimitives;
import edu.rice.batchsig.SimpleQueue;
import edu.rice.batchsig.VerifyQueue;
import edu.rice.batchsig.bench.log.BuildLogForVerificationBench;
import edu.rice.batchsig.bench.log.LogonLogoffEvent;
import edu.rice.batchsig.bench.log.MessageEvent;
import edu.rice.batchsig.bench.log.MultiplexedPublicKeyPrims;
import edu.rice.batchsig.bench.log.ReplayAndQueueMessagesForSigningThread;
import edu.rice.batchsig.bench.log.ReplaySavedMessagesRealtimeThread;


/* 
  

*/


public class BenchSigner {
	boolean isBatch, isBig, isVerifying, isTrace;
	QueueBase queue; // One of the three signing queues or a verifying queue 
	//SignaturePrimitives prims;
	//String ciphertype;
	CommandLine commands;
	Function<String,QueueBase> queuefn;
	

	/** Setup to do a single run of signing, creating and waiting for the threads to die. */
	protected void doSigningRun(CodedOutputStream output, int makeRate, int signRate, int sleepTime) {
		System.out.format("Signing run: %d makeRate, %d signRate, %d experimentTime\n",makeRate,signRate, sleepTime);
		CreateAndQueueMessagesForSigningThread makeThread = new CreateAndQueueMessagesForSigningThread(queue, output, makeRate);
		doCommon(sleepTime, makeThread);
		}

	/** Setup to do a single run of verifying, creating and waiting for the threads to die. */
	protected void doVerifyingRun(FileInputStream input, int makeRate, int signRate, int sleepTime) {
		System.out.format("Verifyign run: %d makeRate, %d signRate, %d experimentTime\n",makeRate,signRate, sleepTime);
		ReplaySavedMessagesThread makeThread = new ReplaySavedMessagesThread(queue,input, makeRate);
		doCommon(sleepTime, makeThread);
		}

	/** Setup to do a single run of verifying, creating and waiting for the threads to die. */
	protected void doVerifyingRunFromTraced(FileInputStream input, int makeRate, int signRate, int sleepTime) {
		// TODO: Code is copy&paste and wrong.
		ReplaySavedMessagesRealtimeThread makeThread = null; // TODO: new ReplaySavedMessagesRealtimeThread(queue,input,makeRate);
		//makeThread.setup((MultiplexedPublicKeyPrims) prims);
		doCommon(sleepTime, makeThread);
		}

	
	private void doCommon(int sleepTime, ShutdownableThread makeThread) {
		ProcessQueueThread processThread = new ProcessQueueThread(queue, 0);
		makeThread.start();
		processThread.start();
		try {
			Thread.sleep(sleepTime);
			makeThread.shutdown(); makeThread.join();
			processThread.interrupt(); // In case it is idle and not doing anything.
			processThread.shutdown(); processThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/** Do some processing to warm up the hotspot compiler 
	 * @throws FileNotFoundException */
	public void hotspotSigning() throws InterruptedException, FileNotFoundException {
		// Activate the hotspot over the data in question. 
		// Store a scratch copy of the queue, then reset to null.
		if (queue != null) 
			throw new Error("Queue should be null!");
		String signer_id = commands.getOptionValue("signerid","Host0");
		CodedOutputStream output = CodedOutputStream.newInstance(new FileOutputStream("/dev/null"));
		queue = queuefn.apply(signer_id);

		System.err.println("Starting hotspot signing");
		if (isBatch) {
			this.doSigningRun(output,100,1,500);
			this.doSigningRun(output,1000,1,1000);
			this.doSigningRun(output,10000,1,5000);
			if (isBig) 
				this.doSigningRun(output,10000, 1, BIGTIME);
		} else {
			this.doSigningRun(output,10,1,100);
			if (commands.hasOption("rsa")) {
				this.doSigningRun(output,50,1,1000);
				this.doSigningRun(output,50,1,5000);
				if (isBig)
					this.doSigningRun(output,50, 1, BIGTIME);
			} else {
				this.doSigningRun(output,50,1,1000);
				this.doSigningRun(output,300,1,5000);
				if (isBig)
					this.doSigningRun(output,300, 1, BIGTIME);
			}
		}
		System.err.println("End hotspot signing");
		queue = null;
	}

	/** Do some processing to warm up the hotspot compiler 
	 * @throws FileNotFoundException */
	public void hotSpotVerifying(FileInputStream input) throws InterruptedException, FileNotFoundException {
		System.err.println("Starting hotspot verifying");
		if (isBatch) {
			this.doVerifyingRun(input,100,1,500);
			this.doVerifyingRun(input,1000,1,1000);
			this.doVerifyingRun(input,10000,1,5000);
			if (isBig) 
				this.doVerifyingRun(input,10000, 1, BIGTIME);
		} else {
			this.doVerifyingRun(input,10,1,5000);
			if (commands.hasOption("rsa")) {
				this.doVerifyingRun(input,50,1,1000);
				this.doVerifyingRun(input,50,1,5000);
				if (isBig)
					this.doVerifyingRun(input,50, 1, BIGTIME);
			} else {
				this.doVerifyingRun(input,50,1,1000);
				this.doVerifyingRun(input,300,1,5000);
				if (isBig)
					this.doVerifyingRun(input,300, 1, BIGTIME);
			}
		}
		System.err.println("Ending hotspot verifying");
	}

	
	

	public void doBenchOne(CallBack cb, int rate) {
		System.err.format("**** RUNNING  rate=%d ****\n",rate);
		System.err.flush();
		Tracker.singleton.reset();
		Tracker.singleton.enable();
		cb.run(rate);
		Tracker.singleton.print(String.format("%05d",rate));
	}

	static final int BIGTIME = 120000;
	static final int NORMALTIME = 5000;

	/* Run an experiment at an ever-increasing rate */
	public void doBenchMany(CallBack cb) throws InterruptedException {
		int rate,incr;
		if (isBatch && !isVerifying) {
			if (commands.hasOption("merkle"))
				rate = 25000; // Merkle tree dies around 35k-45k
			else
				rate = 20000; // History tree dies around 25k
			incr = 500;
		} else {
			if (commands.hasOption("rsa")) {
				if (isVerifying) {
					rate = 2000; // Dies at 3000
					incr = 100;
				} else {
					rate = 40; // RSA dies at ~120
					incr = 4;
				}
			} else { // DSA.
				if (isVerifying) {
					rate = 500; // In java, verifies half as fast as signs.
					incr = 20;
				} else {
					rate = 800;
					incr = 20;
				}
			}
		}
		if (commands.hasOption("rate")) {
			rate = Integer.parseInt(commands.getOptionValue("rate"));
		}
		if (commands.hasOption("incr")) {
			incr = Integer.parseInt(commands.getOptionValue("incr"));
		}
		do {
			doBenchOne(cb, rate);
			rate += incr;
		} while(Tracker.singleton.isAborting() != true);
	}


	interface CallBack {
		void run(int rate);
	}	
	
	@SuppressWarnings("static-access")
	static public Options initOptions() {
		Options o = new Options();
		
		o.addOptionGroup(
				new OptionGroup()
				    .addOption(OptionBuilder.withDescription("Sign a bunch of message from a trace").create("signtrace"))
					.addOption(OptionBuilder.withDescription("Verify messages collected via a trace").create("verifytrace"))
					.addOption(OptionBuilder.withDescription("Verify messages collected via a trace").create("makeverifytrace"))
					.addOption(OptionBuilder.withDescription("Sign a bunch of messages created at a target rate").create("sign"))
					.addOption(OptionBuilder.withDescription("Verify a bunch of messages").create("verify"))
					)
		 .addOptionGroup(
				new OptionGroup()
				.addOption(OptionBuilder.withDescription("Run test with trace from from Google wave").create("wave"))
				.addOption(OptionBuilder.withDescription("Run test with trace from email").create("email"))
				.addOption(OptionBuilder.withDescription("Run test with trace from twitter").create("twitter")))
		 .addOptionGroup(
				new OptionGroup()
				.addOption(OptionBuilder.withDescription("Sign each message one at a time").create("simple"))
				.addOption(OptionBuilder.withDescription("Sign each message with merkle tree").create("merkle"))
				.addOption(OptionBuilder.withDescription("Sign each message with history tree").create("history")))
		.addOption(OptionBuilder.withDescription("Do longer duration experiments").create("big"))
		.addOption(OptionBuilder.withDescription("Trace to use").hasArg().create("trace"))
		.addOption(OptionBuilder.withDescription("name of event trace").hasArg().create("eventtrace"))
		.addOption(OptionBuilder.withDescription("name of user logonlogoff trace").hasArg().create("usertrace"))
		.addOption(OptionBuilder.withDescription("Output file (used when signing)").hasArg().create("output"))
		.addOption(OptionBuilder.withDescription("Input file (used when verifying)").hasArg().create("input"))
		.addOption(OptionBuilder.withDescription("Automatically scale the signing rate").create("autorate"))
		.addOption(OptionBuilder.withDescription("Run at the given signing rate").hasArg().create("rate"))
		.addOption(OptionBuilder.withDescription("Run at the given signing rate increment").hasArg().create("incr"))
		.addOption(OptionBuilder.withDescription("Return help").create('h'))
		.addOption(OptionBuilder.withDescription("Which crypto provider to use").hasArg().create("provider"))
		//.addOption(OptionBuilder.withDescription("When verifying, which signer ID to use").hasArg().create("signerid"))
		;
		
		OptionGroup tmp1 = 
				new OptionGroup()
				.addOption(OptionBuilder.withDescription("Use DSA with the given keysize").hasOptionalArg().create("dsa"))
				.addOption(OptionBuilder.withDescription("Use RSA with the given keysize").hasOptionalArg().create("rsa"))
			;
		tmp1.setRequired(true);
		o.addOptionGroup(tmp1);

		OptionGroup tmp2 = 
			new OptionGroup()
			.addOption(OptionBuilder.withDescription("Use sha256").create("sha256"))
			.addOption(OptionBuilder.withDescription("Use sha1").create("sha1"))
		;
		tmp2.setRequired(true);
		o.addOptionGroup(tmp2);

		return o;
	}

	
	
	
	public void parsecmd(String args[]) throws ParseException, InvalidKeyException, NoSuchAlgorithmException, InterruptedException, IOException, NoSuchProviderException {
		isVerifying = true;
		commands = new BasicParser().parse(initOptions(),args);
		if (commands.hasOption('h')) {
			(new HelpFormatter()).printHelp( "bench", initOptions() );
			System.exit(0);
		}

		if (commands.hasOption("big"))
			isBig = true;
		else
			isBig = false;

		final int time = isBig ? BIGTIME : NORMALTIME;
				
		if (commands.hasOption("verify")) {
			isTrace = false;
			handleVerifying(time);
			return;
		} 
		if (commands.hasOption("verifytrace")) {
			isTrace = true;
			handleVerifyTrace(time);
			return;
		}

		// Must happen before we create queues.
		isVerifying = false;
		
		// Figure out if it is a trace before we setup the cipher.
		if (commands.hasOption("signtrace") || commands.hasOption("verifytrace") || commands.hasOption("makeverifytrace"))
			isTrace = true;

		// Setup the cipher before we create the queues.
		String signer_id = commands.getOptionValue("signerid","Host0");

		// Create queues.
		if (commands.hasOption("history")) {
			isBatch = true;
			queuefn=new Function<String,QueueBase>(){public QueueBase apply(String signer_id) {return new HistoryQueue(setupCipher(signer_id));}};
		} else if (commands.hasOption("merkle")) {
			isBatch = true;
			queuefn=new Function<String,QueueBase>(){public QueueBase apply(String signer_id) {return new MerkleQueue(setupCipher(signer_id));}};
		} else if (commands.hasOption("simple")) {
			isBatch = false;
			queuefn=new Function<String,QueueBase>(){public QueueBase apply(String signer_id) {return new SimpleQueue(setupCipher(signer_id));}};
		} else {
			throw new IllegalArgumentException("Unknown signqueue type. Please choose one of -history -merkle or -simple");
		}


		
		if (commands.hasOption("signtrace")) {
			hotspotSigning();
			handleTraceSigning();
		} else if (commands.hasOption("sign")) {
			hotspotSigning();
			handleSigning(time);
		} else if (commands.hasOption("makeverifytrace")){
			handleMakeVerifyTrace(0);
		} else {
			throw new Error("Need to use a mode");
		}


	}

	private void handleVerifyTrace(int time) {
		
		
	}

	private void handleSigning(final int time) throws FileNotFoundException,
			InterruptedException, IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException {
		String signer_id = commands.getOptionValue("signerid","Host0");
		queue = queuefn.apply(signer_id);
		CodedOutputStream tmpoutput;
		CodedOutputStream output = null;
		if (commands.hasOption("output")) {
			tmpoutput = CodedOutputStream.newInstance(new FileOutputStream("/dev/null"));
			output = CodedOutputStream.newInstance(new FileOutputStream(commands.getOptionValue("output")));
		}
		final CodedOutputStream output2 = output;
		// Pre-load the hotspot.
		CallBack cb = new CallBack(){public void run(int rate) {doSigningRun(output2,rate,1,time);}};
		if (commands.hasOption("autorate"))
			doBenchMany(cb);
		else {
			doBenchOne(cb,Integer.parseInt(commands.getOptionValue("rate")));
		}

		if (output != null)
			output.flush();
	}

	final static int MAX_TRACE_BACKLOG = 2519;
	final static int MAX_SENDERS = 31; // Prime number, not 43 or 37
	final static int RSA_EPOCH_LENGTH = 60;
	final static int DSA_EPOCH_LENGTH = 5;
	final static int RSA_BATCHSIZE = 300;
	final static int DSA_BATCHSIZE = 20;
	private void handleMakeVerifyTrace(int epochtime) throws IOException {
		int epochlength = 0;
		Supplier<Integer> batchsize;
		if (commands.hasOption("rsa")) {
			epochlength = RSA_EPOCH_LENGTH; 
			batchsize = new Supplier<Integer>() { public Integer get() { return RSA_BATCHSIZE; };};
		} else if(commands.hasOption("dsa")) {
			epochlength = RSA_EPOCH_LENGTH;
			batchsize = new Supplier<Integer>() { public Integer get() { return DSA_BATCHSIZE; };};
		} else
			throw new Error("Must pick dsa or rsa");
		
		String outputname=commands.getOptionValue("output");

		CodedOutputStream output = CodedOutputStream.newInstance(new FileOutputStream(outputname+".signed"));
		
		BuildLogForVerificationBench builder = new BuildLogForVerificationBench(
				epochlength,queuefn,MAX_SENDERS,batchsize,output);
		String eventtracename=commands.getOptionValue("eventtrace");
		String usertracename=commands.getOptionValue("usertrace");
		
		if (usertracename == null) 
			throw new Error("Need -usertrace for logonlogoff");
		if (eventtracename == null) 
			throw new Error("Need -eventtrace for events");

		
		Iterator<MessageEvent> events = new MessageEvent.Iter(new FileInputStream(eventtracename));		
		Iterator<LogonLogoffEvent> logonlogoffs = new LogonLogoffEvent.Iter(new FileInputStream(usertracename));		
		builder.makeTrace(events,logonlogoffs);
		output.flush();
	
	}
	
	private void handleTraceSigning() throws FileNotFoundException {
		String signer_id = commands.getOptionValue("signerid","Host0");
		queue = queuefn.apply(signer_id);
		Tracker.singleton.reset();
		Tracker.singleton.enable();
		String eventtracename=commands.getOptionValue("eventtrace");
		String outputname=commands.getOptionValue("output");
		if (outputname == null)
			throw new Error("Must give an output name prefix with -output");
		
		
		Iterator<MessageEvent> events = new MessageEvent.Iter(new FileInputStream(eventtracename));		
		
		System.err.println("Setting up replay queue");
		ReplayAndQueueMessagesForSigningThread thr= new ReplayAndQueueMessagesForSigningThread(queue,MAX_TRACE_BACKLOG);

		HashMap<Object, CodedOutputStream> streammap = new HashMap<Object, CodedOutputStream>();
		for (int i=0 ; i < MAX_SENDERS ; i++) {
			streammap.put(i,CodedOutputStream.newInstance(new FileOutputStream(outputname+".signed="+i)));
		}
		
		thr.configure(streammap );
		thr.configure(0,events);
		doCommon(MAX_TRACE_BACKLOG, thr);
		Tracker.singleton.print(String.format("Trace-%s",eventtracename));
	}

	/* *Simple method for verifying a stream of messages by a single author to a single recipient.*/
	private void handleVerifying(final int time) throws FileNotFoundException,
			Error, InterruptedException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException {
		String signer_id = commands.getOptionValue("signerid","Host0");
		queue = new VerifyQueue(setupCipher(signer_id));
		if (commands.getOptionValue("input") == null)
			throw new Error("Need to define an input file");
		final FileInputStream fileinput = new FileInputStream(commands.getOptionValue("input"));

		hotSpotVerifying(fileinput);
		doBenchMany(new CallBack(){public void run(int rate) {doVerifyingRun(fileinput,rate,1,time);}});
		return; // Done with handling verification.
	}

	SignaturePrimitives setupCipher(String signer_id) {
		try {
		int bits;
		String type = "";
		if (commands.hasOption("sha1")) {
			type += "SHA1";
		} else if (commands.hasOption("sha256")) {
			type += "SHA256";
		} else {
			throw new Error();
		}

		type += "with";
		
		if (commands.hasOption("dsa")) {
			bits = Integer.parseInt(commands.getOptionValue("dsa","1024"));
			type += "DSA";
		} else if (commands.hasOption("rsa")) {
			bits = Integer.parseInt(commands.getOptionValue("rsa","2048"));
			type += "RSA";
		} else {
			throw new Error();
		}

		// Must set the prims first, used with the other.
		if (isTrace && isVerifying) {
				return MultiplexedPublicKeyPrims.make(type,bits,commands.getOptionValue("provider"));
		} else {
			return PublicKeyPrims.make(signer_id,type,bits,commands.getOptionValue("provider"));
		}
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		throw new Error("Failed");
	}
	
	public static void main(String args[]) throws FileNotFoundException, ParseException {
		Security.addProvider(new BouncyCastleProvider());		
		try {
			BenchSigner bench = new BenchSigner();
			bench.parsecmd(args);
			System.exit(0);
			
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		}
	}
}



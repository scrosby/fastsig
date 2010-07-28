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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
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



import com.google.protobuf.CodedOutputStream;

import edu.rice.batchsig.HistoryQueue;
import edu.rice.batchsig.MerkleQueue;
import edu.rice.batchsig.QueueBase;
import edu.rice.batchsig.SignaturePrimitives;
import edu.rice.batchsig.SimpleQueue;
import edu.rice.batchsig.VerifyQueue;


// java -cp lib/bb.jar:lib/bcprov.jar:lib/jsci-core.jar:lib/mt-13.jar:bin/:/usr/share/java/protobuf.jar  edu.rice.batchsig.bench.BenchSigner simple sha1withrsa 1024


public class BenchSigner {
	boolean isBatch, isBig;
	QueueBase signqueue, verifyqueue;
	SignaturePrimitives prims;
	//String ciphertype;
	CommandLine commands;
	

	public void initialization(CodedOutputStream output, int makeRate, int signRate, int sleepTime) throws InterruptedException {
		MakeMessagesThread makeThread = new MakeMessagesThread(signqueue, output, makeRate);
		SignMessageThread signThread = new SignMessageThread(signqueue, signRate);
		makeThread.start();
		signThread.start();
		Thread.sleep(sleepTime);
		makeThread.shutdown(); makeThread.join();
		signThread.shutdown(); signThread.join();
		}
		
	public void hotSpot(CodedOutputStream output) throws InterruptedException {
		if (isBatch) {
			this.initialization(output,100,1,500);
			this.initialization(output,1000,1,1000);
			this.initialization(output,10000,1,5000);
			if (isBig) 
				this.initialization(output,10000, 1, BIGTIME);
		} else {
			this.initialization(output,10,1,100);
			if (commands.hasOption("rsa")) {
				this.initialization(output,50,1,1000);
				this.initialization(output,50,1,5000);
				if (isBig)
					this.initialization(output,50, 1, BIGTIME);
			} else {
				this.initialization(output,50,1,1000);
				this.initialization(output,300,1,5000);
				if (isBig)
					this.initialization(output,300, 1, BIGTIME);
			}
		}
	}

	public void hotSpot(FileInputStream input) throws InterruptedException {
		if (isBatch) {
			this.initialization(input,100,1,500);
			this.initialization(input,1000,1,1000);
			this.initialization(input,10000,1,5000);
			if (isBig) 
				this.initialization(input,10000, 1, BIGTIME);
		} else {
			this.initialization(input,10,1,100);
			if (commands.hasOption("rsa")) {
				this.initialization(input,50,1,1000);
				this.initialization(input,50,1,5000);
				if (isBig)
					this.initialization(input,50, 1, BIGTIME);
			} else {
				this.initialization(input,50,1,1000);
				this.initialization(input,300,1,5000);
				if (isBig)
					this.initialization(input,300, 1, BIGTIME);
			}
		}
	}

	
	
	/*
	public void doBenchOne() throws InterruptedException {
		Tracker.singleton.enable();
		int time = isBig ? BIGTIME : 5000;
		if (isBatch) {
			initialization(null,40000,1,time);
		} else {
			initialization(null,300,1,time);
		}
	}
	*/

	static final int BIGTIME = 120000;
	static final int NORMALTIME = 5000;

	public void doBenchMany(CodedOutputStream output) throws InterruptedException {
		int time = isBig ? NORMALTIME : 5000;
		int rate,incr;
		if (isBatch) {
			rate = 10000;
			incr = 1000;
		} else {
			if (commands.hasOption("rsa")) {
				rate = 12;
				incr = 4;
			} else {
				rate = 300;
				incr = 5;
			}
		}
		do {
			System.err.format("**** RUNNING  rate=%d ****\n",rate);
			System.err.flush();
			Tracker.singleton.reset();
			Tracker.singleton.enable();
			initialization(output,rate,1,time);
			Tracker.singleton.print(String.format("%05d",rate));
			rate += incr;
		} while(Tracker.singleton.isAborting() != true);
	}

	
	public void doBenchMany(FileInputStream input) throws InterruptedException {
		int time = isBig ? NORMALTIME : 5000;
		int rate,incr;
		if (isBatch) {
			rate = 10000;
			incr = 1000;
		} else {
			if (commands.hasOption("rsa")) {
				rate = 2000;
				incr = 100;
			} else {
				rate = 300;
				incr = 5;
			}
		}
		do {
			System.err.format("**** RUNNING  rate=%d ****\n",rate);
			System.err.flush();
			Tracker.singleton.reset();
			Tracker.singleton.enable();
			initialization(input,rate,1,time);
			Tracker.singleton.print(String.format("%05d",rate));
			rate += incr;
		} while(Tracker.singleton.isAborting() != true);
	}
	
	
	
	@SuppressWarnings("static-access")
	static public Options initOptions() {
		Options o = new Options();
		
		o.addOptionGroup(
				new OptionGroup()
					.addOption(OptionBuilder.withDescription("Sign a bunch of messages").create("sign"))
					.addOption(OptionBuilder.withDescription("Verify a bunch of messages").create("verify"))
					)
		 .addOptionGroup(
				new OptionGroup()
				.addOption(OptionBuilder.withDescription("Sign each message one at a time").create("simple"))
				.addOption(OptionBuilder.withDescription("Sign each message with merkle tree").create("merkle"))
				.addOption(OptionBuilder.withDescription("Sign each message with history tree").create("history")))
		.addOption(OptionBuilder.withDescription("Do longer duration experiments").create("big"))
		.addOption(OptionBuilder.withDescription("Output file (used when signing)").hasArg().create("output"))
		.addOption(OptionBuilder.withDescription("Input file (used when signing)").hasArg().create("input"))
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

	public void initialization(FileInputStream input, int makeRate, int signRate, int sleepTime) throws InterruptedException {
		ReplayMessagesThread makeThread = new ReplayMessagesThread(verifyqueue,input, makeRate);
		SignMessageThread signThread = new SignMessageThread(verifyqueue, 0);
		makeThread.start();
		signThread.start();
		Thread.sleep(sleepTime);
		makeThread.shutdown(); makeThread.join();
		signThread.shutdown(); signThread.join();
		}
	
	
	
	public void parsecmd(String args[]) throws ParseException, InvalidKeyException, NoSuchAlgorithmException, InterruptedException, IOException {
		(new HelpFormatter()).printHelp( "bench", initOptions() );
		commands = new BasicParser().parse(initOptions(),args);

		setupCipher();

		if (commands.hasOption("big"))
			isBig = true;
		else
			isBig = false;
		
		if (commands.hasOption("history")) {
			isBatch = true;
			signqueue=new HistoryQueue(prims);
		} else if (commands.hasOption("merkle")) {
			isBatch = true;
			signqueue=new MerkleQueue(prims);
		} else if (commands.hasOption("simple")) {
			isBatch = false;
			signqueue=new SimpleQueue(prims);
		} else if (commands.hasOption("verify")) {
			verifyqueue = new VerifyQueue(prims);
			FileInputStream fileinput = new FileInputStream(commands.getOptionValue("input"));
			if (fileinput == null)
				throw new Error();
			hotSpot(fileinput);
			doBenchMany(fileinput);
			return; // Done with handling verification.
		} else {
			throw new IllegalArgumentException("Unknown signqueue type. Please choose one of -history -merkle or -simple");
		}

		CodedOutputStream output = null, tmpoutput = null;

		if (commands.hasOption("output")) {
			tmpoutput = CodedOutputStream.newInstance(new FileOutputStream("/dev/null"));
			output = CodedOutputStream.newInstance(new FileOutputStream(commands.getOptionValue("output")));
		}
		// Pre-load the hotspot.
		hotSpot(tmpoutput);
		doBenchMany(output);

		output.flush();
		
	}

	void setupCipher() throws InvalidKeyException, NoSuchAlgorithmException {
		int bits;
		String type = "";
		if (commands.hasOption("sha1")) {
			type += "sha1";
		} else if (commands.hasOption("sha256")) {
			type += "sha256";
		} else {
			throw new Error();
		}

		type += "with";
		
		if (commands.hasOption("dsa")) {
			bits = Integer.parseInt(commands.getOptionValue("dsa","1024"));
			type += "dsa";
		} else if (commands.hasOption("rsa")) {
			bits = Integer.parseInt(commands.getOptionValue("rsa","2048"));
			type += "rsa";
		} else {
			throw new Error();
		}
		
		// Must set the prims first, used with the other.
		prims = PublicKeyPrims.make("Bench",type,bits);
	}		
	
	public static void main(String args[]) throws FileNotFoundException, ParseException {
		Security.addProvider(new BouncyCastleProvider());		
		try {
			BenchSigner bench = new BenchSigner();
			bench.parsecmd(args);
			System.exit(0);
			
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}


// 781 271 3186
   //   This is Suzane kurnst cal version....    781 271 3186



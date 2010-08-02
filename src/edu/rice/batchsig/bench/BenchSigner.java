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
import java.security.NoSuchProviderException;
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


/* 
  
PROG="java -cp lib/bb.jar:lib/bcprov.jar:lib/jsci-core.jar:lib/mt-13.jar:bin/:/usr/share/java/protobuf.jar:/usr/share/java/commons-cli.jar  edu.rice.batchsig.bench.BenchSigner"

## DONE: USED TO MAKE THE LOG FILES for verification.
$PROG -provider BC -autorate -sha1 -rsa -simple   -output msglog/autorate.simple.rsa > results/sign.autorate.simple.rsa
$PROG -provider BC -autorate -sha1 -rsa -history  -output msglog/autorate.history.rsa > results/sign.autorate.history.rsa
$PROG -provider BC -autorate -sha1 -rsa -merkle   -output msglog/autorate.merkle.rsa > results/sign.autorate.merkle.rsa

$PROG -provider BC -autorate -sha1 -dsa -simple   -output msglog/autorate.simple.dsa > results/sign.autorate.simple.dsa
$PROG -provider BC -autorate -sha1 -dsa -history  -output msglog/autorate.history.dsa > results/sign.autorate.history.dsa
$PROG -provider BC -autorate -sha1 -dsa -merkle   -output msglog/autorate.merkle.dsa > results/sign.autorate.merkle.dsa

# Used to test verification.
$PROG -provider BC -autorate -sha1 -rsa -verify   -input msglog/autorate.simple.rsa > results/verify.autorate.simple.rsa
$PROG -provider BC -autorate -sha1 -rsa -verify   -input msglog/autorate.history.rsa > results/verify.autorate.history.rsa
$PROG -provider BC -autorate -sha1 -rsa -verify   -input msglog/autorate.merkle.rsa > results/verify.autorate.merkle.rsa

$PROG -provider BC -autorate -sha1 -dsa -verify   -input msglog/autorate.simple.dsa > results/verify.autorate.simple.dsa
$PROG -provider BC -autorate -sha1 -dsa -verify   -input msglog/autorate.history.dsa > results/verify.autorate.history.dsa
$PROG -provider BC -autorate -sha1 -dsa -verify   -input msglog/autorate.merkle.dsa > results/verify.autorate.merkle.dsa



## COLLECT SIGN PERFORMANCE
$PROG -provider BC -big -autorate -sha1 -rsa -simple  > results/sign.autorate.simple.rsa
$PROG -provider BC -big -autorate -sha1 -rsa -history > results/sign.autorate.history.rsa
$PROG -provider BC -big -autorate -sha1 -rsa -merkle  > results/sign.autorate.merkle.rsa

$PROG -provider BC -big -autorate -sha1 -dsa -simple  > results/sign.autorate.simple.dsa
$PROG -provider BC -big -autorate -sha1 -dsa -history > results/sign.autorate.history.dsa
$PROG -provider BC -big -autorate -sha1 -dsa -merkle  > results/sign.autorate.merkle.dsa

# COLLECT VERIFY PERFORMANCE
$PROG -provider BC -big -autorate -sha1 -rsa -verify   -input msglog/autorate.simple.rsa > results/verify.autorate.simple.rsa
#$PROG -provider BC -big -autorate -sha1 -rsa -verify   -input msglog/autorate.history.rsa > results/verify.autorate.history.rsa
$PROG -provider BC -big -autorate -sha1 -rsa -verify   -input msglog/autorate.merkle.rsa > results/verify.autorate.merkle.rsa

$PROG -provider BC -big -autorate -sha1 -dsa -verify   -input msglog/autorate.simple.dsa > results/verify.autorate.simple.dsa
#$PROG -provider BC -big -autorate -sha1 -dsa -verify   -input msglog/autorate.history.dsa > results/verify.autorate.history.dsa
$PROG -provider BC -big -autorate -sha1 -dsa -verify   -input msglog/autorate.merkle.dsa > results/verify.autorate.merkle.dsa

*/


public class BenchSigner {
	boolean isBatch, isBig, isVerifying;
	QueueBase queue; // One of the three signing queues or a verifying queue 
	SignaturePrimitives prims;
	//String ciphertype;
	CommandLine commands;
	

	/** Setup to do a single run of signing, creating and waiting for the threads to die. */
	protected void doSigningRun(CodedOutputStream output, int makeRate, int signRate, int sleepTime) {
		MakeMessagesThread makeThread = new MakeMessagesThread(queue, output, makeRate);
		doCommon(sleepTime, makeThread);
		}

	/** Setup to do a single run of verifying, creating and waiting for the threads to die. */
	protected void doVerifyingRun(FileInputStream input, int makeRate, int signRate, int sleepTime) {
		ReplayMessagesThread makeThread = new ReplayMessagesThread(queue,input, makeRate);
		doCommon(sleepTime, makeThread);
		}

	
	private void doCommon(int sleepTime, ShutdownableThread makeThread) {
		ProcessQueueThread processThread = new ProcessQueueThread(queue, 0);
		makeThread.start();
		processThread.start();
		try {
			Thread.sleep(sleepTime);
			makeThread.shutdown(); makeThread.join();
			processThread.shutdown(); processThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/** Do some processing to warm up the hotspot compiler */
	public void hotspotSigning(CodedOutputStream output) throws InterruptedException {
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
	}

	/** Do some processing to warm up the hotspot compiler */
	public void hotSpotVerifying(FileInputStream input) throws InterruptedException {
		if (isBatch) {
			this.doVerifyingRun(input,100,1,500);
			this.doVerifyingRun(input,1000,1,1000);
			this.doVerifyingRun(input,10000,1,5000);
			if (isBig) 
				this.doVerifyingRun(input,10000, 1, BIGTIME);
		} else {
			this.doVerifyingRun(input,10,1,100);
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
		.addOption(OptionBuilder.withDescription("Automatically scale the signing rate").create("autorate"))
		.addOption(OptionBuilder.withDescription("Run at the given signing rate").hasArg().create("rate"))
		.addOption(OptionBuilder.withDescription("Return help").create('h'))
		.addOption(OptionBuilder.withDescription("Which crypto provider to use").hasArg().create("provider"))
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
		commands = new BasicParser().parse(initOptions(),args);
		if (commands.hasOption('h')) {
			(new HelpFormatter()).printHelp( "bench", initOptions() );
			System.exit(0);
		}
		setupCipher();

		if (commands.hasOption("big"))
			isBig = true;
		else
			isBig = false;

		final int time = isBig ? BIGTIME : NORMALTIME;
				
		if (commands.hasOption("history")) {
			isBatch = true;
			queue=new HistoryQueue(prims);
		} else if (commands.hasOption("merkle")) {
			isBatch = true;
			queue=new MerkleQueue(prims);
		} else if (commands.hasOption("simple")) {
			isBatch = false;
			queue=new SimpleQueue(prims);
		} else if (commands.hasOption("verify")) {
			isVerifying = true;
			queue = new VerifyQueue(prims);
			final FileInputStream fileinput = new FileInputStream(commands.getOptionValue("input"));
			if (fileinput == null)
				throw new Error();
			hotSpotVerifying(fileinput);
			doBenchMany(new CallBack(){public void run(int rate) {doVerifyingRun(fileinput,rate,1,time);}});
			return; // Done with handling verification.
		} else {
			throw new IllegalArgumentException("Unknown signqueue type. Please choose one of -history -merkle or -simple");
		}

		isVerifying = false;
		
		CodedOutputStream output = null, tmpoutput = null;

		if (commands.hasOption("output")) {
			tmpoutput = CodedOutputStream.newInstance(new FileOutputStream("/dev/null"));
			output = CodedOutputStream.newInstance(new FileOutputStream(commands.getOptionValue("output")));
		}
		final CodedOutputStream output2 = output;
		// Pre-load the hotspot.
		hotspotSigning(tmpoutput);
		CallBack cb = new CallBack(){public void run(int rate) {doSigningRun(output2,rate,1,time);}};
		if (commands.hasOption("autorate"))
			doBenchMany(cb);
		else {
			doBenchOne(cb,Integer.parseInt(commands.getOptionValue("rate")));
		}

		if (output != null)
			output.flush();
	}

	void setupCipher() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException {
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
		prims = PublicKeyPrims.make("Bench",type,bits,commands.getOptionValue("provider"));
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
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}


// 781 271 3186
   //   This is Suzane kurnst cal version....    781 271 3186



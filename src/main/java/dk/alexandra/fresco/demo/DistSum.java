/*******************************************************************************
 * Copyright (c) 2015 FRESCO (http://github.com/aicis/fresco).
 *
 * This file is part of the FRESCO project.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * FRESCO uses SCAPI - http://crypto.biu.ac.il/SCAPI, Crypto++, Miracl, NTL,
 * and Bouncy Castle. Please see these projects for any further licensing issues.
 *******************************************************************************/
//package dk.alexandra.fresco.demo;
package dk.alexandra.fresco.demo;

import java.math.BigInteger;
import java.util.logging.Level;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.alexandra.fresco.framework.Application;
import dk.alexandra.fresco.framework.MPCException;
import dk.alexandra.fresco.framework.Protocol;
import dk.alexandra.fresco.framework.ProtocolFactory;
import dk.alexandra.fresco.framework.ProtocolProducer;
import dk.alexandra.fresco.framework.Reporter;
import dk.alexandra.fresco.framework.configuration.CmdLineUtil;
import dk.alexandra.fresco.framework.sce.SCE;
import dk.alexandra.fresco.framework.sce.SCEFactory;
import dk.alexandra.fresco.framework.sce.configuration.ProtocolSuiteConfiguration;
import dk.alexandra.fresco.framework.sce.configuration.SCEConfiguration;
import dk.alexandra.fresco.framework.util.ByteArithmetic;
import dk.alexandra.fresco.framework.value.OBool;
import dk.alexandra.fresco.framework.value.OInt;
import dk.alexandra.fresco.framework.value.SBool;
import dk.alexandra.fresco.framework.value.SInt;
import dk.alexandra.fresco.lib.field.integer.BasicNumericFactory;
import dk.alexandra.fresco.lib.helper.ParallelProtocolProducer;
import dk.alexandra.fresco.lib.helper.builder.NumericIOBuilder;
import dk.alexandra.fresco.lib.helper.builder.NumericProtocolBuilder;
import dk.alexandra.fresco.lib.helper.sequential.SequentialProtocolProducer;
import dk.alexandra.fresco.suite.bgw.configuration.BgwConfiguration;


/**
 * This demonstrates how to aggregate generic protocols to form an application.
 */
public class DistSum implements Application {
	
	/**
	 * Applications can be uploaded to fresco dynamically and are therefore
	 * Serializable's. This means that each application must have a unique
	 * serialVersionUID.
	 * 
	 */
	private static final long serialVersionUID = 1081355810959328531L;
	
	private static long protocolStartTime = Long.MIN_VALUE;
	
	static { System.setProperty("logback.configurationFile", "logback.xml");}
	private final static Logger l = LoggerFactory.getLogger(DistSum.class);
	
	private int id;
	private BigInteger myValue;
	private SCEConfiguration sceConf = null;
	
	public OInt[] result;
	
	
	public DistSum(int id, SCEConfiguration config) {
		this.id = id;
		this.sceConf = config;
		this.myValue = BigInteger.valueOf(config.getMyId() * 2);
	}
	
	
	/**
	 * The main method sets up application specific command line parameters,
	 * parses command line arguments. Based on the command line arguments it
	 * configures the SCE, instantiates the TestAESDemo and runs the TestAESDemo on the
	 * SCE.
	 * 
	 */
	public static void main(String[] args) {
		Reporter.init(Level.FINE);
		
		CmdLineUtil util = new CmdLineUtil();
		SCEConfiguration sceConf = null;
		
		try {
			// Node configuration (our ID, other MPC nodes) from command line
			CommandLine cmd = util.parse(args);
			sceConf = util.getSCEConfiguration();
						
		} catch (IllegalArgumentException e) {
			System.out.println("Error: " + e);
			System.out.println();
			util.displayHelp();
			System.exit(-1);
		}

		// Do the secure computation
		DistSum app = new DistSum(sceConf.getMyId(), sceConf);
		final int numParties = sceConf.getParties().size();
		
		// Overwrite suite configuration for practical threshold
		ProtocolSuiteConfiguration protocolSuiteConf = new BgwConfiguration() {
			@Override
			public int getThreshold() {
				return numParties / 2 - 1;
			}

			@Override
			public BigInteger getModulus() {
				return new BigInteger("618970019642690137449562111");
			}
		};
		
		SCE sce = SCEFactory.getSCEFromConfiguration(sceConf, protocolSuiteConf);
		
		try {
			sce.runApplication(app);
			
		} catch (MPCException e) {
			System.out.println("Error while doing MPC: " + e.getMessage());
			System.exit(-1);
		}
		long runTime = System.currentTimeMillis() - protocolStartTime;
		System.out.println(">>>>> [" + sceConf.getMyId() + "] Computation time: " + runTime);
		
		
		OInt[] rcvdOutput = app.result;
		
		System.out.println(">>>>> [" + sceConf.getMyId() + "] Got output " + rcvdOutput[0].getValue());
		System.out.println(">>>>> Computation done.");
		// Log statistics to common csv file
		l.debug(numParties + "," + runTime + "," + sceConf.getMyId() + "," + rcvdOutput[0].getValue());
		
		sce.shutdownSCE();		
	}
	
	/**
	 * Starts the measurement for the protocol execution.
	 * This function is currently called after having established the connection 
	 * to all neighbors.
	 */
	public static void startMeasurement() {
		if (protocolStartTime == Long.MIN_VALUE) {
			protocolStartTime = System.currentTimeMillis();
			System.out.println(">>>> Updated startMeasurement");
		}
	}	
	
	/**
	 * This is where the actual computation is defined. The method builds up a
	 * protocol that does one evaluation of a distributed sum. This involves
	 * protocols for 'closing' everyone's value, i.e., converting
	 * them from something that one of the players knows to secret values. It
	 * also involves a protocol for summing the secret values, and
	 * protocols for opening up the resulting sum.
	 * 
	 * The final protocol is build from smaller protocols using the
	 * ParallelProtocolProducer and SequentialProtocolProducer. The open and
	 * closed values (OBool and SBool) are used to 'glue' the subprotocols
	 * together.
	 * 
	 */
	@Override
	public ProtocolProducer prepareApplication(ProtocolFactory factory) {

		Reporter.init(Level.FINE);
		Reporter.info(">>>>> I am player " + sceConf.getMyId());
		
		BasicNumericFactory fac = (BasicNumericFactory) factory;
		NumericIOBuilder ioBuilder = new NumericIOBuilder(fac);
		NumericProtocolBuilder npb = new NumericProtocolBuilder(fac);	//< provides more advanced ioBuilder
		
		final int numPeers = sceConf.getParties().size();
		
		// Create wires for retrieving the others' shared secret part and ours one
		SInt[] inputSharings = new SInt[numPeers];
		
		// Closing protocol: what happens beind the scene
//		for (int i = 0; i < inputSharings.length; i++) {
//			inputSharings[i] = fac.getSInt();
//		}
		
		// Each participant provides a secret value which is shared secretly
		// among the other parties
//		ParallelProtocolProducer shareInputPar = new ParallelProtocolProducer();
//		OInt oi = fac.getOInt();
//		oi.setValue(myValue);
//		for (int p = 1; p <= numPeers; p++) {
//			shareInputPar.append(fac.getCloseProtocol(p, oi, inputSharings[p - 1]));								
//		}
		
		ioBuilder.beginParScope();
		for (int p = 1; p <= numPeers; p++) {
			inputSharings[p - 1] = ioBuilder.input(myValue, p);
		}
		ioBuilder.endCurScope();
		ProtocolProducer shareInputPar = (SequentialProtocolProducer)ioBuilder.getProtocol();
		ioBuilder.reset();

		// 2. Protocol: summing up all received shared secrets and one part of our own one
		// This works locally due to the linear properties of the shared secrets
		SInt sum1 = fac.getSInt();
		
		// Behind the scene: create sequence of protocols which will compute the sum
//		SequentialProtocolProducer sumProtocol = new SequentialProtocolProducer();
//		sumProtocol.append(fac.getAddProtocol(inputSharings[0], inputSharings[1], sum1));
//		
//		if (inputSharings.length > 2) {
//			for (int i = 2; i < inputSharings.length; i++) {
//				// Add sum and next secret shared input and
//				// store in sum.
//				sumProtocol.append(fac.getAddProtocol(sum1,
//						inputSharings[i], sum1));
//			}
//		}
		
		ProtocolProducer sumProtocol;
		sum1 = npb.sum(inputSharings);
		sumProtocol = npb.getProtocol();
		

		this.result = new OInt[] { ioBuilder.output(sum1) };
		ProtocolProducer io = ioBuilder.getProtocol();

		ProtocolProducer gp = new SequentialProtocolProducer(
				shareInputPar, sumProtocol, io);
		return gp;
	}
	
	
}
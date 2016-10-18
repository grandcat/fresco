package dk.alexandra.fresco.suite.tinytables.util.ot.extension;

import java.util.List;

import dk.alexandra.fresco.suite.tinytables.util.Encoding;
import dk.alexandra.fresco.suite.tinytables.util.ot.OTSender;
import dk.alexandra.fresco.suite.tinytables.util.ot.datatypes.OTInput;
import edu.biu.scapi.comm.Party;
import edu.biu.scapi.interactiveMidProtocols.ot.otBatch.otExtension.OTExtensionGeneralSInput;
import edu.biu.scapi.interactiveMidProtocols.ot.otBatch.otExtension.OTSemiHonestExtensionSender;

/**
 * We use SCAPI's OT Extension library for doing oblivious transfers. However,
 * this lib is implemented in C++ and we need to call it using JNI. Also, for
 * testing we run the different players on the same machine in two different
 * threads which seem to cause problems with the lib, so to avoid that we run
 * the JNI in seperate processes.
 * 
 * @author jonas
 *
 */
public class OTExtensionSender implements OTSender {

	private Party party;

	/**
	 * Create a new OTExtensionSender. The given <code>party</code> is the ip
	 * and port address we should be listening on (eg. this party's address).
	 * 
	 * @param party
	 */
	public OTExtensionSender(Party party) {
		this.party = party;
	}

	@Override
	public void send(List<OTInput> inputs) {

		byte[] input0 = Encoding.encodeBooleans(OTInput.getAll(inputs, 0));
		byte[] input1 = Encoding.encodeBooleans(OTInput.getAll(inputs, 1));
		
		OTSemiHonestExtensionSender sender = new OTSemiHonestExtensionSender(party);
		OTExtensionGeneralSInput input = new OTExtensionGeneralSInput(input0, input1, inputs.size());
		sender.transfer(null, input);
	}

}

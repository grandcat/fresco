package dk.alexandra.fresco.suite.tinytables.util.ot.extension;

import java.util.List;

import dk.alexandra.fresco.suite.tinytables.util.Encoding;
import dk.alexandra.fresco.suite.tinytables.util.ot.OTReceiver;
import dk.alexandra.fresco.suite.tinytables.util.ot.datatypes.OTSigma;
import edu.biu.scapi.comm.Party;
import edu.biu.scapi.interactiveMidProtocols.ot.OTOnByteArrayROutput;
import edu.biu.scapi.interactiveMidProtocols.ot.otBatch.OTBatchRInput;
import edu.biu.scapi.interactiveMidProtocols.ot.otBatch.otExtension.OTExtensionGeneralRInput;
import edu.biu.scapi.interactiveMidProtocols.ot.otBatch.otExtension.OTSemiHonestExtensionReceiver;

public class OTExtensionReceiver implements OTReceiver {

	private Party party;

	/**
	 * Create new OTExtensionReceiver with an OTExtensionSender running at the
	 * specified party.
	 * 
	 * @param party
	 */
	public OTExtensionReceiver(Party party) {
		this.party = party;
	}

	@Override
	public List<Boolean> receive(List<OTSigma> sigmas) {
		byte[] binarySigmas = Encoding.encodeBooleans(OTSigma.getAll(sigmas));

		OTSemiHonestExtensionReceiver receiver = new OTSemiHonestExtensionReceiver(party);

		OTBatchRInput input = new OTExtensionGeneralRInput(binarySigmas, 8);
		OTOnByteArrayROutput output = (OTOnByteArrayROutput) receiver.transfer(null, input);
		return Encoding.decodeBooleans(output.getXSigma());
	}

}

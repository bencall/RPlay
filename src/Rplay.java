import java.io.IOException;

import com.apple.dnssd.DNSSDException;

/**
 * Main class
 * @author bencall
 *
 */

public class Rplay {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int port = 5000;
		
		try {
			// DNS Emitter (Bonjour)
			RTSPResponder repondeur = new RTSPResponder(port);
			@SuppressWarnings("unused")
			BonjourEmitter emetteur = new BonjourEmitter("Benj", "F1F1F1F1F1F1", repondeur.getPort());

			repondeur.listen();


		} catch (DNSSDException e) {
			// Bonjour error
			e.printStackTrace();
		} catch (IOException e) {
			// No socket
			e.printStackTrace();
		}
	}

}

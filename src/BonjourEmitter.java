import java.util.*;
import javax.jmdns.*;
import java.io.IOException;

/**
 * Emetteur Bonjour pour qu'iTunes detecte la borne airport
 * @author bencall
 *
 */

//
public class BonjourEmitter {
	JmDNS jmdns;
	
	public BonjourEmitter(String name, String identifier, int port, boolean pass) throws IOException {

			// Set up TXT Record	    
		    Map<String,Object> txtRec = new HashMap<String,Object>();
		    txtRec.put("txtvers", "1");
		    txtRec.put("pw", String.valueOf(pass));
		    txtRec.put("sr", "44100");
		    txtRec.put("ss", "16");
		    txtRec.put("ch", "2");
		    txtRec.put("tp", "UDP");
		    txtRec.put("sm", "false");
		    txtRec.put("sv", "false");
		    txtRec.put("ek", "1");
		    txtRec.put("et", "0,1");
		    txtRec.put("cn", "0,1");
		    txtRec.put("vn", "3");
		    		   
		    // Il faut un serial bidon pour se connecter
		    if (identifier == null) {
		    	identifier = "";
		    	for(int i=0; i<6; i++)
		    		identifier = identifier + Integer.toHexString((int) (Math.random()*255)).toUpperCase();
		    }

			// Zeroconf registration
			jmdns = JmDNS.create();
			ServiceInfo serviceInfo = ServiceInfo.create("_raop._tcp.local.", identifier + "@" + name, port, 0, 0, txtRec);
            jmdns.registerService(serviceInfo);
	}

	/**
	 * Stop service publishing
	 */
	public void stop() throws IOException {
		jmdns.unregisterAllServices();
		jmdns.close();
	} 
}


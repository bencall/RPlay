import com.apple.dnssd.DNSSD;
import com.apple.dnssd.DNSSDException;
import com.apple.dnssd.DNSSDRegistration;
import com.apple.dnssd.DNSSDService;
import com.apple.dnssd.RegisterListener;
import com.apple.dnssd.TXTRecord;

/**
 * Emetteur Bonjour pour qu'iTunes detecte la borne airport
 * Needs Bonjour for Windows (apple.com)
 * @author bencall
 *
 */

//
public class BonjourEmitter implements RegisterListener{
	DNSSDRegistration r;
	
	public BonjourEmitter(String name, String identifier, int port) throws DNSSDException{
		    TXTRecord txtRecord = new TXTRecord(  );
		    txtRecord.set("txtvers", "1");
		    txtRecord.set("pw", "false");
		    txtRecord.set("sr", "44100");
		    txtRecord.set("ss", "16");
		    txtRecord.set("ch", "2");
		    txtRecord.set("tp", "UDP");
		    txtRecord.set("sm", "false");
		    txtRecord.set("sv", "false");
		    txtRecord.set("ek", "1");
		    txtRecord.set("et", "0,1");
		    txtRecord.set("cn", "0,1");
		    txtRecord.set("vn", "3");
		    
		    // Il faut un serial bidon pour se connecter
		    if (identifier == null){
		    	identifier = "";
		    	for(int i=0; i<6; i++){
		    		identifier = identifier + Integer.toHexString((int) (Math.random()*255)).toUpperCase();
		    	}
		    }

		    // Zeroconf registration
		    r = DNSSD.register(0, 0, identifier+"@"+name, "_raop._tcp", null, null, port, txtRecord, this);
		    
	}

	/**
	 * Stop service publishing
	 */
	public void stop(){
		r.stop();
	}
	
	/**
	 * Registration failed
	 */
	public void operationFailed(DNSSDService service, int errorCode) {
	    System.out.println("Registration failed " + errorCode); 		
	}

	/**
	 * Confirmation that registration is ok
	 */
	 // Display registered name on success 
	 public void serviceRegistered(DNSSDRegistration registration, int flags, String serviceName, String regType, String domain){ 
	    System.out.println("Registered Name  : " + serviceName); 
	    System.out.println("           Type  : " + regType); 
	    System.out.println("           Domain: " + domain); 
	 } 
}

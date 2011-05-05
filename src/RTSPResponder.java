import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.KeyPair;
import java.security.Security;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.InetSocketAddress;

import javax.crypto.Cipher;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;


/**
 * An primitive RTSP responder for replying iTunes
 * @author bencall
 *
 */
public class RTSPResponder extends Thread{
	
	private ServerSocket sock;				// Initial socket
	private Socket socket;					// Connected socket
	private int[] fmtp;
	private byte[] aesiv, aeskey;			// ANNOUNCE request infos
	private AudioServer serv; 				// Audio listener
	byte[] hwAddr;
	boolean stopThread = false;
	 
	private String key =  
		"-----BEGIN RSA PRIVATE KEY-----\n"
		+"MIIEpQIBAAKCAQEA59dE8qLieItsH1WgjrcFRKj6eUWqi+bGLOX1HL3U3GhC/j0Qg90u3sG/1CUt\n"
		+"wC5vOYvfDmFI6oSFXi5ELabWJmT2dKHzBJKa3k9ok+8t9ucRqMd6DZHJ2YCCLlDRKSKv6kDqnw4U\n"
		+"wPdpOMXziC/AMj3Z/lUVX1G7WSHCAWKf1zNS1eLvqr+boEjXuBOitnZ/bDzPHrTOZz0Dew0uowxf\n"
		+"/+sG+NCK3eQJVxqcaJ/vEHKIVd2M+5qL71yJQ+87X6oV3eaYvt3zWZYD6z5vYTcrtij2VZ9Zmni/\n"
		+"UAaHqn9JdsBWLUEpVviYnhimNVvYFZeCXg/IdTQ+x4IRdiXNv5hEewIDAQABAoIBAQDl8Axy9XfW\n"
		+"BLmkzkEiqoSwF0PsmVrPzH9KsnwLGH+QZlvjWd8SWYGN7u1507HvhF5N3drJoVU3O14nDY4TFQAa\n"
		+"LlJ9VM35AApXaLyY1ERrN7u9ALKd2LUwYhM7Km539O4yUFYikE2nIPscEsA5ltpxOgUGCY7b7ez5\n"
		+"NtD6nL1ZKauw7aNXmVAvmJTcuPxWmoktF3gDJKK2wxZuNGcJE0uFQEG4Z3BrWP7yoNuSK3dii2jm\n"
		+"lpPHr0O/KnPQtzI3eguhe0TwUem/eYSdyzMyVx/YpwkzwtYL3sR5k0o9rKQLtvLzfAqdBxBurciz\n"
		+"aaA/L0HIgAmOit1GJA2saMxTVPNhAoGBAPfgv1oeZxgxmotiCcMXFEQEWflzhWYTsXrhUIuz5jFu\n"
		+"a39GLS99ZEErhLdrwj8rDDViRVJ5skOp9zFvlYAHs0xh92ji1E7V/ysnKBfsMrPkk5KSKPrnjndM\n"
		+"oPdevWnVkgJ5jxFuNgxkOLMuG9i53B4yMvDTCRiIPMQ++N2iLDaRAoGBAO9v//mU8eVkQaoANf0Z\n"
		+"oMjW8CN4xwWA2cSEIHkd9AfFkftuv8oyLDCG3ZAf0vrhrrtkrfa7ef+AUb69DNggq4mHQAYBp7L+\n"
		+"k5DKzJrKuO0r+R0YbY9pZD1+/g9dVt91d6LQNepUE/yY2PP5CNoFmjedpLHMOPFdVgqDzDFxU8hL\n"
		+"AoGBANDrr7xAJbqBjHVwIzQ4To9pb4BNeqDndk5Qe7fT3+/H1njGaC0/rXE0Qb7q5ySgnsCb3DvA\n"
		+"cJyRM9SJ7OKlGt0FMSdJD5KG0XPIpAVNwgpXXH5MDJg09KHeh0kXo+QA6viFBi21y340NonnEfdf\n"
		+"54PX4ZGS/Xac1UK+pLkBB+zRAoGAf0AY3H3qKS2lMEI4bzEFoHeK3G895pDaK3TFBVmD7fV0Zhov\n"
		+"17fegFPMwOII8MisYm9ZfT2Z0s5Ro3s5rkt+nvLAdfC/PYPKzTLalpGSwomSNYJcB9HNMlmhkGzc\n"
		+"1JnLYT4iyUyx6pcZBmCd8bD0iwY/FzcgNDaUmbX9+XDvRA0CgYEAkE7pIPlE71qvfJQgoA9em0gI\n"
		+"LAuE4Pu13aKiJnfft7hIjbK+5kyb3TysZvoyDnb3HOKvInK7vXbKuU4ISgxB2bB3HcYzQMGsz1qJ\n"
		+"2gG0N5hvJpzwwhbhXqFKA4zaaSrw622wDniAK5MlIE0tIAKKP4yxNGjoD2QYjhBGuhvkWKaXTyY=\n"
		+"-----END RSA PRIVATE KEY-----\n"; 

	/**
	 * 
	 * @param port Will try to use the defined port. If not possible, check getPort() to know which port was taken.
	 * @throws IOException 
	 */
	public RTSPResponder(int port, byte[] hwAddr) throws IOException{
		this.hwAddr = hwAddr;
		
		// Try to take the given port. If not possible, take another. If not possible: ERROR
        try {
			sock = new ServerSocket(5000);
		} catch (IOException e) {
			sock = new ServerSocket(); 
		}
	}
	
	
	public void stopThread() throws IOException{
		stopThread = true;
		if(serv != null){
			serv.stop();
		}
		sock.close();
	}
	
	
	/**
	 * @return port number
	 */
	public int getPort(){
		return sock.getLocalPort();
	}
	
	public void handlePacket(RTSPPacket packet){		
		// We init the response holder
		StringBuilder response = new StringBuilder("RTSP/1.0 200 OK\r\n");
		response.append("Audio-Jack-Status: connected; type=analog\r\n");
		response.append("CSeq: "+ packet.valueOfHeader("CSeq")+"\r\n");

		// Apple Challenge-Response field if needed
    	String challenge;
    	if( (challenge = packet.valueOfHeader("Apple-Challenge")) != null){
    		// BASE64 DECODE
    		byte[] decoded = Base64.decodeBase64(challenge);

    		// IP byte array
    		//byte[] ip = socket.getLocalAddress().getAddress();
    		SocketAddress localAddress = socket.getLocalSocketAddress(); //.getRemoteSocketAddress();
    		    		
    		byte[] ip =  ((InetSocketAddress) localAddress).getAddress().getAddress();
    		
    		ByteArrayOutputStream out = new ByteArrayOutputStream();
    		// Challenge
    		try {
				out.write(decoded);
				// IP-Address
				out.write(ip);
				// HW-Addr
				out.write(hwAddr);
				
				// Pad to 32 Bytes
				int padLen = 32 - out.size();
				for(int i = 0; i < padLen; ++i) {
					out.write(0x00);
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}

    		 
    		// RSA
    		byte[] crypted = this.encryptRSA(out.toByteArray());
    		
    		// Encode64
    		String ret = Base64.encodeBase64String(crypted);
    		
    		// On retire les ==
	        ret = ret.replace("=", "").replace("\r", "").replace("\n", "");

    		// Write
        	response.append("Apple-Response: " + ret + "\r\n");	        	
    		
    	} 
    	
    	
		// Paquet request
		String REQ = packet.getReq();
        if(REQ.contentEquals("OPTIONS")){         // OPTIONS
        	System.out.println("REQUEST: OPTIONS");
        	
        	// The response field
        	response.append("Public: ANNOUNCE, SETUP, RECORD, PAUSE, FLUSH, TEARDOWN, OPTIONS, GET_PARAMETER, SET_PARAMETER\r\n");

        } else if (REQ.contentEquals("ANNOUNCE")){	// ANNOUNCE
        	System.out.println("REQUEST: ANNOUNCE");
        	
        	// Nothing to do here. Juste get the keys and values
        	Pattern p = Pattern.compile("^a=([^:]+):(.+)", Pattern.MULTILINE);
        	Matcher m = p.matcher(packet.getContent());
        	while(m.find()){
        		if(m.group(1).contentEquals("fmtp")){
        			// Parse FMTP as array
        			String[] temp = m.group(2).split(" ");
        			fmtp = new int[temp.length];
        			for (int i = 0; i< temp.length; i++){
        				fmtp[i] = Integer.valueOf(temp[i]);
        			}
        			
        		} else if(m.group(1).contentEquals("rsaaeskey")){
        			aeskey = this.decryptRSA(Base64.decodeBase64(m.group(2)));
        		} else if(m.group(1).contentEquals("aesiv")){
        			aesiv = Base64.decodeBase64(m.group(2));
        		}
        	}
        	
        } else if (REQ.contentEquals("SETUP")){
        	System.out.println("REQUEST: SETUP");
        	int controlPort = 0;
        	int timingPort = 0;
        	
        	String value = packet.valueOfHeader("Transport");        	
        	
        	// Control port
        	Pattern p = Pattern.compile(";control_port=(\\d+)");
        	Matcher m = p.matcher(value);
        	if(m.find()){
        		controlPort =  Integer.valueOf(m.group(1));
        	}
        	
        	// Timing port
        	p = Pattern.compile(";timing_port=(\\d+)");
        	m = p.matcher(value);
        	if(m.find()){
        		timingPort =  Integer.valueOf(m.group(1));
        	}
            
        	// Launching audioserver
			serv = new AudioServer(new AudioSession(aesiv, aeskey, fmtp, controlPort, timingPort));
			
        	response.append("Transport: " + packet.valueOfHeader("Transport") + ";server_port=" + serv.getServerPort() + "\r\n");
        			
        	// ??? Why ???
        	response.append("Session: DEADBEEF\r\n");
        } else if (REQ.contentEquals("RECORD")){
//        	Headers	
//        	Range: ntp=0-
//        	RTP-Info: seq={Note 1};rtptime={Note 2}
//        	Note 1: Initial value for the RTP Sequence Number, random 16 bit value
//        	Note 2: Initial value for the RTP Timestamps, random 32 bit value

        } else if (REQ.contentEquals("FLUSH")){
        	serv.flush();
        
        } else if (REQ.contentEquals("TEARDOWN")){
        	response.append("Connection: close\r\n");
        	
        } else if (REQ.contentEquals("SET_PARAMETER")){
        	// Timing port
        	Pattern p = Pattern.compile("volume: (.+)");
        	Matcher m = p.matcher(packet.getContent());
        	if(m.find()){
                double volume = (double) Math.pow(10.0,0.05*Double.parseDouble(m.group(1)));
                serv.setVolume(65536.0 * volume);
        	}
        	
        } else {
        	System.out.println("REQUEST(" + REQ + "): Not Supported Yet!");
        	System.out.println(packet.getRawPacket());
        }
        
    	// We close the response
    	response.append("\r\n");

    	// Write the packet to the wire
    	BufferedWriter oStream;
    	try {			
    		oStream = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    		oStream.write(response.toString());
    		oStream.flush();

    		// Don't know why, but connection must be closed after the OPTIONS request
    		if(REQ.contentEquals("OPTIONS") || REQ.contentEquals("TEARDOWN") ){
    			socket.close();
    			socket = null;
    		}
    		
    		// We listen for a new connection or new data
    		this.run();
    		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Crypts with private key
	 * @param array	data to encrypt
	 * @return encrypted data
	 */
	public byte[] encryptRSA(byte[] array){
		try{
			Security.addProvider(new BouncyCastleProvider());

	        PEMReader pemReader = new PEMReader(new StringReader(key)); 
	        KeyPair pObj = (KeyPair) pemReader.readObject(); 
	        	         
	        // Encrypt
	        Cipher cipher = Cipher.getInstance("RSA/NONE/PKCS1Padding"); 
	        cipher.init(Cipher.ENCRYPT_MODE, pObj.getPrivate());
	        return cipher.doFinal(array);

		}catch(Exception e){
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Decrypt with RSA priv key
	 * @param array
	 * @return
	 */
	public byte[] decryptRSA(byte[] array){
		try{
			Security.addProvider(new BouncyCastleProvider());

			// La clŽ RSA
	        PEMReader pemReader = new PEMReader(new StringReader(key)); 
	        KeyPair pObj = (KeyPair) pemReader.readObject(); 
	        	         
	        // Encrypt
	        Cipher cipher = Cipher.getInstance("RSA/NONE/OAEPPadding"); 
	        cipher.init(Cipher.DECRYPT_MODE, pObj.getPrivate());
	        return cipher.doFinal(array);

		}catch(Exception e){
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Thread to listen packets
	 */
	public void run() {
		boolean fin = stopThread;
		BufferedReader in;
		try {
			
			// Socket & Streams
			// Socket init
			if(socket == null){
				socket = sock.accept();
			}
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			
			String packet = "";
			while(!fin){
				// Buffer
				char[] buffer = new char[4096];
				in.read(buffer);
				String temp = new String(buffer);
				packet = packet + temp;
				
				// If packet completed
				Pattern p = Pattern.compile("(.*)\r\n\r\n");  
		        Matcher m = p.matcher(packet);  
				if(m.find()){
					//System.out.println(packet);
					// We handle the packet
					RTSPPacket paquet = new RTSPPacket(packet);
					this.handlePacket(paquet);
					packet = "";
					break;
				}
				
				synchronized(this){
					fin = stopThread;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
}

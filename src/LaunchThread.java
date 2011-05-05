import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;


/**
 * Main class
 * @author bencall
 *
 */

//
public class LaunchThread extends Thread{
	private RTSPResponder repondeur;
	private BonjourEmitter emetteur;
	private String name;
	
	public LaunchThread(String name){
		super();
		this.name = name;
	}
	
	
	public void run(){
		int port = 5000;
		byte[] hwAddr = null;;
		
		InetAddress local;
		try {
			local = InetAddress.getLocalHost();
			NetworkInterface ni = NetworkInterface.getByInetAddress(local);
			if (ni != null) {
				hwAddr = ni.getHardwareAddress();
			}
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	    StringBuilder sb = new StringBuilder();
	    for (byte b : hwAddr) {
	      sb.append(String.format("%02x", b));
	    }
		
		try {
			// DNS Emitter (Bonjour)
			repondeur = new RTSPResponder(port, hwAddr);
			emetteur = new BonjourEmitter(name, sb.toString(), repondeur.getPort());
			//emetteur1 = new ZeroConfEmitter(nameField.getText(), sb.toString(), repondeur.getPort());
			repondeur.start();


		} catch (Exception e) {
			// Bonjour error
			e.printStackTrace();
		}
	}
	
	public synchronized void stopThread(){
		emetteur.stop();
		try {
			repondeur.stopThread();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

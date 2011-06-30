import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import com.apple.dnssd.DNSSDException;


/**
 * LaunchThread class which starts services
 * @author bencall
 *
 */
public class LaunchThread extends Thread{
	private BonjourEmitter emitter;
	private String name;
	private boolean stopThread = false;
	
	/**
	 * Constructor
	 * @param name
	 */
	public LaunchThread(String name){
		super();
		this.name = name;
	}
	
	private byte[] getHardwareAdress() {
		byte[] hwAddr = null;
		
		InetAddress local;
		try {
			local = InetAddress.getLocalHost();
			NetworkInterface ni = NetworkInterface.getByInetAddress(local);
			if (ni != null) {
				hwAddr = ni.getHardwareAddress();
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return hwAddr;
	}
	
	
	private String getStringHardwareAdress(byte[] hwAddr) {
	    StringBuilder sb = new StringBuilder();
	    for (byte b : hwAddr) {
	      sb.append(String.format("%02x", b));
	    }
	    return sb.toString();
	}
	
	
	public void run(){
		System.out.println("service started.");
		int port = 5000;
		
		ServerSocket servSock = null;
		try {
			// We listen for new connections
			try {
				servSock = new ServerSocket(port);
			} catch (IOException e) {
				servSock = new ServerSocket();
			}

			// DNS Emitter (Bonjour)
			byte[] hwAddr = getHardwareAdress();
			emitter = new BonjourEmitter(name, getStringHardwareAdress(hwAddr), port);
			
			servSock.setSoTimeout(1000);
			while (!stopThread) {
				try {
					Socket socket = servSock.accept();
					System.out.println("got connection from " + socket.toString());
					new RTSPResponder(hwAddr, socket).start();
				} catch(SocketTimeoutException e) {
					// ignore
				}
			}

		} catch (DNSSDException e) {
			throw new RuntimeException(e);
			
		} catch (IOException e) {
			throw new RuntimeException(e);
			
		} finally {
			try {
				servSock.close(); // will stop all RTSPResponders.
				emitter.stop(); 
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("service stopped");
	}
	
	public synchronized void stopThread(){
		stopThread = true;
	}
}

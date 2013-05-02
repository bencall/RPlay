import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;


/**
 * LaunchThread class which starts services
 * @author bencall
 *
 */
public class LaunchThread extends Thread {
	private BonjourEmitter emitter;
	private String name;
	private String password;
	private boolean stopThread = false;
	
	/**
	 * Constructor
	 * @param name
	 */
	public LaunchThread(String name){
		super();
		this.name = name;
	}
	
	/**
	 * Constructor
	 * @param name
	 */
	public LaunchThread(String name, String pass){
		super();
		this.name = name;
		this.password = pass;
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
	
	
	public void run() {
		// Setup safe shutdown
		Runtime.getRuntime().addShutdownHook(new Thread() {
   			@Override
   			public void run() {
   				System.out.println("forced shutdown!");
   				
    			LaunchThread.this.stopThread();
    			
    			try {
	    			sleep(1000);
    			} catch(java.lang.InterruptedException e) {
    				//
    			}	    			
    			
    			System.out.println("done.");
   			}
  		});
		
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
						
			// Check if password is set
			if(password == null)
				emitter = new BonjourEmitter(name, getStringHardwareAdress(hwAddr), port, false);
			else
				emitter = new BonjourEmitter(name, getStringHardwareAdress(hwAddr), port, true);
			
			servSock.setSoTimeout(1000);
						
			while (!stopThread) {
				try {
					Socket socket = servSock.accept();
					System.out.println("got connection from " + socket.toString());
					
					// Check if password is set
					if(password == null)
						new RTSPResponder(hwAddr, socket).start();
					else
						new RTSPResponder(hwAddr, socket, password).start();
				} catch(SocketTimeoutException e) {
					// ignore
				}
			}

		} catch (IOException e) {
			System.out.println("Something is wrong...");
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

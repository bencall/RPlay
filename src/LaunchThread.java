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
	private	ServerSocket servSock = null;
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
			
			if (ni != null)
				hwAddr = ni.getHardwareAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		return hwAddr;
	}
	
	
	private String getStringHardwareAdress(byte[] hwAddr) {
	    StringBuilder sb = new StringBuilder();
	    
	    for (byte b : hwAddr)
	      sb.append(String.format("%02x", b));
	      
	    return sb.toString();
	}
	
	
	public void run() {
		System.out.println("starting service...");
		
		// Setup safe shutdown
		Runtime.getRuntime().addShutdownHook(new Thread() {
   			@Override
   			public void run() {
   				System.out.println("shutting down...");
   				
   				LaunchThread.this.stopThread();
   				
   				try {
					LaunchThread.this.emitter.stop();
	    			LaunchThread.this.servSock.close();
	    			
	    			System.out.println("service stopped.");
   				} catch (IOException e) {
   					//
   				}
   			}
  		});
				
		int port = 5000;
		
		try {
			// DNS Emitter (Bonjour)
			byte[] hwAddr = getHardwareAdress();
						
			// Check if password is set
			if(password == null)
				emitter = new BonjourEmitter(name, getStringHardwareAdress(hwAddr), port, false);
			else
				emitter = new BonjourEmitter(name, getStringHardwareAdress(hwAddr), port, true);
			
			System.out.println("announced ["+name+" @ "+getStringHardwareAdress(hwAddr)+"]");
			
			// We listen for new connections
			try {
				servSock = new ServerSocket(port);
			} catch (IOException e) {
				System.out.println("port busy, using default.");
				servSock = new ServerSocket();
			}
			
			servSock.setSoTimeout(1000);
			
			System.out.println("service started.");
						
			while (!stopThread) {
				try {
					Socket socket = servSock.accept();
					System.out.println("accepted connection from " + socket.toString());

					// Check if password is set
					if(password == null)
						new RTSPResponder(hwAddr, socket).start();
					else
						new RTSPResponder(hwAddr, socket, password).start();
				} catch(SocketTimeoutException e) {
					//
				}
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
			
		} finally {
			try {
				emitter.stop(); 
				servSock.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public synchronized void stopThread() {
		stopThread = true;
	}
}

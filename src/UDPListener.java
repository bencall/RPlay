import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Listen on a given socket
 * @author bencall
 *
 */
public class UDPListener extends Thread{
	// Constantes
	public static final int MAX_PACKET = 2048;

	// Variables d'instances
	DatagramSocket socket;
	UDPDelegate delegate;
	
	public UDPListener(DatagramSocket socket, UDPDelegate delegate){
		super();
		this.socket = socket;
		this.delegate = delegate;
		this.start();
	}
	
	  public void run() {
		  while(true){
			  byte[] buffer = new byte[MAX_PACKET];
			  DatagramPacket p = new DatagramPacket(buffer, buffer.length);
			  try {
				  socket.receive(p);
				  delegate.packetReceived(socket, p);
			  } catch (IOException e) {
				  e.printStackTrace();
			  }
		  }
	  }
}

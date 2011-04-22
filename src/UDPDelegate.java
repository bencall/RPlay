import java.net.DatagramPacket;
import java.net.DatagramSocket;


public interface UDPDelegate {
	public void packetReceived(DatagramSocket socket, DatagramPacket packet);
}

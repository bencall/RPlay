import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Interface for receiving packets
 * @author bencall
 *
 */
public interface UDPDelegate {
	public void packetReceived(DatagramSocket socket, DatagramPacket packet);
}

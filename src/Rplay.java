import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JFrame;

/**
 * Main class
 * @author bencall
 *
 */

//
public class Rplay extends Thread implements ActionListener{
	private boolean on = false;
	private JButton bouton;
	RTSPResponder repondeur;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new Rplay();
	}
	
	public Rplay(){
		super();
		
		JFrame window = new JFrame();
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setSize(200, 65);
		window.setTitle("RPlay");
		
		java.awt.Container contenu = window.getContentPane();
		contenu.setLayout(new FlowLayout());
		
		bouton = new JButton("Start Airport Express");
		bouton.addActionListener(this);
		contenu.add(bouton);
		
		window.setVisible(true);
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
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    StringBuilder sb = new StringBuilder();
	    for (byte b : hwAddr) {
	      sb.append(String.format("%02x", b));
	    }
		
		try {
			// DNS Emitter (Bonjour)
			repondeur = new RTSPResponder(port, hwAddr);
			@SuppressWarnings("unused")
			BonjourEmitter emetteur = new BonjourEmitter("Benj", sb.toString(), repondeur.getPort());
			repondeur.listen();


		} catch (Exception e) {
			// Bonjour error
			e.printStackTrace();
		}
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if(!on){
			this.start();
			bouton.setText("Stop Airport Express");
		} else {
			try {
				repondeur.stop();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			bouton.setText("Start Airport Express");
		}
	}

}

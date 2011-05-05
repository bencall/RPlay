import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

/**
 * Main class
 * @author bencall
 *
 */

//
public class Window implements ActionListener{
	private boolean on = false;
	private JButton bouton;
	private JTextField nameField;
	private LaunchThread t;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new Rplay();
	}
	
	public Window(){		
		JFrame window = new JFrame();
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		window.setSize(380, 100);
		window.setTitle("RPlay");
		
		java.awt.Container contenu = window.getContentPane();
		contenu.setLayout(new FlowLayout());
		
		nameField = new JTextField(15);
		bouton = new JButton("Start Airport Express");
		bouton.addActionListener(this);
		contenu.add(new JLabel("AP Name: "));
		contenu.add(nameField);
		contenu.add(bouton);
		
		window.pack();
		window.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if(!on){
			on = true;
			
			t = new LaunchThread(nameField.getText());
			t.start();
			bouton.setText("Stop Airport Express");
		} else {
			on = false;
			t.stopThread();
			bouton.setText("Start Airport Express");
		}
	}

}

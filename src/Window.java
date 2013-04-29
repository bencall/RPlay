import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.util.prefs.Preferences;

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
	private Preferences prefs;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new Rplay();
	}
	
	public Window(){		
		
		prefs = Preferences.userRoot().node(this.getClass().getName());
		
		JFrame window = new JFrame();
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		window.setSize(380, 100);
		window.setTitle("RPlay");
		
		java.awt.Container contenu = window.getContentPane();
		contenu.setLayout(new FlowLayout());
		
		String apname = prefs.get("apname", "");  // Get the default AP name

		nameField = new JTextField(apname,15);
		bouton = new JButton("Start Airport Express");
		bouton.addActionListener(this);
		contenu.add(new JLabel("AP Name: "));
		contenu.add(nameField);
		contenu.add(bouton);
		
		window.pack();
		window.setVisible(true);
		
		// If was previously started, start it now
		if (prefs.getBoolean("launched", true)) {
			bouton.doClick();
		}
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if(!on){
			on = true;
			
			t = new LaunchThread(nameField.getText());
			t.start();
			bouton.setText("Stop Airport Express");
			
			prefs.put("apname", nameField.getText());
			prefs.putBoolean("launched", true); // Used on next launch
		} else {
			on = false;
			t.stopThread();
			bouton.setText("Start Airport Express");
			prefs.putBoolean("launched", false); // Used on next launch
		}
	}

}

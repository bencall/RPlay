import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JOptionPane;
import java.util.prefs.Preferences;

/**
 * Main class
 * @author bencall
 *
 */

//
public class Window implements ActionListener {
	private JButton startButton;
	private JButton stopButton;
	private JTextField nameField;
	private LaunchThread t;
	private Preferences prefs;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new RPlay();
	}
	
	public Window() {
		prefs = Preferences.userRoot().node(this.getClass().getName());
		String apname = prefs.get("apname", "");
		
		nameField = new JTextField(apname,15);
		startButton = new JButton("Start");
		stopButton = new JButton("Stop");
		startButton.addActionListener(this);
		stopButton.addActionListener(this);
		startButton.setEnabled(true);
		stopButton.setEnabled(false);
		
		JFrame window = new JFrame();
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setTitle("RPlay");
		java.awt.Container content = window.getContentPane();
		content.setLayout(new FlowLayout());
		
		content.add(new JLabel("AP name: "));
		content.add(nameField);
		content.add(startButton);
		content.add(stopButton);
		
		window.pack();
		window.setVisible(true);
		
		// If was previously started, start it now
		if (prefs.getBoolean("launched", true)) {
			startButton.doClick();
		}
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		if(event.getSource() == startButton) {
			t = new LaunchThread(nameField.getText());
			t.start();
			
			prefs.put("apname", nameField.getText());
			prefs.putBoolean("launched", true); // Used on next launch
			
			startButton.setEnabled(false);
			stopButton.setEnabled(true);
		} else if(event.getSource() == stopButton) {
			t.stopThread();
			
			prefs.putBoolean("launched", false); // Used on next launch
			
			startButton.setEnabled(true);
			stopButton.setEnabled(false);
		} else {
			JOptionPane.showMessageDialog(null, "What are you up to?", "?!", JOptionPane.QUESTION_MESSAGE);
		}
	}

}

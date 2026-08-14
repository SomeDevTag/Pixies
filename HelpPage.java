import javax.swing.JFrame;
import javax.swing.WindowConstants;

/**
 * The window around the shortcut list. All the drawing lives in HelpScreen.
 *
 * Usage:
 *   new HelpPage().setVisible(true);
 */
public class HelpPage extends JFrame {

	private static final long serialVersionUID = 1L;

	public HelpPage() {
		HelpScreen screen = new HelpScreen();
		screen.setOnClose(this::dispose);

		setTitle("Pixies - Help");
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setResizable(false);
		add(screen);
		pack();
		setLocationRelativeTo(null);
	}
}

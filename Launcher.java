import javax.swing.SwingUtilities;

/** Entry point. Shows the main menu on the event dispatch thread, as Swing requires. */
public class Launcher {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> new MainMenu().setVisible(true));
	}
}

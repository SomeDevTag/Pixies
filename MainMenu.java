import java.io.File;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

/**
 * The window around the start screen. All the drawing and the input handling
 * lives in {@link StartScreen}, this just validates the choice and opens the
 * editor.
 */
public class MainMenu extends JFrame {

	private static final long serialVersionUID = 1L;

	private static final String DEFAULT_NAME = "Untitled";

	private final StartScreen screen;

	public MainMenu() {
		screen = new StartScreen(Panel.defaultPalette());
		screen.setOnStart(this::start);

		setTitle("Pixies");
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setResizable(false);
		add(screen);
		pack();
		setLocationRelativeTo(null);
	}

	/** Validates what was chosen, then hands over to the editor window. */
	private void start() {
		if (!screen.isOpenMode()) {
			String name = screen.projectName();
			if (name.isEmpty()) {
				name = DEFAULT_NAME;
			}
			new Renderer(name, screen.canvasSize());
			dispose();
			return;
		}

		String path = screen.selectedPath();
		if (path == null) {
			warn("Choose a file first.");
		} else if (!new File(path).isFile()) {
			warn("That file is not there any more.");
		} else if (!path.toLowerCase().endsWith(".pixies")) {
			// .rona carries no canvas size or palette, so there is nothing to rebuild from.
			warn("Only .pixies projects can be opened.");
		} else {
			new Renderer(path);
			dispose();
		}
	}

	private void warn(String message) {
		JOptionPane.showMessageDialog(this, message, "Pixies", JOptionPane.WARNING_MESSAGE);
	}
}

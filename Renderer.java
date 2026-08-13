import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.JFrame;

/** The editor window. It is just a frame around a single drawing {@link Panel}. */
public class Renderer extends JFrame {

	private static final long serialVersionUID = 1L;

	/** Opens a new, empty document of size x size pixels. */
	Renderer(String name, int size) {
		this(new Panel(name, size));
	}

	/** Opens an existing .pixies file. */
	Renderer(String path) {
		this(new Panel(path));
	}

	private Renderer(Panel panel) {
		// The panel already knows the document name, whether it was typed in the
		// main menu or read out of the file, so there is no need to parse it again.
		setTitle("Pixies - " + panel.getCanvasName());
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		add(panel);
		pack();

		// The minimum applies to the whole window, so the border and title bar have to
		// be added on. Without this the panel itself can end up narrower than 680 and
		// the toolbar runs into the palette.
		Insets insets = getInsets();
		setMinimumSize(new Dimension(680 + insets.left + insets.right,
				600 + insets.top + insets.bottom));
		setLocationRelativeTo(null);
		setVisible(true);
		panel.requestFocusInWindow(); // so the keyboard shortcuts work without clicking first
	}
}

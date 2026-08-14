import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/**
 * The little dialog that asks how an export should be sized before it runs.
 *
 * The values are remembered for the rest of the session, so exporting the same
 * thing twice doesn't mean typing the numbers in again.
 *
 * Usage:
 *   ExportOptions options = ExportOptions.ask(parent, "Export GIF", true, false);
 *   if (options != null) { saver.saveGif(options.scale, options.delayMs); }
 */
public class ExportOptions {

	private static final int MIN_SCALE = 1;
	private static final int MAX_SCALE = 64;

	/** Remembered between exports. */
	private static int lastScale = 20;
	private static int lastDelay = 150;
	private static int lastColumns = 0;

	/** How many image pixels each canvas pixel becomes. */
	public final int scale;

	/** Milliseconds each frame is shown for, GIF only. */
	public final int delayMs;

	/** Frames per row on a sprite sheet. 0 means put them all in one row. */
	public final int columns;

	private ExportOptions(int scale, int delayMs, int columns) {
		this.scale = scale;
		this.delayMs = delayMs;
		this.columns = columns;
	}

	/**
	 * Asks for the export settings. Pass true for the extra fields an export needs.
	 * Returns null if the user cancelled, in which case nothing should be written.
	 */
	public static ExportOptions ask(Component parent, String title, boolean withDelay, boolean withColumns) {
		JSpinner scale = new JSpinner(new SpinnerNumberModel(lastScale, MIN_SCALE, MAX_SCALE, 1));
		JSpinner delay = new JSpinner(new SpinnerNumberModel(lastDelay, 20, 2000, 10));
		JSpinner columns = new JSpinner(new SpinnerNumberModel(lastColumns, 0, 512, 1));

		JPanel form = new JPanel(new GridLayout(0, 2, 8, 6));
		form.add(new JLabel("Pixel size:"));
		form.add(scale);
		if (withDelay) {
			form.add(new JLabel("Frame delay (ms):"));
			form.add(delay);
		}
		if (withColumns) {
			form.add(new JLabel("Frames per row:"));
			form.add(columns);
			form.add(new JLabel());
			form.add(new JLabel("0 puts them all in one row"));
		}

		int choice = JOptionPane.showConfirmDialog(parent, form, title,
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (choice != JOptionPane.OK_OPTION) {
			return null;
		}

		lastScale = (Integer) scale.getValue();
		lastDelay = (Integer) delay.getValue();
		lastColumns = (Integer) columns.getValue();
		return new ExportOptions(lastScale, lastDelay, lastColumns);
	}
}

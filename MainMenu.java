import java.io.File;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;

/** Start screen: either create a new canvas or open an existing .pixies file. */
public class MainMenu extends JFrame {

	private static final long serialVersionUID = 1L;

	/** Labels shown in the size dropdown, paired index for index with CANVAS_SIZES. */
	private static final String[] SIZE_LABELS = { "8px", "16px", "32px", "64px", "128px", "30px (Rona)" };
	private static final int[] CANVAS_SIZES = { 8, 16, 32, 64, 128, 30 };

	private static final String DEFAULT_NAME = "Untitled";

	private ButtonGroup modeGroup;
	private JRadioButton newFileRadio;
	private JRadioButton openFileRadio;
	private JTextField nameField;
	private JComboBox<String> sizeCombo;
	private JTextField pathField;
	private JButton browseButton;
	private JButton startButton;
	private JLabel nameLabel;
	private JSeparator separator1;
	private JSeparator separator2;

	/** Absolute path of the file picked with Browse, or null if nothing was picked. */
	private String selectedPath;

	public MainMenu() {
		initComponents();
	}

	private void initComponents() {
		modeGroup = new ButtonGroup();
		newFileRadio = new JRadioButton();
		openFileRadio = new JRadioButton();
		startButton = new JButton();
		nameField = new JTextField();
		sizeCombo = new JComboBox<>();
		separator1 = new JSeparator();
		separator2 = new JSeparator();
		browseButton = new JButton();
		pathField = new JTextField();
		nameLabel = new JLabel();

		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setTitle("Pixies - Main Menu");
		setLocation(new java.awt.Point(300, 300));
		setResizable(false);

		modeGroup.add(newFileRadio);
		newFileRadio.setSelected(true);
		newFileRadio.setText("New file");

		modeGroup.add(openFileRadio);
		openFileRadio.setText("Open file");

		startButton.setText("Start");
		startButton.addActionListener(evt -> start());

		nameField.setText(DEFAULT_NAME);

		sizeCombo.setModel(new DefaultComboBoxModel<>(SIZE_LABELS));

		browseButton.setText("Select");
		browseButton.addActionListener(evt -> browse());

		pathField.setEditable(false);
		pathField.setText("No file selected");

		nameLabel.setText("File Name:");

		GroupLayout layout = new GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(
			layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup()
				.addContainerGap()
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addGroup(layout.createSequentialGroup()
						.addGap(24, 24, 24)
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
							.addComponent(newFileRadio)
							.addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
								.addComponent(sizeCombo, GroupLayout.Alignment.LEADING, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(nameField, GroupLayout.Alignment.LEADING)
								.addGroup(GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
									.addComponent(pathField, GroupLayout.PREFERRED_SIZE, 193, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
									.addComponent(browseButton)))
							.addComponent(nameLabel)
							.addComponent(openFileRadio))
						.addContainerGap(27, Short.MAX_VALUE))
					.addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
						.addComponent(separator1)
						.addContainerGap())))
			.addGroup(layout.createSequentialGroup()
				.addGap(30, 30, 30)
				.addComponent(startButton, GroupLayout.PREFERRED_SIZE, 278, GroupLayout.PREFERRED_SIZE)
				.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
			.addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
				.addContainerGap()
				.addComponent(separator2)
				.addContainerGap())
		);
		layout.setVerticalGroup(
			layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup()
				.addGap(24, 24, 24)
				.addComponent(newFileRadio)
				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
				.addComponent(nameLabel)
				.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
				.addComponent(nameField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
				.addComponent(sizeCombo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
				.addComponent(separator1, GroupLayout.PREFERRED_SIZE, 10, GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
				.addComponent(openFileRadio)
				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(pathField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addComponent(browseButton))
				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
				.addComponent(separator2, GroupLayout.PREFERRED_SIZE, 10, GroupLayout.PREFERRED_SIZE)
				.addGap(18, 18, 18)
				.addComponent(startButton)
				.addContainerGap(24, Short.MAX_VALUE))
		);

		pack();
	}

	/** Asks for a .pixies file. Cancelling simply leaves the previous choice alone. */
	private void browse() {
		JFileChooser chooser = new JFileChooser(new File("."));
		if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		selectedPath = chooser.getSelectedFile().getAbsolutePath();
		pathField.setText(selectedPath);
	}

	/** Validates the chosen options and hands over to the editor window. */
	private void start() {
		if (newFileRadio.isSelected()) {
			// Spaces would break the .pixies header, which is whitespace separated.
			String name = nameField.getText().trim().replace(" ", "");
			if (name.isEmpty()) {
				name = DEFAULT_NAME;
			}
			new Renderer(name, CANVAS_SIZES[sizeCombo.getSelectedIndex()]);
			dispose();
			return;
		}

		if (selectedPath == null) {
			warn("Pick a file first.");
			return;
		}
		if (!new File(selectedPath).isFile()) {
			warn("That file no longer exists.");
			return;
		}
		if (!selectedPath.toLowerCase().endsWith(".pixies")) {
			// .rona has no palette or canvas size in its header, so it cannot be loaded.
			warn("Only .pixies files can be opened.");
			return;
		}
		new Renderer(selectedPath);
		dispose();
	}

	private void warn(String message) {
		JOptionPane.showMessageDialog(this, message, "Pixies", JOptionPane.WARNING_MESSAGE);
	}
}

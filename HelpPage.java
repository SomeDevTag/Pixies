import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;

/** Read only list of the keyboard and mouse shortcuts. Opened with H. */
public class HelpPage extends JFrame {

	private static final long serialVersionUID = 1L;

	private static final String SHORTCUTS =
			"(LM) - Draw\n"
			+ "(RM drag) - Zoom\n"
			+ "(MM drag) / (Space) - Move canvas\n"
			+ "(E) - Open / close the tool menu\n"
			+ "(B) - Toggle brush and line\n"
			+ "(W) - Draw square\n"
			+ "(I) - Eyedropper, replaces the selected swatch\n"
			+ "(C) - Color wheel, replaces the selected swatch\n"
			+ "Drag a swatch onto the canvas to flood fill\n"
			+ "(O) - Onion skin\n"
			+ "(P) - Play animation on a loop\n"
			+ "([) - Previous page\n"
			+ "(]) - Next page\n"
			+ "(Ctrl+Z) - Undo\n"
			+ "(Ctrl+S) - Save .pixies\n"
			+ "(H) - Help";

	private JScrollPane scrollPane;
	private JTextArea shortcutList;
	private JButton okButton;

	public HelpPage() {
		initComponents();
	}

	private void initComponents() {
		scrollPane = new JScrollPane();
		shortcutList = new JTextArea();
		okButton = new JButton();

		setTitle("Pixies - Help");
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		shortcutList.setEditable(false);
		shortcutList.setColumns(20);
		shortcutList.setRows(5);
		shortcutList.setText(SHORTCUTS);
		scrollPane.setViewportView(shortcutList);

		okButton.setText("I understand!");
		okButton.addActionListener(evt -> dispose());

		GroupLayout layout = new GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(
			layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup()
				.addContainerGap()
				.addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, 320, GroupLayout.PREFERRED_SIZE)
				.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
			.addGroup(layout.createSequentialGroup()
				.addComponent(okButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
				.addContainerGap())
		);
		layout.setVerticalGroup(
			layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup()
				.addContainerGap()
				.addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, 260, GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
				.addComponent(okButton, GroupLayout.PREFERRED_SIZE, 35, GroupLayout.PREFERRED_SIZE)
				.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
		);

		pack();
		setLocationRelativeTo(null);
	}
}

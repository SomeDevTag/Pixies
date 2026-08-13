import java.awt.Color;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;

/**
 * Color wheel for editing the palette. Applying overwrites whichever swatch is
 * currently selected in the panel.
 *
 * Note: do not import java.awt.Panel here. A single type import would shadow this
 * project's own Panel class, which is what previously left Apply wired to nothing.
 */
public class ColorPicker extends JFrame {

	private static final long serialVersionUID = 1L;

	private final Panel panel;
	private JColorChooser colorChooser;
	private JButton applyButton;
	private JButton cancelButton;

	ColorPicker(Panel panel, Color initialColor) {
		this.panel = panel;
		initComponents(initialColor == null ? Color.WHITE : initialColor);
	}

	private void initComponents(Color initialColor) {
		colorChooser = new JColorChooser(initialColor);
		applyButton = new JButton();
		cancelButton = new JButton();

		setTitle("Pixies - Color Wheel");
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setResizable(false);

		applyButton.setText("Apply");
		applyButton.addActionListener(evt -> {
			panel.applyPickedColor(colorChooser.getColor());
			dispose();
		});

		cancelButton.setText("Cancel");
		cancelButton.addActionListener(evt -> dispose());

		GroupLayout layout = new GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(
			layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup()
				.addContainerGap()
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
					.addGroup(layout.createSequentialGroup()
						.addComponent(colorChooser, GroupLayout.PREFERRED_SIZE, 644, GroupLayout.PREFERRED_SIZE)
						.addGap(0, 0, Short.MAX_VALUE))
					.addGroup(layout.createSequentialGroup()
						.addGap(6, 6, 6)
						.addComponent(applyButton, GroupLayout.PREFERRED_SIZE, 297, GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(cancelButton, GroupLayout.PREFERRED_SIZE, 297, GroupLayout.PREFERRED_SIZE)))
				.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
		);
		layout.setVerticalGroup(
			layout.createParallelGroup(GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup()
				.addContainerGap()
				.addComponent(colorChooser, GroupLayout.PREFERRED_SIZE, 343, GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
					.addComponent(applyButton)
					.addComponent(cancelButton))
				.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
		);

		pack();
		setLocationRelativeTo(null);
	}
}

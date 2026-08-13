import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Scanner;

import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * The drawing surface and the entire editor UI. The canvas, the palette strip,
 * the tool menu and the page controls are all painted by hand and hit tested by
 * pixel coordinates, so the layout constants below are shared between
 * {@link #paintComponent} and {@link #mousePressed} to keep the two in step.
 */
public class Panel extends JPanel implements MouseListener, MouseMotionListener, KeyListener {

	private static final long serialVersionUID = 1L;

	// ---------------------------------------------------------------- layout

	private static final int TOOLBAR_HEIGHT = 30;
	private static final int SWATCH_SIZE = 25;
	private static final int PALETTE_COUNT = 16;
	private static final int PALETTE_X = 50;
	private static final int PALETTE_WIDTH = PALETTE_COUNT * SWATCH_SIZE;

	private static final int PAGE_ADD_X = 475;   // "+  -" adds and removes pages
	private static final int PAGE_ADD_WIDTH = 50;
	private static final int PAGE_NAV_X = 550;   // "<  n/m  >" steps between pages
	private static final int PAGE_NAV_WIDTH = 90;
	private static final int PLAY_X = 650;       // plays the animation once
	private static final int PLAY_WIDTH = 25;

	private static final int MENU_X = 10;
	private static final int MENU_Y = 10;
	private static final int MENU_WIDTH = 120;
	private static final int MENU_ITEM_HEIGHT = 25;

	// ---------------------------------------------------------------- tools

	/**
	 * Tool ids. They deliberately match the first four rows of TOOL_MENU so the
	 * open menu can highlight the active tool with a plain index comparison.
	 */
	private static final int TOOL_BRUSH = 1;
	private static final int TOOL_LINE = 2;
	private static final int TOOL_EYEDROPPER = 3;
	private static final int TOOL_RECT = 4;

	// Menu rows that are toggles rather than one off actions. The menu stays open
	// for these so the checkbox next to them can be seen changing.
	private static final int MENU_ONION_SKIN = 7;
	private static final int MENU_LOOP_PLAY = 8;

	private static final String[] TOOL_MENU = {
		"Cancel (E)",
		"Brush (B)",
		"Line (B)",
		"Eyedropper (I)",
		"Draw Square (W)",
		"Save PNGs",
		"Save .pixies (Ctrl+S)",
		"Onion Skin (O)",
		"Loop Play (P)",
		"Color Wheel (C)",
		"Duplicate Page",
		"Insert Blank Page",
		"Save .rona",
		"Undo (Ctrl+Z)"
	};

	// ---------------------------------------------------------------- misc

	private static final int DEFAULT_CANVAS_SIZE = 16;
	private static final int MIN_ZOOM = 1;
	private static final int MAX_ZOOM = 64;
	private static final int FRAME_DELAY_MS = 150;
	private static final int ONION_SKIN_ALPHA = 70;
	/** How many steps back the undo stack remembers. Older steps are dropped. */
	private static final int UNDO_LIMIT = 64;

	private static final Color UI_PANEL = new Color(75, 75, 75);
	private static final Color UI_LIGHT = new Color(215, 215, 215);
	private static final Color UI_SELECTED = new Color(180, 180, 180);
	private static final Color UI_HINT = new Color(95, 95, 95);
	private static final Stroke THIN_OUTLINE = new BasicStroke(2);
	private static final Stroke THICK_OUTLINE = new BasicStroke(4);

	// ---------------------------------------------------------------- document

	private final ArrayList<Page> pages = new ArrayList<>();
	private final Color[] palette = new Color[PALETTE_COUNT];
	private String canvasName;
	private int canvasSize = DEFAULT_CANVAS_SIZE;
	private int pageIndex = 0;

	/** Most recent state first. Pushed by beginEdit, popped by undo. */
	private final transient Deque<UndoEntry> undoStack = new ArrayDeque<>();

	// ---------------------------------------------------------------- playback

	/** A Swing timer, so page flipping happens on the same thread that paints. */
	private final Timer animationTimer = new Timer(FRAME_DELAY_MS, evt -> advanceAnimation());
	private boolean isLooping = false;
	private boolean playbackDone = true;
	private boolean holdFirstFrame = false;

	// ---------------------------------------------------------------- view

	private int zoom = 20;
	private int zoomAtDragStart = 20;
	private int camX = 0;
	private int camXAtDragStart = 0;
	private int camY = 0;
	private int camYAtDragStart = 0;
	private int mouseX = 0;
	private int mouseY = 0;
	private int dragStartX = 0;
	private int dragStartY = 0;

	// ---------------------------------------------------------------- input state

	private int selectedTool = TOOL_BRUSH;
	private int selectedSwatch = 0;
	private Color currentColor = Color.RED;
	private boolean menuOpen = false;
	private boolean onionSkinOn = false;
	private boolean isDrawing = false;
	private boolean isDrawingLine = false;
	private boolean isDrawingRect = false;
	private boolean isDraggingSwatch = false;
	private boolean isPanning = false;
	private boolean isZooming = false;
	private int rectStartX = 0;
	private int rectStartY = 0;
	private int lastPaintX = 0;
	private int lastPaintY = 0;

	/** New, empty document. */
	Panel(String name, int size) {
		canvasName = name;
		canvasSize = size;
		setDefaultPalette();
		pages.add(new Page(canvasSize));
		init();
	}

	/** Document read back from a .pixies file. */
	Panel(String path) {
		setDefaultPalette(); // so a partly read file still leaves us with a usable palette
		load(path);
		init();
	}

	private void init() {
		setFocusable(true);
		setBackground(UI_PANEL);
		setPreferredSize(new Dimension(680, 600));
		setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);

		currentColor = palette[0];

		// Start with the canvas filling most of the window and centred in it.
		zoom = clamp(570 / canvasSize, MIN_ZOOM, MAX_ZOOM);
		zoomAtDragStart = zoom;
		camX = camXAtDragStart = (680 - zoom * canvasSize) / 2;
		camY = camYAtDragStart = (570 - zoom * canvasSize) / 2;

		animationTimer.start();
	}

	public String getCanvasName() {
		return canvasName;
	}

	// ================================================================ helpers

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	/** Pixel array of the page currently on screen. */
	private Color[] currentPixels() {
		return pages.get(pageIndex).pixels;
	}

	/** Top edge of the bottom toolbar. Derived from the current size, never cached. */
	private int toolbarY() {
		return Math.max(0, getHeight() - TOOLBAR_HEIGHT);
	}

	/** True if the given screen position lands on a canvas pixel. */
	private boolean isOverCanvas(int screenX, int screenY) {
		if (screenX < camX || screenY < camY) {
			return false;
		}
		return (screenX - camX) / zoom < canvasSize && (screenY - camY) / zoom < canvasSize;
	}

	/** Paints one pixel under a screen position and remembers it. No-op off canvas. */
	private void paintAt(int screenX, int screenY) {
		if (!isOverCanvas(screenX, screenY)) {
			return;
		}
		lastPaintX = (screenX - camX) / zoom;
		lastPaintY = (screenY - camY) / zoom;
		currentPixels()[lastPaintY * canvasSize + lastPaintX] = currentColor;
	}

	/**
	 * The square being dragged out right now, in grid coordinates. Clamped to the
	 * canvas so dragging off the edge cannot produce out of range indices.
	 */
	private Rectangle currentSelection() {
		int x1 = clamp(rectStartX, 0, canvasSize - 1);
		int y1 = clamp(rectStartY, 0, canvasSize - 1);
		int x2 = clamp((mouseX - camX) / zoom, 0, canvasSize - 1);
		int y2 = clamp((mouseY - camY) / zoom, 0, canvasSize - 1);
		return new Rectangle(Math.min(x1, x2), Math.min(y1, y2),
				Math.abs(x2 - x1) + 1, Math.abs(y2 - y1) + 1);
	}

	// ================================================================ painting

	/**
	 * Draws the canvas, the optional onion skin of the previous page, the cursor
	 * preview and then the UI chrome on top.
	 *
	 * This overrides paintComponent rather than paint so that Swing keeps its
	 * double buffering and border handling.
	 */
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		int toolbarY = toolbarY();

		// The page itself.
		for (int x = 0; x < canvasSize; x++) {
			for (int y = 0; y < canvasSize; y++) {
				g2.setColor(currentPixels()[x + (y * canvasSize)]);
				g2.fillRect(camX + x * zoom, camY + y * zoom, zoom, zoom);
			}
		}

		// A translucent copy of the previous page, to line up animation frames.
		if (onionSkinOn && pageIndex > 0) {
			Color[] previous = pages.get(pageIndex - 1).pixels;
			for (int x = 0; x < canvasSize; x++) {
				for (int y = 0; y < canvasSize; y++) {
					Color c = previous[x + (y * canvasSize)];
					g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), ONION_SKIN_ALPHA));
					g2.fillRect(camX + x * zoom, camY + y * zoom, zoom, zoom);
				}
			}
		}

		g2.setStroke(THIN_OUTLINE);
		g2.setColor(Color.BLACK);
		paintCursor(g2);
		g2.drawRect(camX, camY, zoom * canvasSize, zoom * canvasSize);

		// Chrome last, so the canvas cannot cover it. The help hint used to be drawn
		// first, which meant it was hidden behind the canvas at every zoom level.
		String hint = "Press H for help";
		g2.setColor(UI_HINT);
		g2.drawString(hint, (getWidth() - g2.getFontMetrics().stringWidth(hint)) / 2, 30);

		paintPalette(g2, toolbarY);
		paintMenu(g2);
		paintPageControls(g2, toolbarY);
	}

	/** Outline of the pixel under the mouse, or of the square being dragged out. */
	private void paintCursor(Graphics2D g2) {
		if (isDrawingRect) {
			Rectangle r = currentSelection();
			g2.drawRect(camX + r.x * zoom, camY + r.y * zoom, r.width * zoom, r.height * zoom);
			return;
		}
		if (!isOverCanvas(mouseX, mouseY)) {
			return;
		}

		int x = (mouseX - camX) / zoom;
		int y = (mouseY - camY) / zoom;
		if (selectedTool == TOOL_LINE) {
			// Preview the colour that would be laid down, plus the rubber band back
			// to the last painted pixel while the button is held.
			g2.setColor(currentColor);
			g2.fillRect(camX + x * zoom, camY + y * zoom, zoom, zoom);
			g2.setColor(Color.BLACK);
			g2.drawRect(camX + x * zoom, camY + y * zoom, zoom, zoom);
			if (isDrawingLine) {
				g2.drawLine(camX + lastPaintX * zoom + zoom / 2, camY + lastPaintY * zoom + zoom / 2,
						mouseX, mouseY);
			}
		} else {
			g2.drawRect(camX + x * zoom, camY + y * zoom, zoom, zoom);
		}
	}

	/** The 16 swatch strip along the bottom, with the active one ringed in green. */
	private void paintPalette(Graphics2D g2, int toolbarY) {
		for (int i = 0; i < PALETTE_COUNT; i++) {
			g2.setColor(palette[i]);
			g2.fillRect(PALETTE_X + (i * SWATCH_SIZE), toolbarY, SWATCH_SIZE, SWATCH_SIZE);
		}
		g2.setColor(UI_LIGHT);
		g2.drawRect(PALETTE_X, toolbarY, PALETTE_WIDTH, SWATCH_SIZE);

		g2.setStroke(THICK_OUTLINE);
		g2.setColor(Color.GREEN);
		g2.drawRect(PALETTE_X + selectedSwatch * SWATCH_SIZE, toolbarY, SWATCH_SIZE, SWATCH_SIZE);
		g2.setStroke(THIN_OUTLINE);

		// While a swatch is being dragged, carry it under the cursor.
		if (isDraggingSwatch) {
			g2.setColor(currentColor);
			g2.fillRect(mouseX - zoom / 2, mouseY - zoom / 2, zoom, zoom);
		}
	}

	/** The tool menu, either expanded or collapsed to a single "E" button. */
	private void paintMenu(Graphics2D g2) {
		g2.setColor(Color.BLACK);
		if (onionSkinOn) {
			// Right aligned by measurement, it used to run off the edge of the window.
			String label = "onion skin";
			g2.drawString(label, getWidth() - g2.getFontMetrics().stringWidth(label) - 10, 26);
		}

		if (!menuOpen) {
			g2.setColor(UI_LIGHT);
			g2.fillRect(MENU_X, MENU_Y, SWATCH_SIZE, SWATCH_SIZE);
			g2.setColor(Color.BLACK);
			g2.drawString("E", MENU_X + 8, MENU_Y + 16);
			g2.drawString(TOOL_MENU[selectedTool], MENU_X + 33, MENU_Y + 16);
			return;
		}

		for (int i = 0; i < TOOL_MENU.length; i++) {
			int rowY = MENU_Y + (i * MENU_ITEM_HEIGHT);
			g2.setColor(i == selectedTool ? UI_SELECTED : UI_LIGHT);
			g2.fillRect(MENU_X, rowY, MENU_WIDTH, MENU_ITEM_HEIGHT);
			g2.setColor(UI_PANEL);
			g2.fillRect(MENU_X, rowY + MENU_ITEM_HEIGHT - 1, MENU_WIDTH, 1); // row divider
			g2.drawString(TOOL_MENU[i], MENU_X + 8, rowY + 16);
		}

		// Checkboxes for the two rows that are toggles.
		paintCheckbox(g2, MENU_ONION_SKIN, onionSkinOn);
		paintCheckbox(g2, MENU_LOOP_PLAY, isLooping);
	}

	private void paintCheckbox(Graphics2D g2, int menuRow, boolean checked) {
		int boxY = MENU_Y + 6 + (menuRow * MENU_ITEM_HEIGHT);
		g2.setColor(UI_PANEL);
		if (checked) {
			g2.fillRect(MENU_X + 101, boxY, 13, 13);
		} else {
			g2.drawRect(MENU_X + 101, boxY, 13, 13);
		}
	}

	/** Add/remove page, previous/next page and the play button. */
	private void paintPageControls(Graphics2D g2, int toolbarY) {
		g2.setColor(UI_LIGHT);
		g2.fillRect(PAGE_ADD_X, toolbarY, PAGE_ADD_WIDTH, SWATCH_SIZE);
		g2.fillRect(PAGE_NAV_X, toolbarY, PAGE_NAV_WIDTH, SWATCH_SIZE);
		g2.fillRect(PLAY_X, toolbarY, PLAY_WIDTH, SWATCH_SIZE);

		// Dividers splitting each button into its two halves.
		g2.setColor(UI_PANEL);
		g2.fillRect(PAGE_ADD_X + 24, toolbarY + 1, 2, 23);
		g2.fillRect(PAGE_NAV_X + 19, toolbarY + 1, 2, 23);
		g2.fillRect(PAGE_NAV_X + 69, toolbarY + 1, 2, 23);

		g2.setColor(Color.BLACK);
		g2.drawString("+      --", PAGE_ADD_X + 8, toolbarY + 15);
		g2.drawString("<                   >", PAGE_NAV_X + 6, toolbarY + 16);
		g2.drawString((pageIndex + 1) + " / " + pages.size(), PAGE_NAV_X + 23, toolbarY + 16);

		// Play triangle. The button used to be drawn blank.
		g2.fillPolygon(
				new int[] { PLAY_X + 9, PLAY_X + 9, PLAY_X + 18 },
				new int[] { toolbarY + 7, toolbarY + 18, toolbarY + 12 }, 3);
	}

	/** The default 16 colour palette. */
	private void setDefaultPalette() {
		palette[0] = new Color(255, 69, 0);
		palette[1] = new Color(255, 168, 0);
		palette[2] = new Color(255, 214, 53);
		palette[3] = new Color(0, 163, 104);
		palette[4] = new Color(126, 237, 86);
		palette[5] = new Color(36, 80, 164);
		palette[6] = new Color(54, 144, 234);
		palette[7] = new Color(81, 223, 244);
		palette[8] = new Color(129, 30, 159);
		palette[9] = new Color(180, 74, 192);
		palette[10] = new Color(255, 153, 170);
		palette[11] = new Color(157, 105, 38);
		palette[12] = new Color(0, 0, 0);
		palette[13] = new Color(137, 141, 144);
		palette[14] = new Color(212, 215, 217);
		palette[15] = new Color(255, 255, 255);
	}

	/** Called by the colour wheel when Apply is pressed. */
	public void applyPickedColor(Color c) {
		if (c == null || c.equals(palette[selectedSwatch])) {
			return;
		}
		beginEdit();
		palette[selectedSwatch] = c;
		currentColor = c;
		repaint();
	}

	// ================================================================ animation

	/** Starts a single run through the pages. Ignored while looping. */
	public void playAnimation() {
		if (!isLooping) {
			pageIndex = 0;
			holdFirstFrame = true;
			playbackDone = false;
		}
	}

	private void toggleLoop() {
		isLooping = !isLooping;
		pageIndex = 0;
	}

	/** One tick of the playback timer. Does nothing unless something is playing. */
	private void advanceAnimation() {
		if (isLooping) {
			pageIndex = (pageIndex + 1) % pages.size();
			repaint();
		} else if (!playbackDone) {
			if (holdFirstFrame) {
				holdFirstFrame = false; // let frame 0 sit on screen for one tick
			} else if (pageIndex + 1 < pages.size()) {
				pageIndex++;
			} else {
				playbackDone = true;
				return;
			}
			repaint();
		}
	}

	// ================================================================ undo

	/**
	 * Records the current state so {@link #undo} can come back to it, and makes the
	 * page that is about to change unique.
	 *
	 * Call this once per edit, not once per painted pixel: a whole brush stroke is
	 * meant to disappear in a single undo. Callers must also check first that the
	 * edit really is going to change something, so the stack does not fill up with
	 * steps that do nothing.
	 */
	private void beginEdit() {
		if (undoStack.size() >= UNDO_LIMIT) {
			undoStack.removeLast(); // drop the oldest step
		}
		undoStack.push(new UndoEntry(pages, pageIndex, palette));

		// The entry shares Page objects with the live document, so hand the caller a
		// copy to draw on and leave the untouched original in the entry.
		Page copy = new Page(canvasSize);
		System.arraycopy(currentPixels(), 0, copy.pixels, 0, copy.pixels.length);
		pages.set(pageIndex, copy);
	}

	/** Steps back to the state recorded by the most recent {@link #beginEdit}. */
	private void undo() {
		if (undoStack.isEmpty()) {
			return;
		}
		UndoEntry entry = undoStack.pop();
		pages.clear();
		pages.addAll(entry.pages);
		pageIndex = clamp(entry.pageIndex, 0, pages.size() - 1);
		System.arraycopy(entry.palette, 0, palette, 0, palette.length);
		currentColor = palette[selectedSwatch]; // the swatch may have just been restored
	}

	// ================================================================ editing

	/**
	 * Flood fills the connected run of pixels matching target, starting at (x, y).
	 *
	 * Iterative rather than recursive: a recursive version overflows the stack on
	 * a large canvas, and it never terminated at all when the fill colour matched
	 * the target, because every pixel it painted still compared equal to it.
	 */
	private void floodFill(int startX, int startY, Color target) {
		if (currentColor.equals(target)) {
			return; // already that colour, nothing to do
		}
		Color[] pixels = currentPixels();
		Deque<Point> pending = new ArrayDeque<>();
		pending.add(new Point(startX, startY));

		while (!pending.isEmpty()) {
			Point p = pending.poll();
			if (p.x < 0 || p.x >= canvasSize || p.y < 0 || p.y >= canvasSize) {
				continue;
			}
			int i = p.y * canvasSize + p.x;
			if (!pixels[i].equals(target)) {
				continue;
			}
			pixels[i] = currentColor;
			pending.add(new Point(p.x + 1, p.y));
			pending.add(new Point(p.x - 1, p.y));
			pending.add(new Point(p.x, p.y + 1));
			pending.add(new Point(p.x, p.y - 1));
		}
	}

	/** Fills the square that was just dragged out. */
	private void fillSelection() {
		Rectangle r = currentSelection();
		Color[] pixels = currentPixels();
		for (int x = r.x; x < r.x + r.width; x++) {
			for (int y = r.y; y < r.y + r.height; y++) {
				pixels[y * canvasSize + x] = currentColor;
			}
		}
	}

	private void addPage() {
		beginEdit();
		pages.add(new Page(canvasSize));
		pageIndex = pages.size() - 1;
	}

	/** Removes the current page. The document always keeps at least one page. */
	private void removePage() {
		if (pages.size() <= 1) {
			return;
		}
		beginEdit();
		pages.remove(pageIndex);
		if (pageIndex >= pages.size()) {
			pageIndex = pages.size() - 1;
		}
	}

	private void insertPageAfterCurrent(boolean copyCurrent) {
		beginEdit();
		Page page = new Page(canvasSize);
		if (copyCurrent) {
			System.arraycopy(currentPixels(), 0, page.pixels, 0, page.pixels.length);
		}
		pages.add(pageIndex + 1, page);
		pageIndex++;
	}

	// ================================================================ mouse

	@Override
	public void mousePressed(MouseEvent e) {
		requestFocusInWindow();

		if (e.getButton() == MouseEvent.BUTTON1) {
			if (e.getY() > toolbarY()) {
				handleToolbarClick(e.getX());
			} else if (isMenuHit(e.getX(), e.getY())) {
				handleMenuClick(e.getY());
			} else {
				handleCanvasPress(e.getX(), e.getY());
			}
		} else if (e.getButton() == MouseEvent.BUTTON3 && !isPanning) {
			isZooming = true;
			dragStartX = e.getX();
		} else if (e.getButton() == MouseEvent.BUTTON2 && !isZooming) {
			isPanning = true;
			dragStartX = e.getX();
			dragStartY = e.getY();
		}

		repaint();
	}

	private void handleToolbarClick(int x) {
		if (x >= PALETTE_X && x < PALETTE_X + PALETTE_WIDTH) {
			// Picking a swatch also arms the fill tool: release over the canvas fills.
			selectedSwatch = clamp((x - PALETTE_X) / SWATCH_SIZE, 0, PALETTE_COUNT - 1);
			currentColor = palette[selectedSwatch];
			isDraggingSwatch = true;
		} else if (x >= PAGE_ADD_X && x < PAGE_ADD_X + PAGE_ADD_WIDTH) {
			if (x > PAGE_ADD_X + PAGE_ADD_WIDTH / 2) {
				removePage();
			} else {
				addPage();
			}
		} else if (x >= PAGE_NAV_X && x < PAGE_NAV_X + PAGE_NAV_WIDTH) {
			if (x > PAGE_NAV_X + PAGE_NAV_WIDTH / 2) {
				if (pageIndex + 1 < pages.size()) {
					pageIndex++;
				}
			} else if (pageIndex > 0) {
				pageIndex--;
			}
		} else if (x >= PLAY_X && x < PLAY_X + PLAY_WIDTH) {
			playAnimation();
		}
	}

	private boolean isMenuHit(int x, int y) {
		if (x < MENU_X || y < MENU_Y) {
			return false;
		}
		if (!menuOpen) {
			return x < MENU_X + SWATCH_SIZE && y < MENU_Y + SWATCH_SIZE;
		}
		return x < MENU_X + MENU_WIDTH && y < MENU_Y + TOOL_MENU.length * MENU_ITEM_HEIGHT;
	}

	private void handleMenuClick(int y) {
		if (!menuOpen) {
			menuOpen = true;
			return;
		}

		int row = (y - MENU_Y) / MENU_ITEM_HEIGHT;
		switch (row) {
		case 0: // cancel
			break;
		case TOOL_BRUSH:
		case TOOL_LINE:
		case TOOL_EYEDROPPER:
		case TOOL_RECT:
			selectedTool = row;
			break;
		case 5:
			newSaver().savePng();
			break;
		case 6:
			newSaver().savePixies();
			break;
		case MENU_ONION_SKIN:
			onionSkinOn = !onionSkinOn;
			return; // leave the menu open so the checkbox is visible
		case MENU_LOOP_PLAY:
			toggleLoop();
			return;
		case 9:
			new ColorPicker(this, currentColor).setVisible(true);
			break;
		case 10:
			insertPageAfterCurrent(true);
			break;
		case 11:
			insertPageAfterCurrent(false);
			break;
		case 12:
			newSaver().saveRona();
			break;
		case 13:
			undo();
			break;
		default:
			return;
		}
		menuOpen = false; // every one off action closes the menu behind it
	}

	private Saving newSaver() {
		return new Saving(pages, canvasSize, canvasName, palette);
	}

	private void handleCanvasPress(int x, int y) {
		if (!isOverCanvas(x, y)) {
			return;
		}
		switch (selectedTool) {
		case TOOL_BRUSH:
			beginEdit(); // once for the whole stroke, not once per pixel dragged over
			isDrawing = true;
			paintAt(x, y);
			break;
		case TOOL_LINE:
			beginEdit();
			isDrawingLine = true;
			paintAt(x, y);
			break;
		case TOOL_EYEDROPPER:
			// Lift the colour into the selected swatch, then go back to the brush.
			// This overwrites a palette entry, so it is undoable like any other edit.
			Color lifted = currentPixels()[((y - camY) / zoom) * canvasSize + ((x - camX) / zoom)];
			if (!lifted.equals(palette[selectedSwatch])) {
				beginEdit();
				palette[selectedSwatch] = lifted;
				currentColor = lifted;
			}
			selectedTool = TOOL_BRUSH;
			break;
		case TOOL_RECT:
			beginEdit();
			isDrawingRect = true;
			rectStartX = (x - camX) / zoom;
			rectStartY = (y - camY) / zoom;
			break;
		default:
			break;
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (isDrawingLine) {
			isDrawingLine = false;
			paintAt(e.getX(), e.getY());
		}
		isDrawing = false;

		if (isDrawingRect) {
			isDrawingRect = false;
			fillSelection();
		}
		if (isZooming) {
			isZooming = false;
			zoomAtDragStart = zoom;
			camXAtDragStart = camX;
			camYAtDragStart = camY;
		}
		if (isPanning) {
			isPanning = false;
			camXAtDragStart = camX;
			camYAtDragStart = camY;
		}
		if (isDraggingSwatch) {
			// A swatch dragged from the palette onto the canvas flood fills there.
			isDraggingSwatch = false;
			if (e.getY() <= toolbarY() && isOverCanvas(e.getX(), e.getY())) {
				int x = (e.getX() - camX) / zoom;
				int y = (e.getY() - camY) / zoom;
				Color target = currentPixels()[y * canvasSize + x];
				if (!currentColor.equals(target)) {
					beginEdit();
					floodFill(x, y, target);
				}
			}
		}

		repaint();
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		mouseX = e.getX();
		mouseY = e.getY();

		if (isDrawing) {
			paintAt(mouseX, mouseY);
		}

		if (isDrawingLine) {
			// Only lay down another pixel once the mouse has left the last one, so a
			// slow drag does not repaint the same cell over and over.
			int cellCentreX = camX + lastPaintX * zoom + zoom / 2;
			int cellCentreY = camY + lastPaintY * zoom + zoom / 2;
			if (Math.hypot(mouseX - cellCentreX, mouseY - cellCentreY) > zoom) {
				paintAt(mouseX, mouseY);
			}
		}

		if (isZooming) {
			// Horizontal drag distance scales the zoom, and the camera shifts by the
			// same amount so the canvas grows away from the pointer rather than the
			// top left corner. It is an approximation, not a true zoom about a point.
			int dx = mouseX - dragStartX;
			zoom = clamp(dx / Math.max(1, canvasSize / 2) + zoomAtDragStart, MIN_ZOOM, MAX_ZOOM);
			camX = camXAtDragStart - dx;
			camY = camYAtDragStart - dx;
		}

		if (isPanning) {
			camX = (mouseX - dragStartX) + camXAtDragStart;
			camY = (mouseY - dragStartY) + camYAtDragStart;
		}

		repaint();
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		mouseX = e.getX();
		mouseY = e.getY();
		if (isPanning) { // panning with the space bar held, no button is down
			camX = (mouseX - dragStartX) + camXAtDragStart;
			camY = (mouseY - dragStartY) + camYAtDragStart;
		}
		repaint();
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// Presses and releases are handled separately.
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	// ================================================================ keyboard

	/**
	 * Shortcuts are handled here rather than in keyTyped so that modifiers and the
	 * space bar can be read reliably from the key code.
	 */
	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_SPACE) {
			if (!isZooming && !isPanning) {
				isPanning = true;
				dragStartX = mouseX; // last known position, getMousePosition can be null
				dragStartY = mouseY;
			}
			return;
		}

		if (isDrawing || isDrawingLine || isDrawingRect) {
			return; // don't switch tools in the middle of a stroke
		}

		if (e.isControlDown()) {
			if (e.getKeyCode() == KeyEvent.VK_S) {
				newSaver().savePixies();
			} else if (e.getKeyCode() == KeyEvent.VK_Z) {
				undo();
				repaint();
			}
			return;
		}

		switch (e.getKeyCode()) {
		case KeyEvent.VK_P:
			toggleLoop();
			break;
		case KeyEvent.VK_OPEN_BRACKET:
			if (pageIndex > 0) {
				pageIndex--;
			}
			break;
		case KeyEvent.VK_CLOSE_BRACKET:
			if (pageIndex + 1 < pages.size()) {
				pageIndex++;
			}
			break;
		case KeyEvent.VK_H:
			new HelpPage().setVisible(true);
			break;
		case KeyEvent.VK_O:
			onionSkinOn = !onionSkinOn;
			break;
		case KeyEvent.VK_C:
			new ColorPicker(this, currentColor).setVisible(true);
			break;
		case KeyEvent.VK_E:
			menuOpen = !menuOpen;
			break;
		case KeyEvent.VK_I:
			selectedTool = TOOL_EYEDROPPER;
			break;
		case KeyEvent.VK_B:
			selectedTool = (selectedTool == TOOL_BRUSH) ? TOOL_LINE : TOOL_BRUSH;
			break;
		case KeyEvent.VK_W:
			selectedTool = TOOL_RECT;
			break;
		default:
			return;
		}
		repaint();
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_SPACE && isPanning) {
			isPanning = false;
			camXAtDragStart = camX;
			camYAtDragStart = camY;
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// Everything is handled in keyPressed.
	}

	// ================================================================ loading

	/**
	 * Reads a .pixies file written by {@link Saving#savePixies}. A malformed file
	 * leaves whatever was read so far, and falls back to a blank canvas rather
	 * than leaving the panel with no pages to paint.
	 */
	private void load(String path) {
		try (Scanner in = new Scanner(new File(path))) {
			canvasName = in.next();
			int pageCount = in.nextInt();
			canvasSize = in.nextInt();

			for (int i = 0; i < PALETTE_COUNT; i++) {
				palette[i] = new Color(in.nextInt(), in.nextInt(), in.nextInt());
			}
			for (int p = 0; p < pageCount; p++) {
				Page page = new Page(canvasSize);
				for (int i = 0; i < canvasSize * canvasSize; i++) {
					page.pixels[i] = new Color(in.nextInt(), in.nextInt(), in.nextInt());
				}
				pages.add(page); // only added once it is complete
			}
		} catch (Exception e) {
			System.out.println("Could not read " + path + ": " + e);
		}

		if (pages.isEmpty()) {
			canvasName = "Untitled";
			canvasSize = DEFAULT_CANVAS_SIZE;
			setDefaultPalette();
			pages.add(new Page(canvasSize));
		}
	}
}

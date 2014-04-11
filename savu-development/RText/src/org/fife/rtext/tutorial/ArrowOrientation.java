package org.fife.rtext.tutorial;

/**
 * Enum to represent the four types of arrows that need to be drawn in the tutorial.
 * If you add a new possible direction, you will also need to add it to the switch statement
 * in TutorialGlassPane.paintComponent(Graphics g)
 * @author nathanroberts
 *
 */
public enum ArrowOrientation {
	/**
	 * small arrow pointing up
	 */
	UP, 
	/**
	 * small arrow pointing down
	 */
	DOWN, 
	/**
	 * large arrow point right (for debug pane)
	 */
	RIGHT, 
	/**
	 * large arrow pointing down (for debug pane)
	 */
	OUTPUT
}

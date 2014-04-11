package org.fife.rtext.tutorial;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Line2D;
import java.awt.geom.Line2D.Double;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import org.fife.rtext.RTextTabbedPaneView;
import org.fife.rtext.Savu;
import org.fife.rtext.RTextTabbedPaneView.TabbedPane;
import org.fife.rtext.plugins.debug.DebugPlugin;
import org.fife.rtext.tutorial.Tutorial.TutorialStep;
import org.fife.ui.app.AbstractGUIApplication;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.IconRowHeader;

public class TutorialGlassPane extends JComponent {

	private final float HIGHLIGHT_WIDTH = 3f;
	private final int HIGHLIGHT_BORDER_SIZE = 3;
	private final Color HIGHLIGHT_COLOR = Color.RED;
	
	private static final int TUTORIAL_PANEL_HEIGHT = 150; //TODO finalize this number
	private static final Color PANEL_BACKGROUND_COLOR = new Color(40,40,30);
	private static final Color TEXT_COLOR = new Color(255,240,210);
	
	private Savu savu;
	private DebugPlugin debugPlug;
	private StepController stepController;
	
	private JLabel tutMessage;
	private JButton nextButton;
	private JButton exitButton;
	
	private Component highlightedComponent = null;
	private ArrowOrientation arrowOr = null;
	private JPanel tutorialPanel = null;
	
	
	public TutorialGlassPane(Savu savu, JPanel tutorialPanel, StepController controller, DebugPlugin debugPlug) {
		this.savu = savu;
		this.tutorialPanel = tutorialPanel;
		this.stepController = controller;
		this.debugPlug = debugPlug;
		initTutorialPanel();
		this.addMouseListener(new GlassPaneMouseListener());
		this.addMouseMotionListener(new GlassPaneMouseListener());
		this.addMouseWheelListener(new GlassPaneMouseListener());
	}
	
	public JButton getNextButton() {
		return nextButton;
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		if (highlightedComponent != null) { //If a component should be highlighted, draw the highlighting.
			g2d.setColor(HIGHLIGHT_COLOR);
			g2d.setStroke(new BasicStroke(HIGHLIGHT_WIDTH));			
			Rectangle r = highlightedComponent.getBounds();
			r = SwingUtilities.convertRectangle(highlightedComponent.getParent(), r, savu.getActualContentPane());
			r.grow(HIGHLIGHT_BORDER_SIZE, HIGHLIGHT_BORDER_SIZE);
			g2d.draw(r);
			
			// Draw arrow if desired
			if(arrowOr != null){
				double x1 = 0;
				double y1 = 0;
				Polygon arrowhead = new Polygon();
				switch (arrowOr){
					case UP:
						//Arrow that points up, used for most buttons
						x1 = r.x+(r.width/2);
						y1 = r.y+r.height+5;
						arrowhead.addPoint((int)x1,(int)y1);
						arrowhead.addPoint((int)x1-8, (int)y1+16);
						arrowhead.addPoint((int)x1+8, (int)y1+16);
						break;
					case DOWN:
						//Used to point at the gutter
						x1 = r.x+(r.width/2);
						y1 = r.y-5;
						arrowhead.addPoint((int)x1,(int)y1);
						arrowhead.addPoint((int)x1-8, (int)y1-16);
						arrowhead.addPoint((int)x1+8, (int)y1-16);
						break;
					case RIGHT:
						//Arrow pointing to the Debug pane
						x1 = r.x-5;
						y1 = r.y+(r.height/2);
						arrowhead.addPoint((int)x1, (int)y1);
						arrowhead.addPoint((int)x1-36, (int)y1-18);
						arrowhead.addPoint((int)x1-36, (int)y1+18);
						break;
					case OUTPUT:
						//Used to point at run output
						x1 = r.x+(r.width/2);
						y1 = r.y-5;
						arrowhead.addPoint((int)x1,(int)y1);
						arrowhead.addPoint((int)x1-18, (int)y1-36);
						arrowhead.addPoint((int)x1+18, (int)y1-36);
						break;
					default:
						/* Since the switch statement is based on the ArrowOrientation enum, as long
						* as possible orientations are added to both ArrowOrientation.java and this
						* switch statement, this default should never be reached.
						*/
						System.out.println("This is a terrible enum then...");
						break;
				}
				g2d.fillPolygon(arrowhead);
			}
		}
	}
	
	public void setMessage(final String s) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				tutMessage.setText(s);
			}
		});
	}
	
	public void highlightComponent(final TutorialStep s) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				highlightedComponent = s.toHighlight;
				arrowOr = s.orientation;
				TutorialGlassPane.this.repaint();
			}
		});
	}
	
	/**
	 * Initializes the tutorial panel, creating the JLabel used for display and the tutorial buttons.
	 */
	private void initTutorialPanel() {
		
		tutorialPanel.setLayout(new BorderLayout());
		tutorialPanel.setBackground(PANEL_BACKGROUND_COLOR);
		tutorialPanel.setForeground(TEXT_COLOR);
		JPanel tutorialPanelCover = new JPanel();
		tutorialPanelCover.setLayout(new BoxLayout(tutorialPanelCover, BoxLayout.Y_AXIS));
		tutorialPanelCover.setBackground(PANEL_BACKGROUND_COLOR);
		tutorialPanelCover.setForeground(TEXT_COLOR);
		tutorialPanel.add(tutorialPanelCover, BorderLayout.NORTH);
		
		//Initialize and add the tutorial message
		Border margin = BorderFactory.createEmptyBorder(10, 10, 0, 10);
		tutMessage = new JLabel("");
		tutMessage.setFont(tutMessage.getFont().deriveFont(14.0f));
		tutMessage.setForeground(TEXT_COLOR);
		tutMessage.setBorder(margin);
		tutMessage.setAlignmentX((float) 0.0);
		tutorialPanelCover.add(tutMessage);
		tutorialPanel.setPreferredSize(new Dimension(1000, TUTORIAL_PANEL_HEIGHT));
		
		//Create the east button panel
		JPanel buttonPanel = new JPanel();
		margin = BorderFactory.createEmptyBorder(10, 10, 10, 10);
		buttonPanel.setBorder(margin);
		buttonPanel.setBackground(PANEL_BACKGROUND_COLOR);
		buttonPanel.setAlignmentX((float) 0.0);
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		
		//Initialize an Exit button to use during the tutorial
		exitButton = new JButton("Exit Tutorial");
		exitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stepController.alertTutorialExited();
			}
		});
		buttonPanel.add(exitButton);
				
		//Initialize a Continue button to use during the tutorial
		nextButton = new JButton("Next Step");
		nextButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stepController.alertStepFinished();
			}
		});
		buttonPanel.add(nextButton);
		nextButton.setVisible(false);
		
		
		
		tutorialPanelCover.add(buttonPanel);
		tutorialPanel.setVisible(true);
	}
	
	public JButton getExitButton(){
		return this.exitButton;
	}
	
	public void cleanUp() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				tutorialPanel.removeAll();
				tutorialPanel.setVisible(false);
				TutorialGlassPane.this.setVisible(false);
				savu.getMainView().closeCurrentDocument(); //Close the open tutorial file; assumes that the current file is the tutorial file
			}
		});
	}
	
	/**
	 * set visibility for the next/continue button.
	 * @param vis
	 */
	public void setNextVisibility(boolean vis){
		this.nextButton.setVisible(vis);
	}

	class GlassPaneMouseListener implements MouseListener, MouseMotionListener, MouseWheelListener {

		//Inspired by: http://stackoverflow.com/questions/13260145/how-to-click-through-jglasspane-with-mouselistener-to-ui-behind-it
		private void dispatchEvent(MouseEvent e) {
			if (e.isPopupTrigger())
				return; //Ignore all right clicks
	        Point glassPanePoint = e.getPoint();
	        Container container = savu.getActualContentPane();
	        Point containerPoint = SwingUtilities.convertPoint(TutorialGlassPane.this, glassPanePoint, container);
            // Find out exactly which component it's over.
            Component component = SwingUtilities.getDeepestComponentAt(container, containerPoint.x, containerPoint.y);
            //System.out.println(component.getClass());
            Component parent = component;
            while (parent != null) {
            	if (parent.getMouseListeners().length > 0) {        	
                	if ((!isRestrictedComponent(component)) //If it isn't a restricted component, then we're fine
                			|| nextButton.equals(component) || exitButton.equals(component) //If it's one of the tutorial's buttons, then we're fine
                			|| (highlightedComponent != null && highlightedComponent.equals(component))) { //If it's the highlighted component, then we're fine
                		//Forward this event to the component
                		Point componentPoint = SwingUtilities.convertPoint(TutorialGlassPane.this, glassPanePoint, parent);
                		parent.dispatchEvent(new MouseEvent(parent, e.getID(), e.getWhen(), e.getModifiers(),componentPoint.x, componentPoint.y, e.getClickCount(), e.isPopupTrigger()));
                	}
            		break;
            	}
            	parent = parent.getParent();
            }
	    }
		
		/**
		 * Returns true if the given component is considered to be "restricted". I.e., we don't want to pass on mouse events to it while in tutorial mode.
		 * @param comp The component to examine.
		 * @return True or false if the component is restricted or allowed, respectively.
		 */
		private boolean isRestrictedComponent(Component comp) { //TODO Determine which components should be restricted.
			if (comp instanceof JButton) {
				return true; //All buttons are restricted.
			}
			if (comp instanceof IconRowHeader) {
				return true; //Gutter is restricted.
			}
			if (comp instanceof TabbedPane) { //TODO restrict changes in another way as well; Still not perfectly secure. Do not allow any call
				return true;
			}
			//TODO disallow calls to menu items; maybe just disable each of the submenus.
        	return false;
		}
		
		private void dispatchEventAlways(MouseEvent e) {
	        Point glassPanePoint = e.getPoint();
	        Container container = savu.getActualContentPane();
	        Point containerPoint = SwingUtilities.convertPoint(TutorialGlassPane.this, glassPanePoint, container);
            // Find out exactly which component it's over.
            Component component = SwingUtilities.getDeepestComponentAt(container, containerPoint.x, containerPoint.y);
            Component parent = component;
            while (parent != null) {
            	if (parent.getMouseListeners().length > 0) {
            		//Forward this event to the component
            		Point componentPoint = SwingUtilities.convertPoint(TutorialGlassPane.this, glassPanePoint, parent);
            		parent.dispatchEvent(new MouseEvent(parent, e.getID(), e.getWhen(), e.getModifiers(),componentPoint.x, componentPoint.y, e.getClickCount(), e.isPopupTrigger()));
            		break;
            	}
            	parent = parent.getParent();
            }
	    }
		
		private void dispatchEventMouseMotion(MouseEvent e) {			
	        Point glassPanePoint = e.getPoint();
	        Container container = savu.getActualContentPane();
	        Point containerPoint = SwingUtilities.convertPoint(TutorialGlassPane.this, glassPanePoint, container);
            // Find out exactly which component it's over.
            Component component = SwingUtilities.getDeepestComponentAt(container, containerPoint.x, containerPoint.y);
            Component parent = component;
            while (parent != null) {
            	if (parent.getMouseMotionListeners().length > 0) {
            		//Forward this event to the component
            		Point componentPoint = SwingUtilities.convertPoint(TutorialGlassPane.this, glassPanePoint, parent);
                	component.dispatchEvent(new MouseEvent(parent, e.getID(), e.getWhen(), e.getModifiers(),componentPoint.x, componentPoint.y, e.getClickCount(), e.isPopupTrigger()));
            		break;
            	}
            	parent = parent.getParent();
            }
	    }
		
		private void dispatchEventMouseWheel(MouseEvent e) {			
			//Get the location over the pane of this mouse wheel event
			Point glassPanePoint = e.getPoint();
	        Container container = savu.getActualContentPane();
	        Point containerPoint = SwingUtilities.convertPoint(TutorialGlassPane.this, glassPanePoint, container);
            // Find out exactly which component it's over.
            Component component = SwingUtilities.getDeepestComponentAt(container, containerPoint.x, containerPoint.y);
            Component parent = component;
            while (parent != null) {
            	if (parent.getMouseWheelListeners().length > 0) {
            		e.setSource(parent);
            		parent.dispatchEvent(e); //Dispatch the event to the lowest component that actually has a mouse wheel listener
            		break;
            	}
            	parent = parent.getParent();
            }
	    }
		
		@Override
		public void mouseClicked(MouseEvent e) {
			dispatchEvent(e);
		}

		@Override
		public void mousePressed(MouseEvent e) {
			dispatchEvent(e);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			dispatchEvent(e);
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			dispatchEventAlways(e);
		}

		@Override
		public void mouseExited(MouseEvent e) {
			dispatchEventAlways(e);
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			//dispatchEventMouseMotion(e);
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			//dispatchEventMouseMotion(e);
		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			//dispatchEventMouseWheel(e);	
		}
		
	}
	
}


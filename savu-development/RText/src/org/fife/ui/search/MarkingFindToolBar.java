package org.fife.ui.search;
import javax.swing.Icon;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.plaf.metal.MetalComboBoxButton;
import javax.swing.text.JTextComponent;

import org.fife.rsta.ui.AssistanceIconPanel;
import org.fife.rsta.ui.UIUtil;
import org.fife.rsta.ui.search.FindToolBar;
import org.fife.rsta.ui.search.SearchComboBox;
import org.fife.rsta.ui.search.SearchEvent;
import org.fife.rsta.ui.search.SearchListener;
import org.fife.rtext.Savu;
import org.fife.rtext.SavuButtonUI;
import org.fife.rtext.SearchManager;

import com.jtattoo.plaf.hifi.HiFiButtonUI;
import com.jtattoo.plaf.hifi.HiFiComboBoxUI;
import com.jtattoo.plaf.hifi.HiFiPanelUI;

/**
 * An override of the rstaui find toolbar that always marks its contents instantly
 * It does still ober the context's rules about whether or not to mark
 * It does not contain the button to toggle marking
 * @author maegereg
 *
 */
public class MarkingFindToolBar extends FindToolBar {
	
	private SearchManager parent;
	private static Icon closeIcon;
		
	private Container fieldPanel;
	private Container buttonPanel;
	
	public MarkingFindToolBar(SearchListener listener, SearchManager parent)
	{
		super(listener);
		setMarkAllDelay(0);
		
		this.parent = parent;
		URL url = getClass().getResource("grayclose.png");
		try {
			closeIcon = new ImageIcon(ImageIO.read(url));
		} catch (IOException e) {
			// TODO Auto-generated catch block
		}
	}
	
	/**
	 * Sets the background color and foreground color of this component and all subcomponents
	 * @param background
	 * @param foreground
	 */
	public void applyBackgroundColor(Color background, Color foreground)
	{
		
		setBackground(background);
		setForeground(foreground);
				
		recColorize(fieldPanel, background, foreground);
		recColorize(buttonPanel, background, foreground);
	}
	
	/**
	 * Recursively color the child containers of a container
	 * @param c the container to color (and whose children to color)
	 * @param background The background color to use
	 * @param foreground The foreground color to use
	 */
	protected void recColorize(Container c, Color background, Color foreground) {
		c.setBackground(background);
		c.setForeground(foreground);
		for (int i = 0; i < c.getComponentCount(); i++) {
			if (c.getComponent(i) instanceof JPanel || c.getComponent(i) instanceof JCheckBox) {
				recColorize((Container) c.getComponent(i), background, foreground);
			} 			
			
			if (c.getComponent(i) instanceof JButton) {
				JButton b = (JButton) c.getComponent(i);
				if (b.getIcon() == null) {
					b.setUI(new SavuButtonUI());
					b.setBackground(Savu.OUTPUT_AREA_BACKGROUND_COLOR);
				}
			} 
		}
	}
	
	/**
	 * Changes the colors of the find/replace text areas
	 * @param background the background color to use
	 * @param foreground the foreground color to use
	 */
	public void colorTextFields(Color background, Color foreground)
	{
		findCombo.getEditor().getEditorComponent().setBackground(background);
		findCombo.getEditor().getEditorComponent().setForeground(foreground);
		if (replaceCombo != null)
		{
			replaceCombo.getEditor().getEditorComponent().setBackground(background);
			replaceCombo.getEditor().getEditorComponent().setForeground(foreground);
		}
	}
	
	/**
	 * Override to ensure that we can color the panel
	 */
	protected Container createFieldPanel()
	{
		findFieldListener = new MarkingFindFieldListener();
		fieldPanel = new JPanel(new BorderLayout());

		findCombo = new SearchComboBox(this, false);
		findCombo.setUI(new HiFiComboBoxUI());
		JTextComponent findField = UIUtil.getTextComponent(findCombo);
		findFieldListener.install(findField);
		fieldPanel.add(createContentAssistablePanel(findCombo));

		return fieldPanel;
	}
	
	/**
	 * Override to remove the markall button
	 */
	protected Container createButtonPanel()
	{
		buttonPanel = super.createButtonPanel();
		buttonPanel.remove(markAllCheckBox);
		URL url = getClass().getResource("grayclose.png");
		try {
			closeIcon = new ImageIcon(ImageIO.read(url));
		} catch (IOException e) {
			// TODO Auto-generated catch block
		}
		JButton closeButton = new JButton(closeIcon);
		closeButton.setUI(new HiFiButtonUI());
		closeButton.setBackground(Savu.MAIN_BACKGROUND_COLOR);
		ToolBarCloseListener listener = new ToolBarCloseListener();
		closeButton.addActionListener(listener);
		closeButton.addMouseListener(listener);
		buttonPanel.add(closeButton);
		buttonPanel.setBackground(Savu.MAIN_BACKGROUND_COLOR);
		return buttonPanel;
	}
	
	/**
	 * Forces an update of the markings - can be used to remove them if marking has been disabled
	 */
	public void updateMarkings()
	{
		SearchEvent se = new SearchEvent(this, SearchEvent.Type.MARK_ALL,
				getSearchContext());
				fireSearchEvent(se);
	}
	
	/**
	 * Listens for events in this tool bar.  Keeps the UI in sync with the
	 * search context and vice versa.
	 */
	private class ToolBarCloseListener extends MouseAdapter
			implements ActionListener {

		public void actionPerformed(ActionEvent e) {

			parent.hideFindReplaceUI();

		}

	}
	
	protected class MarkingFindFieldListener extends FindFieldListener
	{
		
	}
	
}

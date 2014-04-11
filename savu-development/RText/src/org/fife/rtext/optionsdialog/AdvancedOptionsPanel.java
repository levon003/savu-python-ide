package org.fife.rtext.optionsdialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.fife.rtext.AbstractMainView;
import org.fife.rtext.RTextUtilities;
import org.fife.rtext.Savu;
import org.fife.ui.OptionsDialog;
import org.fife.ui.OptionsDialogPanel;
import org.fife.ui.SelectableLabel;
import org.fife.ui.SpecialValueComboBox;
import org.fife.ui.UIUtil;
import org.fife.ui.OptionsDialogPanel.OptionPanelBorder;
import org.fife.ui.OptionsDialogPanel.OptionsPanelCheckResult;
import org.fife.ui.rtextfilechooser.RDirectoryChooser;
import org.fife.ui.rtextfilechooser.RTextFileChooser;
import org.fife.util.TranslucencyUtil;

public class AdvancedOptionsPanel extends OptionsDialogPanel {

	private Box topPanel;
	
	private static final String PROPERTY	= "property";

	/**
	 * Constructor.
	 *
	 * @param rtext The owning RText instance.
	 * @param msg The resource bundle to use.
	 */
	public AdvancedOptionsPanel(Savu rtext, ResourceBundle msg) {

		super(msg.getString("OptAdvName"));

		ComponentOrientation orientation = ComponentOrientation.
				getOrientation(getLocale());

		// Set up our border and layout.
		setBorder(UIUtil.getEmpty5Border());
		setLayout(new BorderLayout());

		// Create a panel for stuff aligned at the top.
		topPanel = Box.createVerticalBox();
		topPanel.add(new JLabel(msg.getString("OptAdvDesc")));

		add(topPanel, BorderLayout.NORTH);
		applyComponentOrientation(orientation);

	}


	/**
	 * {@inheritDoc}
	 */
	protected void doApplyImpl(Frame owner) {

		Savu rtext = (Savu)owner;
		AbstractMainView mainView = rtext.getMainView();

	}


	/**
	 * {@inheritDoc}
	 */
	public JComponent getTopJComponent() {
		return topPanel;
	}




	/**
	 * {@inheritDoc}
	 */
	protected void setValuesImpl(Frame owner) {

		Savu rtext = (Savu)owner;
		AbstractMainView mainView = rtext.getMainView();

	}


	/**
	 * Always return with valid inputs.
	 */
	@Override
	protected OptionsPanelCheckResult ensureValidInputsImpl() {
		return null;
	}

}

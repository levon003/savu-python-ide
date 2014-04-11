package org.fife.ui.rtextarea;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JScrollBar;

import org.fife.rtext.RTextEditorPane;
import org.fife.ui.SavuScrollbarUI;
import org.fife.ui.dockablewindows.DockableWindowGroup;

import com.jtattoo.plaf.AbstractLookAndFeel;
import com.jtattoo.plaf.hifi.HiFiDefaultTheme;
import com.jtattoo.plaf.hifi.HiFiScrollBarUI;

public class SavuScrollPane extends RTextScrollPane {

	public SavuScrollPane() {
		this(null, true);
	}


	public SavuScrollPane(RTextArea textArea) {
		this(textArea, true);
	}


	public SavuScrollPane(RTextArea textArea, boolean lineNumbers) {
		this(textArea, lineNumbers, Color.GRAY);
	}

	public SavuScrollPane(RTextArea area, boolean lineNumbers, Color lineNumberColor) {
		super(area, lineNumbers, lineNumberColor);
		AbstractLookAndFeel.setTheme(new HiFiDefaultTheme());
		getVerticalScrollBar().setUI(new SavuScrollbarUI());
		getHorizontalScrollBar().setUI(new SavuScrollbarUI());
		getVerticalScrollBar().setBackground(new Color(40, 40, 35));
		getHorizontalScrollBar().setBackground(new Color(40, 40, 35));
	}

}

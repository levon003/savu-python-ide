package org.fife.rtext;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JMenuBar;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.UIResource;

import com.jtattoo.plaf.AbstractLookAndFeel;
import com.jtattoo.plaf.ColorHelper;
import com.jtattoo.plaf.JTattooUtilities;
import com.jtattoo.plaf.hifi.HiFiButtonUI;

public class SavuButtonUI extends HiFiButtonUI {

	public SavuButtonUI() {
		super();
		// TODO Auto-generated constructor stub
	}

	@Override
    protected void paintBackground(Graphics g, AbstractButton b) {
        if (!b.isContentAreaFilled() || (b.getParent() instanceof JMenuBar)) {
            return;
        }

        int width = b.getWidth();
        int height = b.getHeight();
        Graphics2D g2D = (Graphics2D) g;
        Shape savedClip = g.getClip();
        if ((b.getBorder() != null) && b.isBorderPainted() && (b.getBorder() instanceof UIResource)) {
            Area clipArea = new Area(new Rectangle2D.Double(1, 1, width - 2, height - 2));
            clipArea.intersect(new Area(savedClip));
            g2D.setClip(clipArea);
        }
        if (!b.isEnabled()) {
            Color bc = Color.RED;
            g.setColor(bc);
        }
        basepaintBackground(g, b);
        g2D.setClip(savedClip);
    }
	
    protected void basepaintBackground(Graphics g, AbstractButton b) {
        if (!b.isContentAreaFilled() || (b.getParent() instanceof JMenuBar)) {
            return;
        }

        int width = b.getWidth();
        int height = b.getHeight();
        
        ButtonModel model = b.getModel();
        Color colors[] = AbstractLookAndFeel.getTheme().getButtonColors();
        if (b.isEnabled()) {
            Color background = b.getBackground();
            if (background instanceof ColorUIResource) {
                if (model.isPressed() && model.isArmed()) {
                    colors = AbstractLookAndFeel.getTheme().getPressedColors();
                } else if (b.isRolloverEnabled() && model.isRollover()) {
                    colors = AbstractLookAndFeel.getTheme().getRolloverColors();
                } else if (AbstractLookAndFeel.getTheme().doShowFocusFrame() && b.hasFocus()) {
                    colors = AbstractLookAndFeel.getTheme().getFocusColors();
                } else if (JTattooUtilities.isFrameActive(b) 
                        && (b.getRootPane() != null) 
                        && (b.equals(b.getRootPane().getDefaultButton()))) {
                    colors = defaultColors;
                }
            } else {
                if (model.isPressed() && model.isArmed()) {
                    colors = ColorHelper.createColorArr(ColorHelper.darker(background, 30), ColorHelper.darker(background, 10), 20);
                } else {
                    if (b.isRolloverEnabled() && model.isRollover()) {
                        colors = ColorHelper.createColorArr(ColorHelper.brighter(background, 50), ColorHelper.brighter(background, 10), 20);
                    } else {
                        colors = ColorHelper.createColorArr(ColorHelper.brighter(background, 30), ColorHelper.darker(background, 10), 20);
                    }
                }
            }
        } else { // disabled
        	int i = 0;
            while (i < colors.length) {
            	colors[i] = new Color(40 + i, 40 + i, 30 + i);
            	i++;
            }
            
        }
        
        if (b.isBorderPainted() && (b.getBorder() != null)) {
            Insets insets = b.getBorder().getBorderInsets(b);
            int x = insets.left > 0 ? 1 : 0;
            int y = insets.top > 0 ? 1 : 0;
            int w = insets.right > 0 ? width - 1 : width;
            int h = insets.bottom > 0 ? height - 1 : height;
            JTattooUtilities.fillHorGradient(g, colors, x, y, w - x, h - y);
        } else {
            JTattooUtilities.fillHorGradient(g, colors, 0, 0, width, height);
        }
    }
	
	@Override 
    protected void paintText(Graphics g, AbstractButton b, Rectangle textRect, String text) {
        ButtonModel model = b.getModel();
        FontMetrics fm = g.getFontMetrics();
        int mnemIndex;
        if (JTattooUtilities.getJavaVersion() >= 1.4) {
            mnemIndex = b.getDisplayedMnemonicIndex();
        } else {
            mnemIndex = JTattooUtilities.findDisplayedMnemonicIndex(b.getText(), model.getMnemonic());
        }
        int offs = 0;
        if (model.isArmed() && model.isPressed()) {
            offs = 1;
        }

        Graphics2D g2D = (Graphics2D) g;
        Composite composite = g2D.getComposite();
        AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f);
        g2D.setComposite(alpha);
        Color fc = b.getForeground();
        if (fc instanceof ColorUIResource) {
            if (model.isPressed() && model.isArmed()) {
                fc = AbstractLookAndFeel.getTheme().getSelectionForegroundColor();
            }
        }
        if (!model.isEnabled()) {
            fc = AbstractLookAndFeel.getTheme().getDisabledForegroundColor();
        }
        if (ColorHelper.getGrayValue(fc) > 64) {
            g2D.setColor(Color.black);
        } else {
            g2D.setColor(Color.white);
        }
        JTattooUtilities.drawStringUnderlineCharAt(b, g, text, mnemIndex, textRect.x + offs + 1, textRect.y + offs + fm.getAscent() + 1);
        g2D.setComposite(composite);
        g2D.setColor(fc);
        JTattooUtilities.drawStringUnderlineCharAt(b, g, text, mnemIndex, textRect.x + offs, textRect.y + offs + fm.getAscent());
    }
}

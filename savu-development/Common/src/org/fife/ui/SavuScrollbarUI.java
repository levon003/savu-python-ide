package org.fife.ui;

import com.jtattoo.plaf.*;
import com.jtattoo.plaf.hifi.HiFiDefaultTheme;
import com.jtattoo.plaf.hifi.HiFiScrollBarUI;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.plaf.ScrollBarUI;
import javax.swing.plaf.metal.MetalScrollBarUI;

public class SavuScrollbarUI extends HiFiScrollBarUI {
		    private Image imageThumb, imageTrack;
		    public SavuScrollbarUI() {
		        try {
		            imageThumb = ImageIO.read(getClass().getResource("thumb.png"));
		            imageTrack = ImageIO.read(getClass().getResource("track.png"));
		            
		        } catch (IOException e){}

		    }

		    @Override
		    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {        
		        g.translate(thumbBounds.x, thumbBounds.y);
		        g.setColor(new Color(40, 40, 35));
		        g.drawRect( 0, 0, thumbBounds.width - 2, thumbBounds.height +5 );
		        AffineTransform transform = AffineTransform.getScaleInstance((double)thumbBounds.width/imageThumb.getWidth(null),(double)thumbBounds.height/imageThumb.getHeight(null));
		        ((Graphics2D)g).drawImage(imageThumb, transform, null);
		        g.translate( -thumbBounds.x, -thumbBounds.y );
		    }

		    @Override
		    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {        
		        g.translate(trackBounds.x, trackBounds.y);
		        ((Graphics2D)g).drawImage(imageTrack,AffineTransform.getScaleInstance(1,(double)trackBounds.height/imageTrack.getHeight(null)),null);
		        g.translate( -trackBounds.x, -trackBounds.y );
		    } 

		  
		}


/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2009 Dimitry
 *
 *  This file author is Dimitry
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.core.ui.components;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

import org.freeplane.core.util.LogUtils;

import com.thebuzzmedia.imgscalr.AsyncScalr;

/**
 * @author Dimitry Polivaev
 * 22.08.2009
 */
public class BitmapViewerComponent extends JComponent {
	/**
	 * 
	 */
	static{
//		System.setProperty("imgscalr.debug", "true");
		AsyncScalr.setServiceThreadCount(1);
	}
	private static final long serialVersionUID = 1L;
	private int hint;
	private BufferedImage cachedImage;
	private final URL url;
	private final Dimension originalSize;
	private int imageX;
	private int imageY;
	private boolean processing;

	protected int getHint() {
		return hint;
	}

	public void setHint(final int hint) {
		this.hint = hint;
	}

	public BitmapViewerComponent(final URI uri) throws MalformedURLException, IOException {
		url = uri.toURL();
		cachedImage = ImageIO.read(url);
		originalSize = new Dimension(cachedImage.getWidth(), cachedImage.getHeight());
		hint = Image.SCALE_SMOOTH;
		processing = false;
	}

	public Dimension getOriginalSize() {
		return new Dimension(originalSize);
	}

	@Override
	protected void paintComponent(final Graphics g) {
		if(processing)
			return;
		if (getWidth() == 0 || getHeight() == 0) {
			return;
		}
		if(! isCachedImageValid()){
			BufferedImage tempImage;
	        try {
	        	tempImage = ImageIO.read(url);
	        }
	        catch (IOException e) {
				return;
	        }
	        final BufferedImage image = tempImage;
			final int imageWidth = image.getWidth();
			final int imageHeight = image.getHeight();
			if(imageWidth == 0 || imageHeight == 0){
				return;
			}
			processing = true;
			final Future<BufferedImage> result = AsyncScalr.resize(image, getWidth(),getHeight());
			AsyncScalr.getService().submit(new Runnable() {
				public void run() {
					EventQueue.invokeLater(new Runnable() {
						
						public void run() {
							processing = false;
							BufferedImage scaledImage = null;
							try {
								scaledImage = result.get();
							} catch (Exception e) {
								LogUtils.severe(e);
								return;
							}
							finally{
								image.flush();
							}
							final int scaledImageHeight = scaledImage.getHeight();
							final int scaledImageWidth = scaledImage.getWidth();
							if (scaledImageHeight > getHeight()) {
								imageX = 0;
								imageY = (getHeight() - scaledImageHeight) / 2;
							}
							else {
								imageX = (getWidth() - scaledImageWidth) / 2;
								imageY = 0;
							}
							cachedImage = scaledImage;
							repaint();
						}
					});
				}
			});
		}
		else{
			g.drawImage(cachedImage, imageX, imageY, null);
			cachedImage.flush();
		}
	}

	private boolean isCachedImageValid() {
		return cachedImage != null && 
				(1 >= Math.abs(getWidth() -  cachedImage.getWidth()) && getHeight() >= cachedImage.getHeight()
				||
				getWidth() >=  cachedImage.getWidth() && 1 >= Math.abs(getHeight() - cachedImage.getHeight()));
	}
}

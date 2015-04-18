/*
 * Copyright (c) JForum Team
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, 
 * with or without modification, are permitted provided 
 * that the following conditions are met:
 * 
 * 1) Redistributions of source code must retain the above 
 * copyright notice, this list of conditions and the 
 * following disclaimer.
 * 2) Redistributions in binary form must reproduce the 
 * above copyright notice, this list of conditions and 
 * the following disclaimer in the documentation and/or 
 * other materials provided with the distribution.
 * 3) Neither the name of "Rafael Steil" nor 
 * the names of its contributors may be used to endorse 
 * or promote products derived from this software without 
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT 
 * HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, 
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL 
 * THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE 
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER 
 * IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 * 
 * Created on Nov 13, 2004 / 17:17:09
 * The JForum Project
 * http://www.jforum.net
 */

package net.jforum.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import net.jforum.JForumExecutionContext;
import net.jforum.SessionFacade;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.log4j.Logger;

import com.octo.captcha.component.image.backgroundgenerator.BackgroundGenerator;
import com.octo.captcha.component.image.backgroundgenerator.FunkyBackgroundGenerator;
import com.octo.captcha.component.image.backgroundgenerator.GradientBackgroundGenerator;
import com.octo.captcha.component.image.color.RandomListColorGenerator;
import com.octo.captcha.component.image.fontgenerator.FontGenerator;
import com.octo.captcha.component.image.fontgenerator.TwistedAndShearedRandomFontGenerator;
import com.octo.captcha.component.image.textpaster.RandomTextPaster;
import com.octo.captcha.component.image.textpaster.TextPaster;
import com.octo.captcha.component.image.wordtoimage.ComposedWordToImage;
import com.octo.captcha.component.image.wordtoimage.WordToImage;
import com.octo.captcha.component.word.wordgenerator.RandomWordGenerator;
import com.octo.captcha.component.word.wordgenerator.WordGenerator;
import com.octo.captcha.engine.image.ListImageCaptchaEngine;
import com.octo.captcha.image.ImageCaptchaFactory;
import com.octo.captcha.image.gimpy.GimpyFactory;

/**
 * @author James Yong
 * @version $Id$
 */
public class Captcha extends ListImageCaptchaEngine
{
	private static final Logger LOGGER = Logger.getLogger(Captcha.class);
	
	private static Captcha classInstance = new Captcha();

	private static String charsInUse;
	
	private void initializeChars()
	{
		if (SystemGlobals.getBoolValue(ConfigKeys.CAPTCHA_IGNORE_CASE)) {
			charsInUse = "123456789abcdefghijklmnopqrstuvwxyz@#%^";
		}
		else {
			charsInUse = "123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz@#%^";
		}
	}
	
	/**
	 * Gets the singleton
	 * 
	 * @return Instance of Captcha class
	 */
	public static Captcha getInstance()
	{
		return classInstance;
	}

	protected void buildInitialFactories()
	{
		this.initializeChars();
		
		final Integer width = SystemGlobals.getIntValue(ConfigKeys.CAPTCHA_WIDTH);
		final Integer height = SystemGlobals.getIntValue(ConfigKeys.CAPTCHA_HEIGHT);
		final Integer minWords = SystemGlobals.getIntValue(ConfigKeys.CAPTCHA_MIN_WORDS);
		final Integer maxWords = SystemGlobals.getIntValue(ConfigKeys.CAPTCHA_MAX_WORDS);
		final Integer minFontSize = SystemGlobals.getIntValue(ConfigKeys.CAPTCHA_MIN_FONT_SIZE);
		final Integer maxFontSize = SystemGlobals.getIntValue(ConfigKeys.CAPTCHA_MAX_FONT_SIZE);

		Color[] colors = new Color[] { Color.PINK, Color.RED, Color.GREEN, Color.ORANGE, Color.MAGENTA };
		final RandomListColorGenerator colorGenerator = new RandomListColorGenerator(colors);	

		final List<BackgroundGenerator> backgroundGeneratorList = new ArrayList<BackgroundGenerator>();
		Color previousColor = colorGenerator.getNextColor();
		for (int i = 0; i < colors.length - 1; i++) {
			Color nextColor = colorGenerator.getNextColor();
			backgroundGeneratorList.add(new GradientBackgroundGenerator(width, 
					height, previousColor, nextColor));
			previousColor = nextColor;
		}
		backgroundGeneratorList.add(new FunkyBackgroundGenerator(width, height));		
		
		final List<TextPaster> textPasterList = new ArrayList<TextPaster>();
		textPasterList.add(new RandomTextPaster(minWords, maxWords, Color.DARK_GRAY));
		textPasterList.add(new RandomTextPaster(minWords, maxWords, Color.BLUE));
		textPasterList.add(new RandomTextPaster(minWords, maxWords, Color.BLACK));

		final List<FontGenerator> fontGeneratorList = new ArrayList<FontGenerator>();
		fontGeneratorList.add(new TwistedAndShearedRandomFontGenerator(minFontSize, maxFontSize));

		// Create a random word generator
		final WordGenerator words = new RandomWordGenerator(charsInUse);

		for (final FontGenerator fontGeny : fontGeneratorList) {
			LOGGER.debug("use font: " + fontGeny.getFont().getFontName());
			for (final BackgroundGenerator bkgdGeny : backgroundGeneratorList) {
				for (final TextPaster textPaster : textPasterList) {
					final WordToImage word2image = new ComposedWordToImage(fontGeny, bkgdGeny, textPaster);
					
					// Create an ImageCaptcha Factory
					final ImageCaptchaFactory factory = new GimpyFactory(words, word2image);
					
					// Add a factory to the gimpy list (A Gimpy is an ImageCaptcha)
					addFactory(factory);
				}
			}
		}
	}

	public void writeCaptchaImage()
	{
		final BufferedImage image = SessionFacade.getUserSession().getCaptchaImage();
		
		if (image == null) {
			return;
		}

		OutputStream outputStream = null;
		
		try {
			outputStream = JForumExecutionContext.getResponse().getOutputStream();
			ImageIO.write(image, "jpg", outputStream);			
		}
		catch (IOException ex) {
			LOGGER.error(ex);
		}
		finally {
			if (outputStream != null) {
				try {
					outputStream.close();
				}
				catch (IOException ex) {
					LOGGER.error(ex);
				}
			}
			image.flush();			
		}
	}
}

package com.sourcecode.malls.service.impl;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sourcecode.malls.service.base.BaseImageService;

@Service
public class ImageService extends BaseImageService {

	@Value("${qrcode.logo.path}")
	private String qrCodeLogoPath;

	@Value("${font.path}")
	private String fontPath;

	private Font font;

	@PostConstruct
	public void init() throws Exception {
		font = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(fileService.load(true, fontPath)));
	}

	public Font getFont() {
		return this.font;
	}

	public BufferedImage generateQRCodeImage(String text, int width, int height, int margin) throws Exception {
		return super.generateQRCodeImage(text, width, height, margin, qrCodeLogoPath);
	}
}

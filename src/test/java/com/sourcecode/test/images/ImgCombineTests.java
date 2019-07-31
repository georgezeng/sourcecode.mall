package com.sourcecode.test.images;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.Test;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class ImgCombineTests {
	@Test
	public void test() throws Exception {
		InputStream is = generateQRCodeImage("https://www.baidu.com", 350, 350);
		generateBigImg(is);
//		
//		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
//        Font[] fonts = ge.getAllFonts();
//
//        for (Font font : fonts) {
//            System.out.print(font.getFontName() + " : ");
//            System.out.println(font.getFamily());
//        }
	}

	private InputStream generateQRCodeImage(String text, int width, int height) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Map<EncodeHintType, Object> hintMap = new HashMap<EncodeHintType, Object>();
		hintMap.put(EncodeHintType.MARGIN, new Integer(1));
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hintMap);
		MatrixToImageWriter.writeToStream(bitMatrix, "PNG", out);
		return new ByteArrayInputStream(out.toByteArray());
	}

	private void generateBigImg(InputStream is) throws Exception {
		BufferedImage qrCode = ImageIO.read(is);
		BufferedImage avatar = ImageIO.read(new File("./src/test/resources/avatar.png"));
		int avatarSize = 160;
		BufferedImage result = ImageIO.read(getClass().getResourceAsStream("/share-info-bg.png"));
		Graphics2D g = (Graphics2D)result.getGraphics();
		g.drawImage(qrCode, 320, 1000, null);
	    g.setColor(Color.DARK_GRAY);
	    g.setFont(new Font("STSong", Font.BOLD, 40));
	    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	    String name1 = "小兆";
	    g.drawString(name1, (result.getWidth() - 42 * name1.length()) / 2, 275);
	    g.setFont(new Font("STSong", Font.BOLD, 50));
	    g.setColor(Color.RED);
	    String name2 = "邀请您注册多呗家居商城";
	    g.drawString(name2, (result.getWidth() - 50 * name2.length()) / 2, 350);
	    g.setClip(new Ellipse2D.Float(410, 60, avatarSize, avatarSize));
	    g.drawImage(avatar, 410, 60, avatarSize, avatarSize, null);
		ImageIO.write(result, "png", new File("./src/test/resources/result.png"));
	}
}
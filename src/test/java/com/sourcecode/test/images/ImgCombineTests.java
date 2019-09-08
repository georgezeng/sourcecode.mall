package com.sourcecode.test.images;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.TextAttribute;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.text.AttributedString;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.Test;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.sourcecode.malls.util.ImageUtil;

public class ImgCombineTests {
	@Test
	public void test() throws Exception {
		generateInvitePoster("https://www.baidu.com");
//		generateGoodsPoster("https://www.baidu.com");

	}

	private BufferedImage generateQRCodeImage(String text, int width, int height, int margin) throws Exception {
		Map<EncodeHintType, Object> hintMap = new HashMap<EncodeHintType, Object>();
		hintMap.put(EncodeHintType.MARGIN, margin);
		QRCodeWriter qrCodeWriter = new QRCodeWriter();
		BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hintMap);
		BufferedImage qrCode = MatrixToImageWriter.toBufferedImage(bitMatrix);
		BufferedImage logo = ImageIO.read(getClass().getResourceAsStream("/logo.png"));
		// Calculate the delta height and width
		int deltaHeight = height - logo.getHeight();
		int deltaWidth = width - logo.getWidth();
		BufferedImage combined = new BufferedImage(height, width, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) combined.getGraphics();
		g.drawImage(qrCode, 0, 0, null);
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
		g.drawImage(logo, (int) Math.round(deltaWidth / 2), (int) Math.round(deltaHeight / 2), null);
		return combined;
	}

	private void generateInvitePoster(String url) throws Exception {
		BufferedImage qrCode = ImageUtil.resizeImage(generateQRCodeImage(url, 1000, 1000, 1), 250, 250);
		BufferedImage avatar = ImageIO.read(new File("./src/test/resources/avatar.png"));
		int avatarSize = 160;
		BufferedImage result = ImageIO.read(getClass().getResourceAsStream("/invite-share-bg.png"));
		Graphics2D g = (Graphics2D) result.getGraphics();
		g.drawImage(qrCode, 250, 720, null);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Font font = Font.createFont(Font.TRUETYPE_FONT, new File("./src/test/resources/msjh.ttf"));
		g.setColor(Color.DARK_GRAY);
//	    g.setFont(new Font(font.getName(), Font.BOLD, 40));
		String name1 = "测试一下";
//	    g.drawString(name1, (result.getWidth() - 42 * name1.length()) / 2, 275);
		ImageUtil.drawCenteredString(g, name1, 0, 400, result.getWidth(), 45,
				font.deriveFont(30f).deriveFont(Font.BOLD));
//	    g.setFont(new Font(font.getName(), Font.BOLD, 50));
//	    g.setColor(Color.RED);
//	    String name2 = "邀请您注册多呗家居商城";
////	    g.drawString(name2, (result.getWidth() - 50 * name2.length()) / 2, 350);
//	    drawCenteredString(g, name2, 0, 320, result.getWidth(), 50, font.deriveFont(50f).deriveFont(Font.BOLD));
		avatar = ImageUtil.resizeImage(avatar, avatarSize, avatarSize);
		int y = 220;
		int x = 300;
		g.setClip(new Ellipse2D.Float(x, y, avatarSize, avatarSize));
		g.drawImage(ImageUtil.rotateImage(avatar, 90), x, y, avatarSize, avatarSize, null);
		ImageIO.write(result, "png", new File("./src/test/resources/result.png"));

	}

	private void generateGoodsPoster(String url) throws Exception {
		Font font = Font.createFont(Font.TRUETYPE_FONT, new File("./src/test/resources/msyhc.ttf"));
		BufferedImage result = ImageIO.read(getClass().getResourceAsStream("/goods-poster-bg.png"));
		BufferedImage logo = ImageIO.read(getClass().getResourceAsStream("/goods-poster-logo.png"));
		BufferedImage qrCode = ImageUtil.resizeImage(generateQRCodeImage(url, 1000, 1000, 0), 250, 250);
		BufferedImage grayBg = ImageIO.read(getClass().getResourceAsStream("/goods-poster-bg-2.png"));
		Graphics2D g1 = (Graphics2D) grayBg.getGraphics();
		g1.drawImage(qrCode, 10, 10, null);
		g1.setColor(Color.decode("#333333"));
		g1.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		ImageUtil.drawCenteredString(g1, "长按识别二维码下单", 10, 275, qrCode.getWidth(), 30,
				font.deriveFont(25f).deriveFont(Font.BOLD));
		BufferedImage goodsItem = ImageUtil.resizeImage(ImageIO.read(getClass().getResourceAsStream("/goods-item.png")),
				result.getWidth(), result.getWidth());
		Graphics2D g2 = (Graphics2D) result.getGraphics();
		g2.drawImage(goodsItem, 0, 0, null);
		g2.drawImage(logo, 0, 30, null);
		g2.drawImage(grayBg, result.getWidth() * 3 / 4 - grayBg.getWidth() / 2 + 30, result.getWidth() + 50, null);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(Color.decode("#333333"));
		Font font2 = font.deriveFont(40f).deriveFont(Font.BOLD);
		g2.setFont(font2);
		String itemName = "乐秀空气炸锅乐秀空气炸锅乐秀空气炸锅乐秀空气炸锅";
		int size = itemName.length() / 10;
		int startY = result.getWidth() + 90;
		boolean outOfBound = false;
		int line = 0;
		int lineHeight = 60;
		int lineNums = 10;
		for (; line < size; line++) {
			if (line < 2) {
				g2.drawString(itemName.substring(lineNums * line, lineNums * (line + 1)), 20,
						startY + lineHeight * line);
			} else {
				outOfBound = true;
				break;
			}
		}
		if (outOfBound) {
			int end = line * lineNums + 5;
			if (itemName.length() < end) {
				end = itemName.length();
			}
			g2.drawString(itemName.substring(line * lineNums, end) + "...", 20, startY + lineHeight * line);
		} else {
			g2.drawString(itemName.substring(line * lineNums), 20, startY + lineHeight * line);
		}
		g2.setColor(Color.decode("#D06E6D"));
		int height = result.getHeight() - (result.getHeight() - (result.getWidth() + 50 + grayBg.getHeight()));
		g2.drawString("￥1499", 20, height);
		g2.setColor(Color.decode("#919BAD"));
		FontMetrics metrics = g2.getFontMetrics(font2);
		Font font3 = font.deriveFont(30f).deriveFont(Font.BOLD).deriveFont(Font.ITALIC);
		AttributedString as = new AttributedString("￥1899");
		as.addAttribute(TextAttribute.FONT, font3);
		as.addAttribute(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
		g2.drawString(as.getIterator(), metrics.stringWidth("￥1499") + 30, height);
		ImageIO.write(ImageUtil.setClip(result, 40), "png", new File("./src/test/resources/result2.png"));
	}

}

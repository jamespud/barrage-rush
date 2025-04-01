package com.spud.barrage.auth.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 验证码工具类
 * 
 * @author Spud
 * @date 2025/3/27
 */
@Slf4j
@Component
public class CaptchaUtil {

    // 字体列表
    private static final String[] FONT_TYPES = { "Arial", "Arial Black", "Courier New", "Times New Roman" };
    // 字符集合（不包含易混淆的字符0,o,1,i,l）
    private static final char[] CHARS = "23456789abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ".toCharArray();
    // 验证码宽度
    private static final int WIDTH = 130;
    // 验证码高度
    private static final int HEIGHT = 48;
    // 验证码长度
    private static final int LENGTH = 4;
    // 干扰线数量
    private static final int LINES = 6;

    private final Random random = new Random();

    /**
     * 生成验证码
     *
     * @return 包含验证码文本和图片Base64的Map
     */
    public Map<String, Object> generateCaptcha() {
        // 创建空白图片
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // 设置抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 填充背景色
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // 绘制干扰线
        drawInterferenceLines(g2d);

        // 绘制噪点
        drawNoise(image);

        // 绘制验证码
        String captchaText = drawCaptchaText(g2d);

        // 释放资源
        g2d.dispose();

        // 转换为Base64
        String base64Image = imageToBase64(image);

        // 返回验证码和图片
        Map<String, Object> result = new HashMap<>();
        result.put("code", captchaText);
        result.put("img", base64Image);

        return result;
    }

    /**
     * 绘制干扰线
     */
    private void drawInterferenceLines(Graphics2D g2d) {
        g2d.setStroke(new BasicStroke(1.5f));
        for (int i = 0; i < LINES; i++) {
            // 随机颜色
            g2d.setColor(randomColor(150, 250));
            // 随机起点和终点
            int x1 = random.nextInt(WIDTH);
            int y1 = random.nextInt(HEIGHT);
            int x2 = random.nextInt(WIDTH);
            int y2 = random.nextInt(HEIGHT);
            g2d.drawLine(x1, y1, x2, y2);
        }
    }

    /**
     * 绘制噪点
     */
    private void drawNoise(BufferedImage image) {
        int noiseCount = random.nextInt(WIDTH * HEIGHT / 50);
        for (int i = 0; i < noiseCount; i++) {
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            image.setRGB(x, y, randomColor(150, 250).getRGB());
        }
    }

    /**
     * 绘制验证码文本
     */
    private String drawCaptchaText(Graphics2D g2d) {
        StringBuilder captchaText = new StringBuilder();

        // 计算每个字符的宽度
        int charWidth = WIDTH / (LENGTH + 1);
        int charHeight = HEIGHT - 12;

        // 绘制验证码
        for (int i = 0; i < LENGTH; i++) {
            // 随机字符
            char c = CHARS[random.nextInt(CHARS.length)];
            captchaText.append(c);

            // 随机字体
            String fontType = FONT_TYPES[random.nextInt(FONT_TYPES.length)];
            Font font = new Font(fontType, Font.BOLD, 28 + random.nextInt(10));
            g2d.setFont(font);

            // 随机颜色
            g2d.setColor(randomColor(20, 130));

            // 随机旋转
            AffineTransform transform = new AffineTransform();
            transform.translate((i + 1) * charWidth, HEIGHT / 2.0 + random.nextInt(charHeight / 2) - charHeight / 4);
            transform.rotate(Math.toRadians(random.nextInt(30) - 15));
            g2d.setTransform(transform);

            // 绘制字符
            g2d.drawString(String.valueOf(c), 0, 0);
            g2d.setTransform(new AffineTransform());
        }

        return captchaText.toString();
    }

    /**
     * 生成随机颜色
     */
    private Color randomColor(int min, int max) {
        int r = min + random.nextInt(max - min);
        int g = min + random.nextInt(max - min);
        int b = min + random.nextInt(max - min);
        return new Color(r, g, b);
    }

    /**
     * 将图片转换为Base64编码
     */
    private String imageToBase64(BufferedImage image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
            byte[] bytes = baos.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            log.error("验证码图片转Base64失败", e);
            return null;
        } finally {
            try {
                baos.close();
            } catch (IOException e) {
                log.error("关闭ByteArrayOutputStream失败", e);
            }
        }
    }
}
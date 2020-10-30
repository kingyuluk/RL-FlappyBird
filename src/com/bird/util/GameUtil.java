package com.bird.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;

import javax.imageio.ImageIO;

/**
 * 工具类，游戏中用到的工具都在此类
 *
 * @author Kingyu
 */
public class GameUtil {

    private GameUtil() {
    } // 私有化，防止其他类实例化此类

    /**
     * 装载图片的方法
     *
     * @param imgPath 图片路径
     * @return 图片资源
     */
    public static BufferedImage loadBufferedImage(String imgPath) {
        try {
            return ImageIO.read(new FileInputStream(imgPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 判断任意概率的概率性事件是否发生
     *
     * @param numerator   分子，不小于0的值
     * @param denominator 分母，不小于0的值
     * @return 概率性事件发生返回true，否则返回false
     */
    public static boolean isInProbability(int numerator, int denominator) throws Exception {
        // 分子分母不小于0
        if (numerator <= 0 || denominator <= 0) {
            throw new Exception("传入了非法的参数");
        }
        //分子大于分母，一定发生
        if (numerator >= denominator) {
            return true;
        }

        return getRandomNumber(1, denominator + 1) <= numerator;
    }

    /**
     * 返回指定区间的一个随机数
     *
     * @param min 区间最小值，包含
     * @param max 区间最大值，不包含
     * @return 该区间的随机数
     */
    public static int getRandomNumber(int min, int max) {
        return (int) (Math.random() * (max - min) + min);
    }

    /**
     * 获得指定字符串在指定字体的宽高
     */
    public static int getStringWidth(Font font, String str) {
        AffineTransform affinetransform = new AffineTransform();
        FontRenderContext frc = new FontRenderContext(affinetransform, true, true);
        return (int) (font.getStringBounds(str, frc).getWidth());
    }

    public static int getStringHeight(Font font, String str) {
        AffineTransform affinetransform = new AffineTransform();
        FontRenderContext frc = new FontRenderContext(affinetransform, true, true);
        return (int) (font.getStringBounds(str, frc).getHeight());
    }


    /**
     * @param image:图片资源
     * @param x：x坐标
     * @param y：y坐标
     * @param g：画笔
     */
    public static void drawImage(BufferedImage image, int x, int y, Graphics g) {
        g.drawImage(image, x, y, null);
    }

    public static Rectangle RectClip(Rectangle rect1, Rectangle rect2) {
        Rectangle rect = null;
        if (rect1 == null || rect2 == null) {
            return rect;
        }
        double p1_x = rect1.getX(), p1_y = rect1.getY();
        double p2_x = p1_x + rect1.getHeight(), p2_y = p1_y + rect1.getWidth();
        double p3_x = rect2.getX(), p3_y = rect2.getY();
        double p4_x = p3_x + rect2.getHeight(), p4_y = p3_y + rect2.getWidth();

        if (p1_x > p4_x || p2_x < p3_x || p1_y > p4_y || p2_y < p3_y) {
            return rect;
        }

        double Len = Math.min(p2_x, p4_x) - Math.max(p1_x, p3_x);
        double Wid = Math.min(p2_y, p4_y) - Math.max(p1_y, p3_y);

        rect = new Rectangle((int) Math.min(p2_x, p4_x), (int) Math.min(p2_y, p4_y), (int) Len, (int) Wid);

        return rect;
    }

    private static int colorToRGB(int alpha, int red, int green, int blue) {

        int newPixel = 0;
        newPixel += alpha;
        newPixel = newPixel << 8;
        newPixel += red;
        newPixel = newPixel << 8;
        newPixel += green;
        newPixel = newPixel << 8;
        newPixel += blue;
        return newPixel;
    }


    // 更改图片尺寸的方法
    public static BufferedImage resize(BufferedImage img, int newWidth, int newHeight) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage new_img = new BufferedImage(newWidth, newHeight, img.getType());
        Graphics2D g = new_img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, newWidth, newHeight, 0, 0, w, h, null);
        g.dispose();
        return new_img;
    }


    // 图片预处理：将图像转换为80x80的灰度图
    public static BufferedImage imgPreprocess(BufferedImage observation){

        BufferedImage grayImage =
                new BufferedImage(observation.getWidth(), observation.getHeight(), observation.getType());

        for (int i = 0; i < observation.getWidth(); i++) {
            for (int j = 0; j < observation.getHeight(); j++) {
                final int color = observation.getRGB(i, j);
                final int r = (color >> 16) & 0xff;
                final int g = (color >> 8) & 0xff;
                final int b = color & 0xff;
                int gray = (int) (0.3 * r + 0.59 * g + 0.11 * b);
                int newPixel = colorToRGB(255, gray, gray, gray);
                grayImage.setRGB(i, j, newPixel);
            }
        }
        return resize(grayImage, 80, 80);
    }
}

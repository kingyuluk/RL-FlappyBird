package com.kingyu.rlbird.util;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.util.NDImageUtils;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * 工具类，游戏中用到的工具都在此类
 *
 * @author Kingyu
 */
public class GameUtil {

    private GameUtil() {
    }

    /**
     * 装载图片
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
     * Image preprocess
     *
     * @param observation input BufferedImage
     * @return NDArray:Shape(80,80,1)
     */
    public static NDArray imgPreprocess(BufferedImage observation) {
        return NDImageUtils.toTensor(
                NDImageUtils.resize(
                        ImageFactory.getInstance().fromImage(observation).toNDArray(NDManager.newBaseManager(), Image.Flag.GRAYSCALE)
                        ,80,80));
    }
}
package com.kingyu.rlbird.game.component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.kingyu.rlbird.util.Constant;
import com.kingyu.rlbird.util.GameUtil;

/**
 * 水管类
 *
 * @author Kingyu
 */
public class Pipe {
    static BufferedImage[] images; 

    static {
        final int PIPE_IMAGE_COUNT = 3;
        images = new BufferedImage[PIPE_IMAGE_COUNT];
        for (int i = 0; i < PIPE_IMAGE_COUNT; i++) {
            images[i] = GameUtil.loadBufferedImage(Constant.PIPE_IMG_PATH[i]);
        }
    }

    // 水管图片的宽高
    public static final int PIPE_WIDTH = images[0].getWidth();
    public static final int PIPE_HEIGHT = images[0].getHeight();
    public static final int PIPE_HEAD_WIDTH = images[1].getWidth();
    public static final int PIPE_HEAD_HEIGHT = images[1].getHeight();

    private int x, y; // 水管相对于元素层的坐标
    private final int width; // 水管的宽度
    private int height; // 水管的高度

    boolean visible; // 水管可见状态，true为可见，false表示可归还至对象池
    // 水管的类型
    int type;
    public static final int TYPE_TOP_NORMAL = 0;
    public static final int TYPE_BOTTOM_NORMAL = 1;
    private final int velocity;
    Rectangle pipeCollisionRect;

    public Pipe() {
        this.velocity = Constant.GAME_SPEED;
        this.width = PIPE_WIDTH;
        pipeCollisionRect = new Rectangle();
        pipeCollisionRect.width = PIPE_WIDTH;
    }

    /**
     * 设置水管参数
     *
     * @param x: x坐标
     * @param y：y坐标
     * @param height：水管高度
     * @param type：水管类型
     * @param visible：水管可见性
     */
    public void setAttribute(int x, int y, int height, int type, boolean visible) {
        this.x = x;
        this.y = y;
        this.height = height;
        this.type = type;
        this.visible = visible;
        setRectangle(this.x + 5, this.y, this.height); // 碰撞矩形位置补偿
    }

    /**
     * 设置碰撞矩形参数
     */
    public void setRectangle(int x, int y, int height) {
        pipeCollisionRect.x = x;
        pipeCollisionRect.y = y;
        pipeCollisionRect.height = height;
    }

    public void draw(Graphics g, Bird bird) {
        switch (this.type) {
            case TYPE_TOP_NORMAL:
                drawTopNormal(g);
                break;
            case TYPE_BOTTOM_NORMAL:
                drawBottomNormal(g);
                break;
        }
        if (bird.isDead()) {
            return;
        }
        movement();
//      //绘制碰撞矩形
//        g.setColor(Color.white);
//        g.drawRect((int) pipeRect.getX(), (int) pipeRect.getY(), (int) pipeRect.getWidth(), (int) pipeRect.getHeight());
    }

    // 上方管道加长
    public static final int TOP_PIPE_LENGTHENING = 100;

    // 绘制从上往下的普通水管
    private void drawTopNormal(Graphics g) {
        // 拼接的个数
        int count = (height - PIPE_HEAD_HEIGHT) / PIPE_HEIGHT + 1; // 取整+1
        // 绘制水管的主体
        for (int i = 0; i < count; i++) {
            g.drawImage(images[0], x, y + i * PIPE_HEIGHT, null);
        }
        // 绘制水管的顶部
        g.drawImage(images[1], x - ((PIPE_HEAD_WIDTH - width) >> 1),
                height - TOP_PIPE_LENGTHENING - PIPE_HEAD_HEIGHT, null); // 水管头部与水管主体的宽度不同，x坐标需要处理
    }

    // 绘制从下往上的普通水管
    private void drawBottomNormal(Graphics g) {
        // 拼接的个数
        int count = (height - PIPE_HEAD_HEIGHT - Ground.GROUND_HEIGHT) / PIPE_HEIGHT + 1;
        // 绘制水管的主体
        for (int i = 0; i < count; i++) {
            g.drawImage(images[0], x, Constant.FRAME_HEIGHT - PIPE_HEIGHT - Ground.GROUND_HEIGHT - i * PIPE_HEIGHT,
                    null);
        }
        // 绘制水管的顶部
        g.drawImage(images[2], x - ((PIPE_HEAD_WIDTH - width) >> 1), Constant.FRAME_HEIGHT - height, null);
    }

    private void movement() {
        x -= velocity;
        pipeCollisionRect.x -= velocity;
        if (x < -1 * PIPE_HEAD_WIDTH) {// 水管完全离开了窗口
            visible = false;
        }
    }

    /**
     * 判断当前水管是否完全出现在窗口中
     */
    public boolean isInFrame() {
        return x + width < Constant.FRAME_WIDTH;
    }

    public int getX() {
        return x;
    }

    public Rectangle getPipeCollisionRect() {
        return pipeCollisionRect;
    }

    public boolean isVisible() {
        return visible;
    }

    static class PipePool {
        private static final List<Pipe> pool = new ArrayList<>();

        // 容器内水管数量 = 窗口可容纳的水管数量+2， 由窗口宽度、水管宽度、水管间距算得
        public static final int FULL_PIPE = (Constant.FRAME_WIDTH
                / (Pipe.PIPE_HEAD_WIDTH + GameElementLayer.HORIZONTAL_INTERVAL) + 2) * 2;
        public static final int MAX_PIPE_COUNT = 30; // 对象池中对象的最大个数

        // 初始化水管容器
        static {
            for (int i = 0; i < FULL_PIPE; i++) {
                pool.add(new Pipe());
            }
        }

        /**
         * 从对象池中获取一个对象
         *
         * @return pipe from pipePool
         */
        public static Pipe get() {
            int size = pool.size();
            if (size > 0) {
                return pool.remove(size - 1); // 移除并返回最后一个
            } else {
                return new Pipe(); // 空对象池，返回一个新对象
            }
        }

        /**
         * 归还对象给容器
         */
        public static void giveBack(Pipe pipe) {
            if (pool.size() < MAX_PIPE_COUNT) {
                pool.add(pipe);
            }
        }
    }

}

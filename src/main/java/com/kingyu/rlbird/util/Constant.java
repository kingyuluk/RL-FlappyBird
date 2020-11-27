package com.kingyu.rlbird.util;

import java.awt.Color;
import java.awt.Font;

/**
 * 常量类
 *
 * @author Kingyu 后续优化可写入数据库或文件中，便于修改
 */

public class Constant {
    // 窗口尺寸
    public static final int FRAME_WIDTH = 288;
    public static final int FRAME_HEIGHT = 512;

    // 游戏标题
    public static final String GAME_TITLE = "RL Flappy Bird written by Kingyu";

    // 窗口位置
    public static final int FRAME_X = 0;
    public static final int FRAME_Y = 0;

    // 游戏速度（水管及背景层的移动速度）
    public static final int GAME_SPEED = 6;

    // 游戏背景色
    public static final Color BG_COLOR = new Color(0x000000);

    // 游戏刷新率
    public static final int FPS = 1000 / 30;

    // 标题栏高度
    public static final int WINDOW_BAR_HEIGHT = 30;

    // 小鸟动作
    public static final int[] DO_NOTHING = {1, 0};
    public static final int[] FLAP = {0, 1};

    // 图像资源路径
    public static final String BG_IMG_PATH = "src/main/resources/img/background.png";

    // 小鸟图片
    public static final String BIRDS_IMG_PATH = "src/main/resources/img/0.png";

    // 水管图片
    public static final String[] PIPE_IMG_PATH = {"src/main/resources/img/pipe.png", "src/main/resources/img/pipe_top.png",
            "src/main/resources/img/pipe_bottom.png"};

    public static final String TITLE_IMG_PATH = "src/main/resources/img/title.png";
    public static final String NOTICE_IMG_PATH = "src/main/resources/img/start.png";
    public static final String SCORE_FILE_PATH = "src/main/resources/score"; // 分数文件路径
}

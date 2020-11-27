package com.kingyu.rlbird.game;

import java.util.ArrayList;
import java.util.List;

import com.kingyu.rlbird.util.Constant;

/**
 * 为了避免反复地创建和销毁对象，使用对象池来提前创建好一些对象，使用时从对象池中获得，使用完后归还
 *
 * @author Kingyu
 */
public class PipePool {

    // 容器内水管数量 = 窗口可容纳的水管数量+2， 由窗口宽度、水管宽度、水管间距算得
    public static final int FULL_PIPE = (Constant.FRAME_WIDTH
            / (Pipe.PIPE_HEAD_WIDTH + GameElementLayer.HORIZONTAL_INTERVAL) + 2) * 2;

    private static final List<Pipe> pool = new ArrayList<>();
    public static final int MAX_PIPE_COUNT = 30; // 对象池中对象的最大个数，自行定义

    // 初始化水管容器
    static {
        for (int i = 0; i < FULL_PIPE; i++) {
            pool.add(new Pipe());
        }
    }

    /**
     * 从对象池中获取一个对象
     *
     * @return 传入对象的类型，以判断从哪个对象池中获取
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

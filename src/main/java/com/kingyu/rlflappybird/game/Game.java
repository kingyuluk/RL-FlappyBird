package com.kingyu.rlflappybird.game;

import com.kingyu.rlflappybird.ai.TrainBird;
import com.kingyu.rlflappybird.rl.ActionSpace;
import com.kingyu.rlflappybird.rl.LruReplayBuffer;
import com.kingyu.rlflappybird.rl.ReplayBuffer;
import com.kingyu.rlflappybird.rl.agent.RlAgent;
import com.kingyu.rlflappybird.rl.env.RlEnv;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import com.kingyu.rlflappybird.util.GameUtil;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.*;

import static com.kingyu.rlflappybird.util.Constant.*;

/**
 * 主窗口类，游戏窗口和绘制的相关内容
 *
 * @author Kingyu
 */

public class Game extends Frame implements RlEnv {
    private static final long serialVersionUID = 1L; // 保持版本的兼容性

//    private final NDManager subManager = NDManager.newBaseManager();

    private static int gameMode;
    public static final int NORMAL_MODE = 0;
    public static final int NOUI_MODE = 1;
    public static final int UI_MODE = 2;

    private static int gameState; // 游戏状态
    //    public static final int GAME_READY = 0; // 游戏未开始
    public static final int GAME_START = 1; // 游戏开始
    public static final int GAME_OVER = 2; // 游戏结束

    private GameBackground background; // 游戏背景
    private Bird bird; // 小鸟
    private GameElementLayer gameElement; // 游戏元素
    public static boolean birdFlapped = false;

    private final NDManager manager;
    private final ReplayBuffer replayBuffer;
    private State currentState;
    private State postState;

    /**
     * Constructs a {@link Game} with a basic {@link LruReplayBuffer}.
     *
     * @param manager          the manager for creating the game in
     * @param batchSize        the number of steps to train on per batch
     * @param replayBufferSize the number of steps to hold in the buffer
     */
    public Game(NDManager manager, int batchSize, int replayBufferSize, int gameMode) {
        this(manager, new LruReplayBuffer(batchSize, replayBufferSize));
        setGameMode(gameMode);
        if (gameMode != NOUI_MODE) {
            initFrame(); // 初始化游戏窗口
            setVisible(true);
        }
        initGame(); // 初始化游戏
        setGameState(GAME_START);
    }

    /**
     * Constructs a {@link Game}.
     *
     * @param manager      the manager for creating the game in
     * @param replayBuffer the replay buffer for storing data
     */
    public Game(NDManager manager, ReplayBuffer replayBuffer) {
        this.manager = manager;
        this.replayBuffer = replayBuffer;
        this.currentState = new State(createObservation(bufImg), currentReward, currentTerminal);
        this.postState = new State(createObservation(bufImg), currentReward, currentTerminal);
    }

    public static int timeStep = 0;
    public static int trainStep = 0;
    private static boolean currentTerminal = false;
    private static float currentReward = 0.1f;
    private String trainState = "observe";


//    class TrainImplements implements Runnable {
//        private final RlAgent agent;
//        public TrainImplements(RlAgent agent) {
//            this.agent = agent;
//        }
//        public void run() {
//            while (true) {
//                try {
//                    Thread.sleep(0);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                if (timeStep > TrainBird.OBSERVE) {
//                    System.out.println("*****start training*****");
//                    this.agent.trainBatch(batchSteps);
//                    trainStep++;
//                }
//            }
//        }
//    }

    public Step[] runEnv(RlAgent agent, boolean training) throws CloneNotSupportedException {
        /*
         * input_actions[0] == 1: do nothing
         * input_actions[1] == 1: flap the bird
         */
//        final TrainImplements trainImplements = new TrainImplements(agent);
//        final Thread trainThread = new Thread(trainImplements);
//        if(!trainThread.isAlive()) {
//            trainThread.start();
//        }
        // run the game
        NDList action = agent.chooseAction(this, training);
        step(action, training);
        Step[] batchSteps = this.getBatch();
        if (timeStep <= TrainBird.OBSERVE) {
            trainState = "observe";
        } else {
            trainState = "explore";
        }
        this.currentState = postState.clone();
        timeStep++;
        return batchSteps;
    }

    /**
     * {@inheritDoc}
     * action[0] == 1 : do nothing
     * action[1] == 1 : flap the bird
     */
//    int FRAME_PER_ACTION = 1;
    @Override
    public void step(NDList action, boolean training) {
//        action = new NDList(manager.create(FLAP));
        currentReward = 0.1f;
        currentTerminal = false;
//         动作接收间隔
//        if (timeStep % FRAME_PER_ACTION == 0 && action.singletonOrThrow().getInt(1) == 1) {
        if (action.singletonOrThrow().getInt(1) == 1) {
            bird.birdFlap();
        }
        stepFrame();
        if (gameMode == UI_MODE) {
            repaint();
            try {
                Thread.sleep(FPS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        State preState = new State(postState.getObservation(), currentReward, currentTerminal);
        postState = new State(createObservation(bufImg), currentReward, currentTerminal);

        FlappyBirdStep step = new FlappyBirdStep(manager.newSubManager(), preState, postState, action);
        if (training) {
            replayBuffer.addStep(step);
        }
        System.out.println("TIME_STEP " + timeStep +
                " / " + "TRAIN_STEP " + trainStep +
                " / " + getTrainState() +
                " / " + "ACTION " + (Arrays.toString(action.singletonOrThrow().toArray())) +
                " / " + "REWARD " + step.getReward().getFloat() +
                " / " + "SCORE " + getScore());
        if (gameState == GAME_OVER) {
            resetGame();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NDList getObservation() {
        return currentState.getObservation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionSpace getActionSpace() {
        return currentState.getActionSpace(manager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Step[] getBatch() {
        return replayBuffer.getBatch();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        manager.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
    }

    private final Queue<NDArray> imgQueue = new ArrayDeque<>(4);

    public NDList createObservation(BufferedImage postImg) {
        // 获取连续帧（4）图片：复制当前帧图片 -> 堆积成4帧图片 -> 将获取到得下一帧图片替换当前第4帧，保证当前的batch图片是连续的。
        NDArray observation = GameUtil.imgPreprocess(postImg);
        if (imgQueue.isEmpty()) {
            for (int i = 0; i < 4; i++) {
                imgQueue.offer(observation);
            }
            return new NDList(NDArrays.stack(new NDList(observation, observation, observation, observation), 1));
        } else {
            imgQueue.remove();
            imgQueue.offer(observation);
            NDArray[] buf = new NDArray[4];
            int i = 0;
            for (NDArray nd : imgQueue) {
                buf[i++] = nd;
            }
            return new NDList(NDArrays.stack(new NDList(buf[0], buf[1], buf[2], buf[3]), 1));
        }
    }

    static final class FlappyBirdStep implements RlEnv.Step {
        private final NDManager manager;
        private final State preState;
        private final State postState;
        private final NDList action;

        private FlappyBirdStep(NDManager manager, State preState, State postState, NDList action) {
            this.manager = manager;
            this.preState = preState;
            this.postState = postState;
            this.action = action;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NDList getAction() {
            return action;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NDList getPostObservation() {
            return postState.getObservation();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ActionSpace getPostActionSpace() {
            return postState.getActionSpace(manager);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NDArray getReward() {
            return manager.create(postState.getReward());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isDone() {
            return postState.isTerminal();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NDList getPreObservation() {
            return preState.getObservation();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() {
        }
    }

    private static final class State implements Cloneable {
        private NDList observation;
        private final float reward;
        private final boolean terminal;

        private State(NDList observation, float reward, boolean terminal) {
            this.observation = observation;
            this.reward = reward;
            this.terminal = terminal;
        }

        private NDList getObservation() {
            return this.observation;
        }

        private ActionSpace getActionSpace(NDManager manager) {
            ActionSpace actionSpace = new ActionSpace();
            actionSpace.add(new NDList(manager.create(DO_NOTHING)));
            actionSpace.add(new NDList(manager.create(FLAP)));
            return actionSpace;
        }

        private float getReward() {
            return reward;
        }

        private boolean isTerminal() {
            return terminal;
        }

        @Override
        public State clone() throws CloneNotSupportedException {
            State cloned = (State) super.clone();
            cloned.observation = (NDList) this.observation.clone();
            return cloned;
        }
    }


    private final BufferedImage bufImg = new BufferedImage(FRAME_WIDTH, FRAME_HEIGHT, BufferedImage.TYPE_4BYTE_ABGR);

    /**
     * Draw one frame by performing all elements' draw function.
     */
    public void stepFrame() {
        Graphics bufG = bufImg.getGraphics();
        background.draw(bufG, bird);
        bird.draw(bufG);
        gameElement.draw(bufG, bird);
    }

    private void initFrame() {
        setSize(FRAME_WIDTH, FRAME_HEIGHT); // 设置窗口大小
        setTitle(GAME_TITLE); // 设置窗口标题
        setLocation(FRAME_X, FRAME_Y); // 窗口初始位置
        setResizable(false); // 设置窗口大小不可变
        // 添加关闭窗口事件（监听窗口发生的事件，派发给参数对象，参数对象调用对应的方法）
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    private void initGame() {
        background = new GameBackground();
        gameElement = new GameElementLayer();
        bird = new Bird();
//        new Thread(this).start();  // 启动线程
    }

    /**
     * Restart game
     */
    private void resetGame() {
        setGameState(GAME_START);
        gameElement.reset();
        bird.reset();
    }

    public void update(Graphics g) {
        if (gameMode != NOUI_MODE) {
            g.drawImage(bufImg, 0, 0, null); // 将图片绘制到屏幕
        }
        birdFlapped = false;
    }

    public static void setGameState(int gameState) {
        Game.gameState = gameState;
    }

    public static void setGameMode(int gameMode) {
        Game.gameMode = gameMode;
    }

    public static int getGameMode() {
        return gameMode;
    }

    public String getTrainState() {
        return trainState;
    }

    public boolean isTraining() {
        return gameMode != NORMAL_MODE;
    }

    public static void setCurrentTerminal(boolean currentTerminal) {
        Game.currentTerminal = currentTerminal;
    }

    public static void setCurrentReward(float currentReward) {
        Game.currentReward = currentReward;
    }

    public long getScore() {
        return bird.getCurrentScore();
    }
}

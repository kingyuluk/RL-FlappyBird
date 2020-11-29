package com.kingyu.rlbird.game;

import com.kingyu.rlbird.game.component.Bird;
import com.kingyu.rlbird.game.component.Ground;
import com.kingyu.rlbird.game.component.GameElementLayer;
import com.kingyu.rlbird.rl.ActionSpace;
import com.kingyu.rlbird.rl.LruReplayBuffer;
import com.kingyu.rlbird.rl.ReplayBuffer;
import com.kingyu.rlbird.rl.agent.RlAgent;
import com.kingyu.rlbird.rl.env.RlEnv;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import com.kingyu.rlbird.util.Constant;
import com.kingyu.rlbird.util.GameUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.*;

import static com.kingyu.rlbird.ai.TrainBird.OBSERVE;
import static com.kingyu.rlbird.util.Constant.*;

/**
 * @author Kingyu
 */

public class FlappyBird extends Frame implements RlEnv {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(FlappyBird.class);

    private static int gameState;
    public static final int GAME_START = 1;
    public static final int GAME_OVER = 2;

    private Ground ground;
    private Bird bird;
    private GameElementLayer gameElement;
    private boolean withGraphics;

    private final NDManager manager;
    private final ReplayBuffer replayBuffer;
    private final BufferedImage currentImg = new BufferedImage(FRAME_WIDTH, FRAME_HEIGHT, BufferedImage.TYPE_4BYTE_ABGR);
    private State currentState;

    /**
     * Constructs a {@link FlappyBird} with a basic {@link LruReplayBuffer}.
     *
     * @param manager          the manager for creating the game in
     * @param batchSize        the number of steps to train on per batch
     * @param replayBufferSize the number of steps to hold in the buffer
     */
    public FlappyBird(NDManager manager, int batchSize, int replayBufferSize, boolean withGraphics) {
        this(manager, new LruReplayBuffer(batchSize, replayBufferSize));
        this.withGraphics = withGraphics;
        if (this.withGraphics) {
            initFrame();
            this.setVisible(true);
        }
        ground = new Ground();
        gameElement = new GameElementLayer();
        bird = new Bird();
        setGameState(GAME_START);
    }

    /**
     * Constructs a {@link FlappyBird}.
     *
     * @param manager      the manager for creating the game in
     * @param replayBuffer the replay buffer for storing data
     */
    public FlappyBird(NDManager manager, ReplayBuffer replayBuffer) {
        this.manager = manager;
        this.replayBuffer = replayBuffer;
        this.currentState = new State(createObservation(currentImg), currentReward, currentTerminal);
    }

    public static int gameStep = 0;
    public static int trainStep = 0;
    private static boolean currentTerminal = false;
    private static float currentReward = 0.2f;
    private String trainState = "observe";

    /**
     * {@inheritDoc}
     */
    @Override
    public Step[] runEnvironment(RlAgent agent, boolean training) {
        Step[] batchSteps = new Step[0];
        reset();

        // run the game
        NDList action = agent.chooseAction(this, training);
        step(action, training);
        if(training) {
            batchSteps = this.getBatch();
        }
        if (gameStep % 5000 == 0){
            this.closeStep();
        }
        if (gameStep <= OBSERVE) {
            trainState = "observe";
        } else {
            trainState = "explore";
        }
        gameStep++;
        return batchSteps;
    }

    /**
     * {@inheritDoc}
     * action[0] == 1 : do nothing
     * action[1] == 1 : flap the bird
     */
    @Override
    public void step(NDList action, boolean training) {
        if (action.singletonOrThrow().getInt(1) == 1) {
            bird.birdFlap();
        }
        stepFrame();
        if (this.withGraphics) {
            repaint();
            try {
                Thread.sleep(FPS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        State preState = new State(currentState.getObservation(), currentReward, currentTerminal);
        currentState = new State(createObservation(currentImg), currentReward, currentTerminal);

        FlappyBirdStep step = new FlappyBirdStep(manager.newSubManager(), preState, currentState, action);
        if (training) {
            replayBuffer.addStep(step);
        }
        logger.info("GAME_STEP " + gameStep +
                " / " + "TRAIN_STEP " + trainStep +
                " / " + getTrainState() +
                " / " + "ACTION " + (Arrays.toString(action.singletonOrThrow().toArray())) +
                " / " + "REWARD " + step.getReward().getFloat() +
                " / " + "SCORE " + getScore());
        if (gameState == GAME_OVER) {
            restartGame();
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

    public void closeStep() {
        replayBuffer.closeStep();
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
        currentReward = 0.2f;
        currentTerminal = false;
    }

    private final Queue<NDArray> imgQueue = new ArrayDeque<>(4);

    /**
     * Convert image to CNN input.
     * Copy the initial frame image, stack into NDList,
     * then replace the fourth frame with the current frame to ensure that the batch picture is continuous.
     *
     * @param currentImg the image of current frame
     * @return the CNN input
     */
    public NDList createObservation(BufferedImage currentImg) {
        NDArray observation = GameUtil.imgPreprocess(currentImg);
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

    static final class FlappyBirdStep implements RlEnv.Step, Cloneable {
        private final NDManager manager;
        private final State preState;
        private final State postState;
        private final NDList action;

        private FlappyBirdStep(NDManager manager, State preState, State postState, NDList action)  {
            this.manager = manager;
            this.preState = preState;
            this.postState = postState;
            this.action = action;
        }

        public NDManager getManager() {
            return this.manager;
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
        public NDList getPreObservation(NDManager manager) {
            return preState.getObservation(manager);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public NDList getPostObservation(NDManager manager) {
            return postState.getObservation(manager);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void attachPostStateManager(NDManager manager) {
            postState.attachManager(manager);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void attachPreStateManager(NDManager manager) {
            preState.attachManager(manager);
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
        public void close() {
            this.manager.close();
        }
    }

    private static final class State {
        private final NDList observation;
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

        private NDList getObservation(NDManager manager) {
            observation.attach(manager);
            return this.observation;
        }

        private void attachManager(NDManager manager) {
            observation.attach(manager);
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

    }

    /**
     * Draw one frame by performing all elements' draw function.
     */
    public void stepFrame() {
        Graphics bufG = currentImg.getGraphics();
        bufG.setColor(Constant.BG_COLOR);
        bufG.fillRect(0, 0, Constant.FRAME_WIDTH, Constant.FRAME_HEIGHT);
        ground.draw(bufG, bird);
        bird.draw(bufG);
        gameElement.draw(bufG, bird);
    }

    /**
     * Initialize the game frame
     */
    private void initFrame() {
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setTitle(GAME_TITLE);
        setLocation(FRAME_X, FRAME_Y);
        setResizable(false);
        setVisible(true);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    /**
     * Restart game
     */
    private void restartGame() {
        setGameState(GAME_START);
        gameElement.reset();
        bird.reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(Graphics g) {
        g.drawImage(currentImg, 0, 0, null);
    }

    public static void setGameState(int gameState) {
        FlappyBird.gameState = gameState;
    }

    public String getTrainState() {
        return this.trainState;
    }

    public static void setCurrentTerminal(boolean currentTerminal) {
        FlappyBird.currentTerminal = currentTerminal;
    }

    public static void setCurrentReward(float currentReward) {
        FlappyBird.currentReward = currentReward;
    }

    public long getScore() {
        return this.bird.getCurrentScore();
    }
}

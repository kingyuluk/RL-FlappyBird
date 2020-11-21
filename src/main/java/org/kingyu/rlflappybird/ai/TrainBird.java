package org.kingyu.rlflappybird.ai;

import ai.djl.Model;
import ai.djl.training.evaluator.Accuracy;
import ai.djl.training.optimizer.Adam;
import org.kingyu.rlflappybird.rl.agent.EpsilonGreedy;
import org.kingyu.rlflappybird.rl.agent.QAgent;
import org.kingyu.rlflappybird.rl.agent.RlAgent;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Activation;
import ai.djl.nn.Blocks;
import ai.djl.nn.SequentialBlock;
import ai.djl.nn.convolutional.Conv2d;
import ai.djl.nn.core.Linear;
import ai.djl.nn.pooling.Pool;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.Trainer;
import ai.djl.training.listener.TrainingListener;
import ai.djl.training.loss.Loss;
import ai.djl.training.tracker.LinearTracker;
import ai.djl.training.tracker.Tracker;
import org.kingyu.rlflappybird.game.Game;
import org.kingyu.rlflappybird.util.Arguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrainBird {
    private static final Logger logger = LoggerFactory.getLogger(TrainBird.class);

    public final static int OBSERVE = 1000; // timeSteps to observe before training
    public final static int EXPLORE = 3000000; // frames over which to anneal epsilon

    private TrainBird() {
    }

    public static void main(String[] args) {
        TrainBird.train(args);
    }

    public static void train(String[] args) {
        Arguments arguments = Arguments.parseArgs(args);

        int gameMode = 1;  // 1:no ui   2:ui
        int batchSize = 32;  // size of mini batch
        int replayBufferSize = 50000; // number of previous transitions to remember;
        float rewardDiscount = 0.99f;  // decay rate of past observations
        float INITIAL_EPSILON = 0.1f;
        float FINAL_EPSILON = 0.0001f;

        Game game = new Game(NDManager.newBaseManager(), batchSize, replayBufferSize, gameMode);

        SequentialBlock block = getBlock();

        try (Model model = Model.newInstance("QNetwork")) {
            model.setBlock(block);

            DefaultTrainingConfig config = setupTrainingConfig();
            try (Trainer trainer = model.newTrainer(config)) {
                trainer.initialize(new Shape(1, 4, 80, 80));

                trainer.notifyListeners(listener -> listener.onTrainingBegin(trainer));

                RlAgent agent = new QAgent(trainer, rewardDiscount);
                Tracker exploreRate =
                        new LinearTracker.Builder()
                                .setBaseValue(INITIAL_EPSILON)
                                .optSlope(-(INITIAL_EPSILON - FINAL_EPSILON) / EXPLORE)
                                .optMinValue(FINAL_EPSILON)
                                .build();
                agent = new EpsilonGreedy(agent, exploreRate);

                game.runEnv(agent, trainer, true);

//                // 输出神经网络的结构
//                Shape currentShape = new Shape(1, 4, 80, 80);
//                for (int i = 0; i < block.getChildren().size(); i++) {
//                    Shape[] newShape = block.getChildren().get(i).getValue().getOutputShapes(NDManager.newBaseManager(), new Shape[]{currentShape});
//                    currentShape = newShape[0];
//                    System.out.println(block.getChildren().get(i).getKey() + " layer output : " + currentShape);
//                }

            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }

    }

    public static SequentialBlock getBlock() {
        // conv（8*8*4*32) -> maxPool -> conv(4*4*32*64) -> maxPool -> conv(3*3*64*64）-> fc  -> fc [shape(1,2)]
        return new SequentialBlock()
                .add(Conv2d.builder()
                        .setKernelShape(new Shape(8, 8))
                        .optStride(new Shape(4, 4))
                        .optPadding(new Shape(3, 3))
                        .setFilters(32).build())
                .add(Activation::relu)
                .add(Pool.maxPool2dBlock(new Shape(2, 2)))

                .add(Conv2d.builder()
                        .setKernelShape(new Shape(4, 4))
                        .optStride(new Shape(2, 2))
                        .optPadding(new Shape(1, 1))
                        .setFilters(64).build())
                .add(Activation::relu)
                .add(Pool.maxPool2dBlock(new Shape(2, 2)))

                .add(Conv2d.builder()
                        .setKernelShape(new Shape(3, 3))
                        .optStride(new Shape(1, 1))
                        .optPadding(new Shape(1, 1))
                        .setFilters(64).build())
                .add(Activation::relu)

                .add(Blocks.batchFlattenBlock())
                .add(Linear
                        .builder()
                        .setUnits(256).build())
                .add(Activation::relu)

                .add(Linear
                        .builder()
                        .setUnits(2).build());
    }

    public static DefaultTrainingConfig setupTrainingConfig() {
        return new DefaultTrainingConfig(Loss.l2Loss())
                .optOptimizer(Adam.builder().optLearningRateTracker(Tracker.fixed(1e-6f)).build())
                .addEvaluator(new Accuracy())
                .addTrainingListeners(TrainingListener.Defaults.basic());
    }
}

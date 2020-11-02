package org.bird.ai;

import ai.djl.Model;
import ai.djl.training.evaluator.Accuracy;
import ai.djl.training.optimizer.Adam;
import org.bird.rl.agent.EpsilonGreedy;
import org.bird.rl.agent.QAgent;
import org.bird.rl.agent.RlAgent;
import org.bird.rl.env.RlEnv;
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
import ai.djl.training.optimizer.Optimizer;
import ai.djl.training.tracker.LinearTracker;
import ai.djl.training.tracker.Tracker;
import org.apache.commons.cli.ParseException;
import org.bird.main.GameFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DQN {
    private static final Logger logger = LoggerFactory.getLogger(DQN.class);

    public static void main(String[] arg) throws ParseException {
        DQN.train();
    }

    public static void train() throws ParseException {
//        Arguments arguments = Arguments.parseArgs(args);
        NDManager manager = NDManager.newBaseManager();

        int epoch = 10;
        int batchSize = 32;  // size of mini batch
        int replayBufferSize = 50000; // number of previous transitions to remember;
        int EXPLORE = 100000; // frames over which to anneal epsilon
        float INITIAL_EPSILON = 0.01f;
        float FINAL_EPSILON = 0.0001f; // final value of epsilon
        float rewardDiscount = 0.99f;

        GameFrame game = new GameFrame(manager, batchSize, replayBufferSize);
        SequentialBlock block = getBlock();

        try (Model model = Model.newInstance("QNetwork")) {
            model.setBlock(block);
            DefaultTrainingConfig config = setupTrainingConfig();
            try (Trainer trainer = model.newTrainer(config)) {

                Shape inputShape = new Shape(batchSize, 1, 80, 80);

                trainer.initialize(inputShape);

                trainer.notifyListeners(listener -> listener.onTrainingBegin(trainer));

                RlAgent agent = new QAgent(trainer, rewardDiscount);

                Tracker exploreRate =
                        new LinearTracker.Builder()
                                .setBaseValue(INITIAL_EPSILON)
                                .optSlope(-(INITIAL_EPSILON - FINAL_EPSILON) / EXPLORE)
                                .optMinValue(FINAL_EPSILON)
                                .build();
                agent = new EpsilonGreedy(agent, exploreRate);

                float reward;
                game.repaint();
                while (true) {
                    game.runEnv(agent, true);
                    // obtain random minibatch from replay memory
                    RlEnv.Step[] batchSteps = game.getBatch();
                    agent.trainBatch(batchSteps);
                    trainer.step();

//                    logger.info("reward: {}", (reward));
//                    trainer.notifyListeners(listener -> listener.onEpoch(trainer));
                }

//                // 输出神经网络的结构
//                Shape currentShape = new Shape(1, 4, 80, 80);
//                for (int i = 0; i < block.getChildren().size(); i++) {
//                    Shape[] newShape = block.getChildren().get(i).getValue().getOutputShapes(manager, new Shape[]{currentShape});
//                    currentShape = newShape[0];
//                    System.out.println(block.getChildren().get(i).getKey() + " layer output : " + currentShape);
//                }


            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public static SequentialBlock getBlock() {
        // 2个卷积层（8*8*4*32，4*4*32*64，3*3*64*64），2个(2*2)池化层，3个Relu激活函数，2个全连接层
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
                .optOptimizer(Adam.builder().optLearningRateTracker(Tracker.fixed(0.0001f)).build())
                .addEvaluator(new Accuracy())
                .addTrainingListeners(TrainingListener.Defaults.basic());
    }
}

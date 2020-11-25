/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.kingyu.rlflappybird.rl.agent;

import ai.djl.ndarray.NDArrays;
import com.kingyu.rlflappybird.rl.ActionSpace;
import com.kingyu.rlflappybird.rl.env.RlEnv;
import com.kingyu.rlflappybird.rl.env.RlEnv.Step;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.training.GradientCollector;
import ai.djl.training.Trainer;
import ai.djl.training.listener.TrainingListener.BatchData;
import ai.djl.translate.Batchifier;


import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An {@link RlAgent} that implements Q or Deep-Q Learning.
 *
 * <p>Deep-Q Learning estimates the total reward that will be given until the environment ends in a
 * particular state after taking a particular action. Then, it is trained by ensuring that the
 * prediction before taking the action match what would be predicted after taking the action. More
 * information can be found in the <a
 * href="https://www.cs.toronto.edu/~vmnih/docs/dqn.pdf">paper</a>.
 *
 * <p>It is one of the earliest successful techniques for reinforcement learning with Deep learning.
 * It is also a good introduction to the field. However, many better techniques are commonly used
 * now.
 */
public class QAgent implements RlAgent {

    private final Trainer trainer;
    private final float rewardDiscount;

    /**
     * Constructs a {@link ai.djl.modality.rl.agent.QAgent} with a custom {@link Batchifier}.
     *
     * @param trainer        the trainer for the model to learn
     * @param rewardDiscount the reward discount to apply to rewards from future states
     */
    public QAgent(Trainer trainer, float rewardDiscount) {
        this.trainer = trainer;
        this.rewardDiscount = rewardDiscount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NDList chooseAction(RlEnv env, boolean training) {
        ActionSpace actionSpace = env.getActionSpace();
        NDArray actionReward = trainer.evaluate(env.getObservation()).singletonOrThrow().get(0);
        System.out.println(Arrays.toString(actionReward.toFloatArray()));
        int bestAction = Math.toIntExact(actionReward.argMax().getLong());
        return actionSpace.get(bestAction);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    // TODO : may have problem
    public void trainBatch(Step[] batchSteps) {
        /* Initialize replay memory D to size N
         * Initialize action-value function Q with random weights
         * for episode = 1, M do
         *     Initialize state s_1
         *     for t = 1, T do
         *         With probability ϵ select random action a_t
         *         otherwise select a_t=max_a  Q(s_t,a; θ_i)
         *         Execute action a_t in emulator and observe r_t and s_(t+1)
         *         Store transition (s_t,a_t,r_t,s_(t+1)) in D
         *         Sample a minibatch of transitions (s_j,a_j,r_j,s_(j+1)) from D
         *         Set y_j:=
         *             r_j for terminal s_(j+1)
         *             r_j+γ*max_(a^' )  Q(s_(j+1),a'; θ_i) for non-terminal s_(j+1)
         *         Perform a gradient step on (y_j-Q(s_j,a_j; θ_i))^2 with respect to θ
         *     end for
         * end for
         **/
        BatchData batchData =
                new BatchData(null, new ConcurrentHashMap<>(), new ConcurrentHashMap<>());

        NDList preObservationBatch = new NDList();
        Arrays.stream(batchSteps).forEach(step -> preObservationBatch.addAll(step.getPreObservation()));
        NDList preInput = new NDList(NDArrays.concat(preObservationBatch, 0));

        NDList postObservationBatch = new NDList();
        Arrays.stream(batchSteps).forEach(step -> postObservationBatch.addAll(step.getPreObservation()));
        NDList postInput = new NDList(NDArrays.concat(postObservationBatch, 0));

        NDList actionBatch = new NDList();
        Arrays.stream(batchSteps).forEach(step -> actionBatch.addAll(step.getAction()));
        NDList actionInput = new NDList(NDArrays.stack(actionBatch, 0));

        NDList rewardBatch = new NDList();
        Arrays.stream(batchSteps).forEach(step -> rewardBatch.addAll(new NDList(step.getReward())));
        NDList rewardInput = new NDList(NDArrays.stack(rewardBatch, 0));

        try (GradientCollector collector = trainer.newGradientCollector()) {
            NDList QReward = trainer.forward(preInput);
            NDList targetQReward = trainer.forward(postInput);

//            NDList _targetQ = new NDList(targetQReward.singletonOrThrow()
//                    .max(new int[]{1})
//                    .mul(rewardDiscount)
//                    .add(rewardInput.singletonOrThrow()));

            NDList Q = new NDList(QReward.singletonOrThrow()
                    .mul(actionInput.singletonOrThrow())
                    .sum(new int[]{1}));

            NDArray[] targetQValue = new NDArray[batchSteps.length];
            for (int i = 0; i < batchSteps.length; i++) {
                if (batchSteps[i].isDone()){
                    targetQValue[i] = batchSteps[i].getReward();
                }
                else{
                    targetQValue[i] = targetQReward.singletonOrThrow().get(i)
                            .max()
                            .mul(rewardDiscount)
                            .add(rewardInput.singletonOrThrow().get(i));
                }
            }
            NDList targetQBatch = new NDList();
            Arrays.stream(targetQValue).forEach(value -> targetQBatch.addAll(new NDList(value)));
            NDList targetQ = new NDList(NDArrays.stack(targetQBatch, 0));

            NDArray lossValue = trainer.getLoss().evaluate(targetQ, Q);
            collector.backward(lossValue);
            batchData.getLabels().put(targetQ.singletonOrThrow().getDevice(), targetQ);
            batchData.getPredictions().put(Q.singletonOrThrow().getDevice(), Q);
            this.trainer.step();
        }


//            NDList[] Q = new NDList[batchSteps.length];
//            NDList[] targetQ = new NDList[batchSteps.length];
//            for (int i = 0; i < batchSteps.length; i++) {
//                Q[i] = new NDList(QReward.singletonOrThrow().get(i)
//                        .mul(batchSteps[i].getAction().singletonOrThrow()).sum());

//                if (batchSteps[i].isDone()) {
//                    targetQ[i] = new NDList(batchSteps[i].getReward());
//                } else {
//                    targetQ[i] =
//                            new NDList(targetQReward.singletonOrThrow().get(i).max()
//                                    .mul(rewardDiscount).add(batchSteps[i].getReward()));
//                }
//                NDArray lossValue = trainer.getLoss().evaluate(targetQ[i], Q[i]);
//                collector.backward(lossValue);
//                batchData.getLabels().put(targetQ[i].get(0).getDevice(), targetQ[i]);
//                batchData.getPredictions().put(Q[i].get(0).getDevice(), Q[i]);
//                this.trainer.step();
//            }
        /*
         * self.trainStep.run(feed_dict={
         *        self.yInput : y_batch,
         *        self.actionInput : action_batch,
         *         self.observationInput : observation_batch
         *         })
         */
    }
//        trainer.notifyListeners(listener -> listener.onTrainingBatch(trainer, batchData));


    private NDList[] buildInputs(NDList observation) {
        NDList[] inputs = new NDList[32];
        for (int i = 0; i < 32; i++) {
            NDList nextData = new NDList().addAll(observation);
            inputs[i] = nextData;
        }
        return inputs;
    }
}

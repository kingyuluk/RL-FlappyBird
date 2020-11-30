package com.kingyu.rlbird.rl.agent;

import com.kingyu.rlbird.rl.env.RlEnv;
import ai.djl.ndarray.NDList;

/**
 * An {@link ai.djl.modality.rl.agent.RlAgent} is the model or technique to decide the actions to take in an {@link RlEnv}.
 */
public interface RlAgent {

    /**
     * Chooses the next action to take within the {@link RlEnv}.
     *
     * @param env the current environment
     * @param training true if the agent is currently traning
     * @return the action to take
     */
    NDList chooseAction(RlEnv env, boolean training);

    /**
     * Trains this {@link ai.djl.modality.rl.agent.RlAgent} on a batch of {@link RlEnv.Step}s.
     *
     * @param batchSteps the steps to train on
     */
    void trainBatch(RlEnv.Step[] batchSteps);
}

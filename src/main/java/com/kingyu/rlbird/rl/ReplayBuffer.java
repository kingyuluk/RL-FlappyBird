package com.kingyu.rlbird.rl;

import com.kingyu.rlbird.rl.env.RlEnv;

/**
 * Records {@link RlEnv.Step}s so that they can be trained on.
 *
 * <p>Using a replay buffer ensures that a variety of states are trained on for every training batch
 * making the training more stable.
 */
public interface ReplayBuffer {

    /**
     * Returns a batch of steps from this buffer.
     *
     * @return a batch of steps from this buffer
     */
    RlEnv.Step[] getBatch();

    /**
     * close the step not pointed to.
     */
    void closeStep();

    /**
     * Adds a new step to the buffer.
     *
     * @param step the step to add
     */
    void addStep(RlEnv.Step step);
}

package com.kingyu.rlbird.util;

import org.apache.commons.cli.*;

public class Arguments {

    private final int batchSize;
    private final boolean withGraphics;
    private final boolean preTrained;

    public Arguments(CommandLine cmd) {
        withGraphics = cmd.hasOption("graphics");

        if (cmd.hasOption("batch-size")) {
            batchSize = Integer.parseInt(cmd.getOptionValue("batch-size"));
        } else {
            batchSize = 32;
        }

        preTrained = cmd.hasOption("pre-trained");
    }

    public static Arguments parseArgs(String[] args) throws ParseException {
        Options options = Arguments.getOptions();
        DefaultParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args, null, false);
        return new Arguments(cmd);
    }

    public static Options getOptions() {
        Options options = new Options();
        options.addOption(
                Option.builder("g")
                        .longOpt("graphics")
                        .argName("GRAPHICS")
                        .desc("Training with graphics")
                        .build());
        options.addOption(
                Option.builder("b")
                        .longOpt("batch-size")
                        .hasArg()
                        .argName("BATCH-SIZE")
                        .desc("The batch size of the training data.")
                        .build());
        options.addOption(
                Option.builder("p")
                        .longOpt("pre-trained")
                        .argName("PRE-TRAINED")
                        .desc("Use pre-trained weights")
                        .build());
        return options;
    }

    public boolean isWithGraphics(){
        return withGraphics;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public boolean isPreTrained() {
        return preTrained;
    }
}

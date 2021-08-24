package com.infoworks.lab.controllers.batch;

import org.springframework.core.env.Environment;

public abstract class AbstractBatchScheduler {

    private Environment env;

    public AbstractBatchScheduler(Environment env) {
        this.env = env;
    }

    public abstract void process();

    protected int getBatchSize(){
        String val = env.getProperty("batch.processing.batch.size");
        return (val != null) ? Integer.valueOf(val) : 1000;
    }

    protected int getOffset(){
        String val = env.getProperty("batch.processing.batch.offset");
        return (val != null) ? Integer.valueOf(val) : 0;
    }

    protected int getMaxCount(){
        String val = env.getProperty("batch.processing.batch.max.size");
        return (val != null) ? Integer.valueOf(val) : -1;
    }

    protected int getDayMinusCount(){
        String val = env.getProperty("batch.processing.day.minus.count");
        return (val != null) ? Integer.valueOf(val) : -1;
    }

    protected boolean shouldPreventExecution(){
        String val = env.getProperty("batch.processing.cron.prevent.execution");
        return (val != null) ? Boolean.valueOf(val) : false;
    }
}

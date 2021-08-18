package com.infoworks.lab.controllers.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SampleBatchProcess {

    private Environment env;
    private JobLauncher jobLauncher;
    private Job job;

    public SampleBatchProcess(Environment env
            , JobLauncher jobLauncher
            , Job job) {
        this.env = env;
        this.jobLauncher = jobLauncher;
        this.job = job;
    }

    /**
     *  (second, minute, hour, day of month, month, day(s) of week)
     *  (* * * * * *)
     *
     *  "0 0 * * * *" = the top of every hour of every day.
     *  "*\/10 * * * * *" = every ten seconds.
     *  "0 0 8-10 * * *" = 8, 9 and 10 o'clock of every day.
     *  "0 0 8,10 * * *" = 8 and 10 o'clock of every day.
     *  "0 0/30 8-10 * * *" = 8:00, 8:30, 9:00, 9:30 and 10 o'clock every day.
     *  "0 0 9-17 * * MON-FRI" = on the hour nine-to-five weekdays
     *  "0 0 0 25 12 ?" = every Christmas Day at midnight
     */

    @Scheduled(cron = "${batch.processing.cron.expression}")
    public void process()
            throws JobParametersInvalidException, JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {

        if (shouldPreventExecution()) return;
        //
        System.out.println("Running");
        JobParameters params = new JobParametersBuilder()
                .addString("JobID", String.valueOf(System.currentTimeMillis()))
                .toJobParameters();
        jobLauncher.run(job, params);
    }

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

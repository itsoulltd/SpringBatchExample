package com.infoworks.lab.webapp.config;

import com.infoworks.lab.controllers.batch.steps.PassengerListProcessor;
import com.infoworks.lab.controllers.batch.steps.PassengerListWriter;
import com.infoworks.lab.controllers.batch.steps.PassengerRowsMapper;
import com.infoworks.lab.controllers.batch.tasks.*;
import com.infoworks.lab.domain.entities.Passenger;
import com.it.soul.lab.sql.SQLExecutor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.builder.JobFlowBuilder;
import org.springframework.batch.core.job.builder.SimpleJobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

@Configuration
@EnableScheduling
@EnableBatchProcessing
public class BatchConfig {

    private JobBuilderFactory jobs;
    private StepBuilderFactory steps;

    public BatchConfig(JobBuilderFactory jobs, StepBuilderFactory steps) {
        this.jobs = jobs;
        this.steps = steps;
    }

    @Bean("taskletJobSample")
    public Job demoJob(){

        Step one = steps.get("stepOne")
                .tasklet(new MyTaskOne())
                .build();

        Step two = steps.get("stepTwo")
                .tasklet(new MyTaskTwo())
                .build();

        return jobs.get("demoJob")
                .incrementer(new RunIdIncrementer())
                .start(one)
                .next(two)
                .build();
    }

    @Value("${batch.processing.batch.size}")
    private Integer batchSize;

    @Value("${batch.processing.batch.offset}")
    private Integer batchOffset;

    @Value("${batch.processing.batch.max.size}")
    private Integer batchMaxSize;

    @Bean("jdbcJobSample")
    public Job sampleJob(DataSource dataSource) throws SQLException {

        SQLExecutor executor = new SQLExecutor(dataSource.getConnection());

        JdbcCursorItemReader<List<Passenger>> itemReader = new JdbcCursorItemReaderBuilder<List<Passenger>>()
                .dataSource(dataSource)
                .name("PassengerReader")
                .sql("select * from Passenger")
                .rowMapper(new PassengerRowsMapper(executor))
                .maxRows(batchSize)
                .build();

        Step one = steps.get("stepOne")
                .<List<Passenger>, List<Passenger>>chunk(batchSize)
                .reader(itemReader)
                .processor(new PassengerListProcessor())
                .writer(new PassengerListWriter())
                .build();

        return jobs.get("sampleJob")
                .incrementer(new RunIdIncrementer())
                .flow(one)
                .end()
                .build();
    }

    @Bean("jdbcMultiStepJobSample")
    public Job multiStepSampleJob(DataSource dataSource) throws SQLException {

        JobFlowBuilder batchJobBuilder = null;
        SQLExecutor executor = new SQLExecutor(dataSource.getConnection());

        String entity = Passenger.tableName(Passenger.class);
        int cursor = batchOffset;
        while (batchMaxSize != -1 && cursor < batchMaxSize){

            String query = String.format("SELECT * FROM %s LIMIT %s, %s", entity, cursor,  batchSize);
            System.out.println(query);

            JdbcCursorItemReader<List<Passenger>> itemReader = new JdbcCursorItemReaderBuilder<List<Passenger>>()
                    .dataSource(dataSource)
                    .name(String.format("%s_Reader", entity))
                    .sql(query)
                    .fetchSize(batchSize)
                    .rowMapper(new PassengerRowsMapper(executor))
                    .build();

            Step batch = steps.get(String.format("batchStep_%s", cursor))
                    .<List<Passenger>, List<Passenger>>chunk(batchSize)
                    .reader(itemReader)
                    .processor(new PassengerListProcessor())
                    .writer(new PassengerListWriter())
                    .build();

            if (batchJobBuilder == null){
                batchJobBuilder = jobs.get("multiSampleJob")
                        .incrementer(new RunIdIncrementer())
                        .flow(batch);
            }else {
                batchJobBuilder.next(batch);
            }
            cursor = cursor + batchSize; //Loop-Increment
        }

        return batchJobBuilder.end().build();
    }

}

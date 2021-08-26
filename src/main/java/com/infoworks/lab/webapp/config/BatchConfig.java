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
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
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

    @Bean("jdbcJobSample")
    public Job sampleJob(DataSource dataSource) throws SQLException {

        SQLExecutor executor = new SQLExecutor(dataSource.getConnection());

        JdbcCursorItemReader<List<Passenger>> itemReader = new JdbcCursorItemReaderBuilder<List<Passenger>>()
                .dataSource(dataSource)
                .name("PassengerReader")
                .sql("select * from Passenger")
                .rowMapper(new PassengerRowsMapper(executor))
                .maxRows(10)
                .build();

        Step one = steps.get("stepOne")
                .<List<Passenger>, List<Passenger>>chunk(10)
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

}

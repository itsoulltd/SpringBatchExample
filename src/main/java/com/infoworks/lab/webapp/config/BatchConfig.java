package com.infoworks.lab.webapp.config;

import com.infoworks.lab.controllers.batch.steps.PassengerMapper;
import com.infoworks.lab.controllers.batch.steps.PassengerProcessor;
import com.infoworks.lab.controllers.batch.steps.PassengerWriter;
import com.infoworks.lab.controllers.batch.tasks.MyTaskOne;
import com.infoworks.lab.controllers.batch.tasks.MyTaskTwo;
import com.infoworks.lab.domain.entities.Passenger;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.JobFlowBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

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

        ItemReader<Passenger> itemReader = new JdbcCursorItemReaderBuilder<Passenger>()
                .dataSource(dataSource)
                .name("PassengerReader")
                .sql("SELECT * FROM Passenger")
                .rowMapper(new PassengerMapper()) //OR Next Line
                //.rowMapper(new BeanPropertyRowMapper(Passenger.class))
                .fetchSize(batchSize)
                //.maxRows(batchSize)
                .build();

        Step one = steps.get("stepOne")
                .<Passenger, Passenger>chunk(batchSize)
                .reader(itemReader)
                .processor(new PassengerProcessor())
                .writer(new PassengerWriter())
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

        String entity = Passenger.tableName(Passenger.class);
        int cursor = batchOffset;
        while (batchMaxSize != -1 && cursor < batchMaxSize){

            String query = String.format("SELECT * FROM %s LIMIT %s, %s", entity, cursor,  batchSize);
            System.out.println(query);

            ItemReader<Passenger> itemReader = new JdbcCursorItemReaderBuilder<Passenger>()
                    .dataSource(dataSource)
                    .name(String.format("%s_Reader", entity))
                    .sql(query)
                    .rowMapper(new PassengerMapper())//OR Next Line
                    //.rowMapper(new BeanPropertyRowMapper(Passenger.class))
                    .fetchSize(batchSize)
                    .build();

            Step batch = steps.get(String.format("batchStep_%s", cursor))
                    .<Passenger, Passenger>chunk(batchSize/2)
                    .reader(itemReader)
                    .processor(new PassengerProcessor())
                    .writer(new PassengerWriter())
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

    @Bean("jdbcMultiStepPagingJobSample")
    public Job multiStepPagingJob(DataSource dataSource, PagingQueryProvider provider) throws SQLException {
        //
        ItemReader<Passenger> itemReader = new JdbcPagingItemReaderBuilder<Passenger>()
                .name("pagingItemReader")
                .dataSource(dataSource)
                .pageSize(batchSize)
                .queryProvider(provider)
                .rowMapper(new BeanPropertyRowMapper<>(Passenger.class))
                .build();

        Step one = steps.get("stepPaging")
                .<Passenger, Passenger>chunk(batchSize)
                .reader(itemReader)
                .processor(new PassengerProcessor())
                .writer(new PassengerWriter())
                .build();

        return jobs.get("samplePageJob")
                .incrementer(new RunIdIncrementer())
                .flow(one)
                .end()
                .build();
    }

    @Bean
    public SqlPagingQueryProviderFactoryBean pagingQueryProviderFactoryBean(DataSource dataSource){
        //
        Map<String, Order> sortConfiguration = new HashMap<>();
        sortConfiguration.put("id", Order.ASCENDING);
        //
        SqlPagingQueryProviderFactoryBean provider =
                new SqlPagingQueryProviderFactoryBean();
        provider.setDataSource(dataSource);
        provider.setSelectClause("id, name, sex, age, dob, active");
        provider.setFromClause("Passenger");
        provider.setSortKeys(sortConfiguration);
        return provider;
    }

}

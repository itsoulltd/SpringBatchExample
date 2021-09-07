package com.infoworks.lab.webapp.config;

import com.infoworks.lab.controllers.batch.message.MessageProcessor;
import com.infoworks.lab.controllers.batch.message.MessageReader;
import com.infoworks.lab.controllers.batch.message.MessageWriter;
import com.infoworks.lab.controllers.batch.passenger.PassengerMapper;
import com.infoworks.lab.controllers.batch.passenger.PassengerProcessor;
import com.infoworks.lab.controllers.batch.passenger.PassengerWriter;
import com.infoworks.lab.controllers.batch.tasks.MyTaskOne;
import com.infoworks.lab.controllers.batch.tasks.MyTaskTwo;
import com.infoworks.lab.domain.entities.Passenger;
import com.infoworks.lab.rest.models.Message;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobFlowBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableBatchProcessing
@PropertySource("classpath:batch-job.properties")
public class BatchConfig {

    private JobBuilderFactory jobs;
    private StepBuilderFactory steps;

    public BatchConfig(JobBuilderFactory jobs, StepBuilderFactory steps) {
        this.jobs = jobs;
        this.steps = steps;
    }

    @Value("${batch.processing.batch.size}")
    private Integer batchSize;

    @Value("${batch.processing.batch.offset}")
    private Integer batchOffset;

    @Value("${batch.processing.batch.max.size}")
    private Integer batchMaxSize;

    @Bean("simpleJob")
    public Job simpleJob(){

        Step one = steps.get("stepOne")
                .<Message, Message>chunk(batchSize)
                .reader(new MessageReader())
                .processor(new MessageProcessor())
                .writer(new MessageWriter())
                .build();

        return jobs.get("simpleJob")
                .incrementer(new RunIdIncrementer())
                .start(one)
                .build();
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
    public Job multiStepPagingJob(@Qualifier("pagedPassengerReader") JdbcPagingItemReader<Passenger> itemReader)
            throws SQLException {
        //
        Step one = steps.get("stepPaging")
                .<Passenger, Passenger>chunk(batchSize)
                .reader(itemReader)
                .processor(new PassengerProcessor())
                .writer(new PassengerWriter())
                .build();

        return jobs.get("samplePageJob")
                .incrementer(new RunIdIncrementer())
                .start(one)
                .build();
    }

    @Bean("pagedPassengerReader")
    @StepScope
    public JdbcPagingItemReader<Passenger> pagingItemReader(DataSource dataSource) {
        //
        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("id", Order.ASCENDING);
        //
        MySqlPagingQueryProvider queryProvider = new MySqlPagingQueryProvider();
        queryProvider.setSelectClause("id, name, sex, age, dob, active");
        queryProvider.setFromClause("Passenger");
        //queryProvider.setWhereClause("where id >= " + minValue + " and id < " + maxValue);
        queryProvider.setSortKeys(sortKeys);
        //
        JdbcPagingItemReader<Passenger> reader = new JdbcPagingItemReader<>();
        reader.setName("pagingItemReader");
        reader.setDataSource(dataSource);
        reader.setFetchSize(batchSize);
        reader.setRowMapper(new BeanPropertyRowMapper<>(Passenger.class));
        reader.setQueryProvider(queryProvider);
        return reader;
    }

}

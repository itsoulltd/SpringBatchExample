package com.infoworks.lab.webapp.config;

import com.infoworks.lab.jsql.ExecutorType;
import com.infoworks.lab.jsql.JsqlConfig;
import com.it.soul.lab.connect.io.ScriptRunner;
import com.it.soul.lab.sql.QueryExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class StartupConfig implements CommandLineRunner {

    private JsqlConfig jsqlConfig;
    private String dbKey;

    public StartupConfig(@Autowired JsqlConfig jsqlConfig
            ,@Autowired @Qualifier("AppDBNameKey") String dbKey) {
        this.jsqlConfig = jsqlConfig;
        this.dbKey = dbKey;
    }

    @Override
    public void run(String... args) throws Exception {

        try(QueryExecutor executor = jsqlConfig.create(ExecutorType.SQL, dbKey)) {
            ScriptRunner runner = new ScriptRunner();
            File file = new File("person_insert_dump.sql");
            String[] cmds = runner.commands(runner.createStream(file));
            runner.execute(cmds, jsqlConfig.pullConnection(dbKey));
        }

        System.out.println("Startup Done");
    }
}

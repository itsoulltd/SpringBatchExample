package com.infoworks.lab.controllers.batch.tasks;

import com.infoworks.lab.domain.entities.Passenger;
import com.it.soul.lab.sql.SQLExecutor;
import com.it.soul.lab.sql.query.models.Table;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PassengerRowMapper implements RowMapper<List<Passenger>> {

    private SQLExecutor executor;

    public PassengerRowMapper(SQLExecutor executor) {
        this.executor = executor;
    }

    @Override
    public List<Passenger> mapRow(ResultSet resultSet, int i) throws SQLException {
        if (executor != null){
            Table table = executor.collection(resultSet);
            try {
                List<Passenger> items = table.inflate(Passenger.class);
                return  items;
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return new ArrayList<>();
    }
}

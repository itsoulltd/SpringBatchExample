package com.infoworks.lab.controllers.batch.passenger;

import com.infoworks.lab.domain.entities.Passenger;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PassengerMapper implements RowMapper<Passenger> {
    @Override
    public Passenger mapRow(ResultSet resultSet, int i) throws SQLException {
        Passenger passenger = new Passenger();
        passenger.setId(resultSet.getInt("id"));
        passenger.setName(resultSet.getString("name"));
        passenger.setActive(resultSet.getBoolean("active"));
        passenger.setAge(resultSet.getInt("age"));
        passenger.setSex(resultSet.getString("sex"));
        passenger.setDob(resultSet.getDate("dob"));
        return passenger;
    }
}

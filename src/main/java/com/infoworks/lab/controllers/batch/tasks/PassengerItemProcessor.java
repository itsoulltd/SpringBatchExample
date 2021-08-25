package com.infoworks.lab.controllers.batch.tasks;

import com.infoworks.lab.domain.entities.Passenger;
import org.springframework.batch.item.ItemProcessor;

import java.util.List;

public class PassengerItemProcessor implements ItemProcessor<List<Passenger>, List<Passenger>> {

    @Override
    public List<Passenger> process(List<Passenger> passengers) throws Exception {
        System.out.println("ItemProcessor");
        System.out.println("Processing: " + passengers.size());
        return passengers;
    }
}

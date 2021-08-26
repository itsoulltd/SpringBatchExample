package com.infoworks.lab.controllers.batch.steps;

import com.infoworks.lab.domain.entities.Passenger;
import org.springframework.batch.item.ItemProcessor;

public class PassengerProcessor implements ItemProcessor<Passenger, Passenger> {
    @Override
    public Passenger process(Passenger passenger) throws Exception {
        //System.out.println("PassengerProcessor: " + Thread.currentThread().getName());
        //System.out.println("Processing: " + passenger.getName());
        return passenger;
    }
}

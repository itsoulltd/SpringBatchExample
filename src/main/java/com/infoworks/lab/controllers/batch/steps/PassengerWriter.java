package com.infoworks.lab.controllers.batch.steps;

import com.infoworks.lab.domain.entities.Passenger;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

public class PassengerWriter implements ItemWriter<Passenger> {
    @Override
    public void write(List<? extends Passenger> list) throws Exception {
        System.out.println("PassengerWriter: " + Thread.currentThread().getName());
        System.out.println("Chunk Size: " + list.size());
        list.forEach(passenger -> System.out.println(passenger.getName()));
    }
}

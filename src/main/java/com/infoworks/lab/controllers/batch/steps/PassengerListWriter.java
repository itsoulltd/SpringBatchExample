package com.infoworks.lab.controllers.batch.steps;

import com.infoworks.lab.domain.entities.Passenger;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

public class PassengerListWriter implements ItemWriter<List<Passenger>> {
    @Override
    public void write(List<? extends List<Passenger>> list) throws Exception {
        if (list.size() < 0) return;
        System.out.println("ConsoleItemWriter: ");
        List<Passenger> writeList = list.get(0);
        writeList.forEach(passenger -> System.out.println(passenger.getName()));
    }
}

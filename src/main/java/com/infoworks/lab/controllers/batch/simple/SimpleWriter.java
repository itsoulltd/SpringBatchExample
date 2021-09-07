package com.infoworks.lab.controllers.batch.simple;

import com.infoworks.lab.rest.models.Message;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

public class SimpleWriter implements ItemWriter<Message> {
    @Override
    public void write(List<? extends Message> list) throws Exception {
        System.out.println("SimpleWriter");
        list.forEach(msg -> System.out.println(msg.getPayload()));
    }
}

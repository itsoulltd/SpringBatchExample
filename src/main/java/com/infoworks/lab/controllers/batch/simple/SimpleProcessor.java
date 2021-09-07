package com.infoworks.lab.controllers.batch.simple;

import com.infoworks.lab.rest.models.Message;
import org.springframework.batch.item.ItemProcessor;

public class SimpleProcessor implements ItemProcessor<Message, Message> {
    @Override
    public Message process(Message o) throws Exception {
        System.out.println("SimpleProcessor");
        //o.setPayload("{\"message\":\"hello there! processed!\"}");
        return o;
    }
}

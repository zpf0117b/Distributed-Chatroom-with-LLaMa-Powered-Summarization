// ----------------------------- CHAT SUMMARY CODE WORKING ------------------------------------------------

package com.shubh.kafkachat.controller;

import com.shubh.kafkachat.constants.KafkaConstants;
import com.shubh.kafkachat.model.Message;
import com.shubh.kafkachat.model.GptSummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.kafka.core.ProducerFactory;

@RestController
public class ChatController {
    
    @Autowired
    private KafkaTemplate<String, Message> kafkaTemplate;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping(value = "/api/message", consumes = "application/json", produces = "application/json")
    public void sendMessage(@RequestBody Message message) {
        try {
            //Sending the message to kafka topic queue
            //System.out.println("sending to kafka producer..");
            //System.out.println(message.toString());
            kafkaTemplate.send(message.getTopic(), message).get();
            //KafkaClientMetrics consumerKafkaMetrics = new KafkaClientMetrics();
            //consumerKafkaMetrics.bindTo(registry);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping(value = "/api/message", produces = "application/json")
    public Map<String, Object> getMessages(@RequestParam int offset, @RequestParam String topic ) {

        //KafkaClientMetrics producerKafkaMetrics = new KafkaClientMetrics(producer);
        //producerKafkaMetrics.bindTo(registry);
        //redisTemplate.discard();
        System.out.println("Topic Id: ");
        System.out.println(topic);
        ArrayList<Message> messages = new ArrayList<Message>();
        int lastSeqNum = 0;
        // String old_messages = ''; // optinal
        GptSummaryService summaryService = new GptSummaryService();
        if(redisTemplate.opsForValue().get(topic + "_last_seq_num") != null){
            lastSeqNum = Integer.parseInt((String)redisTemplate.opsForValue().get(topic + "_last_seq_num"));
            System.out.println(lastSeqNum);
        }

        String usrlist = "";
        String msglist = "";
        int count = lastSeqNum-offset-1;
        // for(int i=lastSeqNum-offset;i>0&&i>lastSeqNum-offset-10;i--){
        for(int i=lastSeqNum-offset;i>0;i--){
            try {
                if(redisTemplate.opsForValue().get(topic+"_seq_" + Integer.toString(i)) != null){
                    Message message = objectMapper.readValue((redisTemplate.opsForValue().get(topic+"_seq_" + Integer.toString(i))).toString(), Message.class);
                    messages.add(message);
                    if(count == 0){
                        Message summary = new Message("GPT SUMMARY", "This is the GPT summary:", message.getTopic(), message.getSeqNumber(), message.getTimestamp()+1);
                        // summary.setContent(summaryService.generateSummary(old_messages));
                        usrlist += message.getSender();
                        msglist += message.getContent();
                        summary.setContent("");
                        summary.setContent(GptSummaryService.runScript(usrlist, msglist));
                        System.out.println("Set the summary for:"+summary.getContent());
                        messages.add(0,summary);
                        usrlist = "";
                        msglist = "";
                        count = 100;
                    }
                    else{
                        count -= 1;
                        usrlist += message.getSender() + "USRSEP";
                        msglist += message.getContent() + "MSGSEP";
                    }
                    // old_messages += message.getContent();
                }
                else break;
            } catch (JsonMappingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        Map<String, Object> response = new HashMap<>();
        response.put("messages", messages);
        System.out.println("sending old messages to the client");
        System.out.println(messages);
        return response;
    }

    //    -------------- WebSocket API ----------------
    @MessageMapping("/sendMessage")
    @SendTo("/topic/group")
    public Message broadcastGroupMessage(@Payload Message message) {
        //Sending this message to all the subscribers
        return message;
    }

    @MessageMapping("/newUser")
    @SendTo("/topic/group")
    public Message addUser(@Payload Message message,
                           SimpMessageHeaderAccessor headerAccessor) {
        // Add user in web socket session
        headerAccessor.getSessionAttributes().put("username", message.getSender());
        message.setContent(message.getSender()+" joined the chat");
        return message;
    }

}

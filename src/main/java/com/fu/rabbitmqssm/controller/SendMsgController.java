package com.fu.rabbitmqssm.controller;

import com.fu.rabbitmqssm.config.ConfirmConfig;
import com.fu.rabbitmqssm.config.DelayedQueueConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Date;
import java.util.UUID;

@RestController
public class SendMsgController {
    private static final Logger log = LoggerFactory.getLogger(SendMsgController.class);

    @Resource
    private RabbitTemplate rabbitTemplate;

    //开始发消息
    @GetMapping("/sendMsg/{message}")
    public void sendMsg(@PathVariable String message){
        log.info("当前时间：{},发送一条消息给2个TTL队列：{}",new Date().toString(),message);
        rabbitTemplate.convertAndSend("X","XA","消息来自ttl为10S的队列："+message);
        rabbitTemplate.convertAndSend("X","XB","消息来自ttl为40S的队列："+message);
    }

    //开始发消息 消息  ttl （延迟队列会有问题，前面时间长，后面时间短的消息会出现，时间短的要等待时间长的执行完才执行时间短的）
    @GetMapping("/sendTtlMsg/{message}/{ttlTime}")
    public void sendTtlMsg(@PathVariable String message,@PathVariable String ttlTime){
        log.info("当前时间：{},发送一条时长{}ms TTL消息给队列QC：{}",new Date().toString(),ttlTime,message);
        rabbitTemplate.convertAndSend("X","XC",message,msg->{
            //发送消息的时候   延迟时长
            msg.getMessageProperties().setExpiration(ttlTime);
            return msg;
        });
    }

    //基于插件的消息及延迟的时间 解决：时间短的要等待时间长的执行完才执行时间短的问题
    @GetMapping("/sendDelayedMsg/{message}/{delayTime}")
    public void sendDelayedMsg(@PathVariable String message,@PathVariable Integer delayTime){
        log.info("当前时间：{},发送一条时长{}ms 消息给延迟队列delayed.queue：{}",new Date().toString(),delayTime,message);
        rabbitTemplate.convertAndSend(DelayedQueueConfig.DELAYED_EXCHANGE_NAME,DelayedQueueConfig.DELAYED_ROUTING_KEY,message, msg->{
            //发送消息的时候   延迟时长    单位ms
            msg.getMessageProperties().setDelay(delayTime);
            return msg;
        });
    }

    //发布订阅高级（确认消息是否成功发送）    测试确认
    @GetMapping("/sendConfirmMsg/{message}")
    public void sendConfirmMsg(@PathVariable String message){
        CorrelationData correlationData = new CorrelationData("1");
        rabbitTemplate.convertAndSend(ConfirmConfig.CONFIRM_EXCHANGE_NAME,ConfirmConfig.CONFIRM_ROUTING_KEY,message+"A",correlationData);
        log.info("发送一条消息：{}",message+"A");

        CorrelationData correlationData2 = new CorrelationData("2");
        rabbitTemplate.convertAndSend(ConfirmConfig.CONFIRM_EXCHANGE_NAME,ConfirmConfig.CONFIRM_ROUTING_KEY+"2",message+"B",correlationData2);
        log.info("发送一条消息：{}",message+"B");
    }
}

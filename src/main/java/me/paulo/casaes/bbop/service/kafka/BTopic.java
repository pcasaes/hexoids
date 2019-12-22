package me.paulo.casaes.bbop.service.kafka;

import org.apache.kafka.clients.admin.NewTopic;

import java.util.function.Supplier;

public interface BTopic extends Supplier<NewTopic> {

}

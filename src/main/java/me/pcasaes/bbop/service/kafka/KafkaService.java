package me.pcasaes.bbop.service.kafka;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class KafkaService {

    private boolean okToConnect = false;

    void setOkToConnect(boolean okToConnect) {
        this.okToConnect = okToConnect;
    }

    public boolean hasStarted() {
        return okToConnect;
    }
}

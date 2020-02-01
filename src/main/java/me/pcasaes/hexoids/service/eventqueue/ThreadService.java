package me.pcasaes.hexoids.service.eventqueue;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
class ThreadService {

    private long gameLoopThread;


    void setGameLoopThread() {
        this.gameLoopThread = Thread.currentThread().getId();
    }

    boolean isInGameLoop() {
        return Thread.currentThread().getId() == gameLoopThread;
    }

}

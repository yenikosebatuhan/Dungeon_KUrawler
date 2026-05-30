package com.kurawler.engine;

import com.kurawler.model.TeamMatchSession;

/** Stub — team match sistemi başka branch'te geliştiriliyor. */
public class TeamMatchGameLoop {

    private final TeamMatchSession session;
    private boolean running;

    public TeamMatchGameLoop(TeamMatchSession session) {
        this.session = session;
    }

    public void start()  { running = true; }
    public void stop()   { running = false; }
    public boolean isRunning() { return running; }
    public void update(long deltaMs) {}
}

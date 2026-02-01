package ing.boykiss.gearworks.server.engine;

public class GameEngine {
    private final Thread engineThread;

    private boolean isStarted = false;

    public GameEngine() {
        engineThread = Thread.ofVirtual().unstarted(this::initThread);
    }

    public void start() {
        if (!isStarted) isStarted = true;
        engineThread.start();
    }

    private void initThread() {
    }
}

package ing.boykiss.gearworks.server;

import ing.boykiss.gearworks.common.Greeting;
import ing.boykiss.gearworks.server.engine.GameEngine;

public class ServerMain {
    private static GameEngine gameEngine;

    static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(() -> System.out.println(Greeting.getGoodbye())));

        System.out.println(Greeting.getGreeting());

        gameEngine = new GameEngine();
        gameEngine.start();

        while (true) {
            // NOOP to prevent shutting down this thread, might not be the proper way to handle this but whatever
        }
    }
}

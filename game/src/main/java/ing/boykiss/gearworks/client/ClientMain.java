package ing.boykiss.gearworks.client;

import ing.boykiss.gearworks.client.render.game.GameRenderer;
import ing.boykiss.gearworks.common.Greeting;

public class ClientMain {
    private static GameRenderer gameRenderer;

    static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(() -> System.out.println(Greeting.getGoodbye())));

        System.out.println(Greeting.getGreeting());

        gameRenderer = new GameRenderer();
        gameRenderer.start();

        while (true) {
            // NOOP to prevent shutting down this thread, might not be the proper way to handle this but whatever
        }
    }
}
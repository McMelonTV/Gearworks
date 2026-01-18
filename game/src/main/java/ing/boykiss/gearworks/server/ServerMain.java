package ing.boykiss.gearworks.server;

import ing.boykiss.gearworks.common.Greeting;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerMain {
    private static final int TPS = 120;
    private static final long PERIOD = 1000 / TPS;
    private static final int CHUNKS_PER_AREA = 512;
    private static final List<Chunk> chunks = new ArrayList<>();
    private static final List<ChunkArea> chunkAreas = new ArrayList<>();

    static void main(String[] args) {
        System.out.println(Greeting.getGreeting());

        for (int i = 0; i < 5000000; i++) {
            chunks.add(new Chunk(i));
        }

        partitionChunksIntoAreas();
        System.out.println("Created " + chunkAreas.size() + " chunk areas");

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

        scheduler.scheduleAtFixedRate(() -> tickAllChunkAreas(virtualThreadExecutor), 0, PERIOD, TimeUnit.MILLISECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down gracefully...");
            scheduler.shutdown();
            virtualThreadExecutor.close();
        }));

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static void partitionChunksIntoAreas() {
        for (int i = 0; i < chunks.size(); i += CHUNKS_PER_AREA) {
            int endIndex = Math.min(i + CHUNKS_PER_AREA, chunks.size());
            List<Chunk> areaChunks = chunks.subList(i, endIndex);
            chunkAreas.add(new ChunkArea(i / CHUNKS_PER_AREA, areaChunks));
        }
    }

    private static void tickAllChunkAreas(ExecutorService virtualThreadExecutor) {
        for (ChunkArea area : chunkAreas) {
            virtualThreadExecutor.submit(area::tick);
        }
    }

    static class ChunkArea {
        private final int areaId;
        private final List<Chunk> chunks;
        private int tickCount = 0;

        public ChunkArea(int areaId, List<Chunk> chunks) {
            this.areaId = areaId;
            this.chunks = chunks;
        }

        public void tick() {
            tickCount++;

            for (Chunk chunk : chunks) {
                chunk.tick();
            }

//            if (tickCount % TPS == 0) {
//                System.out.println("ChunkArea " + areaId + " (containing " + chunks.size() +
//                        " chunks) completed tick " + tickCount +
//                        " on thread: " + Thread.currentThread());
//            }
        }
    }

    static class Chunk {
        private final int id;
        private int tickCount = 0;

        public Chunk(int id) {
            this.id = id;
        }

        public void tick() {
            tickCount++;
        }
    }
}

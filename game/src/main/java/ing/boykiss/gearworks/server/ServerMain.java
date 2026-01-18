package ing.boykiss.gearworks.server;

import ing.boykiss.gearworks.common.Greeting;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerMain {
    private static final int TPS = 60;
    private static final long PERIOD = 1000 / TPS;
    private static final int AREA_SIZE_READJUST_PERIOD = 5000;
    private static final int CHUNK_COUNT = 10000;
    private static final int INITIAL_AREA_SIZE = 100;
    private static final List<Chunk> chunks = new ArrayList<>();
    private static final List<ChunkArea> chunkAreas = new ArrayList<>();
    private static final AdaptiveChunkManager adaptiveManager = new AdaptiveChunkManager();

    static void main(String[] args) {
        System.out.println(Greeting.getGreeting());

        for (int i = 0; i < CHUNK_COUNT; i++) {
            chunks.add(new Chunk(i));
        }

        partitionChunksIntoAreas(INITIAL_AREA_SIZE);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

        scheduler.scheduleAtFixedRate(() -> tickAllChunkAreas(virtualThreadExecutor), 0, PERIOD, TimeUnit.MILLISECONDS);

        scheduler.scheduleAtFixedRate(() -> adjustChunkAreaSizes(), AREA_SIZE_READJUST_PERIOD, AREA_SIZE_READJUST_PERIOD, TimeUnit.MILLISECONDS);

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

    private static void partitionChunksIntoAreas(int chunksPerArea) {
        chunkAreas.clear();
        for (int i = 0; i < chunks.size(); i += chunksPerArea) {
            int endIndex = Math.min(i + chunksPerArea, chunks.size());
            List<Chunk> areaChunks = chunks.subList(i, endIndex);
            chunkAreas.add(new ChunkArea(i / chunksPerArea, areaChunks));
        }
        System.out.println("Repartitioned into " + chunkAreas.size() + " areas with ~" + chunksPerArea + " chunks each");
    }

    private static void tickAllChunkAreas(ExecutorService virtualThreadExecutor) {
        for (ChunkArea area : chunkAreas) {
            virtualThreadExecutor.submit(() -> {
                long start = System.nanoTime();
                area.tick();
                long duration = System.nanoTime() - start;
                adaptiveManager.recordTickTime(duration);
            });
        }
    }

    private static void adjustChunkAreaSizes() {
        int newSize = adaptiveManager.calculateOptimalChunkSize(chunkAreas.get(0).chunks.size());
        if (newSize != chunkAreas.get(0).chunks.size()) {
            partitionChunksIntoAreas(newSize);
        }
    }

    static class AdaptiveChunkManager {
        // Target: each area should complete in < 50% of tick period
        private static final long TARGET_AREA_TIME_NS = (PERIOD * 1_000_000) / 2; // 50% of tick period
        private static final int MIN_CHUNKS_PER_AREA = 10;
        private static final int MAX_CHUNKS_PER_AREA = 1000;
        private static final int SAMPLE_SIZE = 100;

        private final List<Long> recentTickTimes = new ArrayList<>();
        private int currentAreaSize = 100;

        public synchronized void recordTickTime(long nanos) {
            recentTickTimes.add(nanos);
            if (recentTickTimes.size() > SAMPLE_SIZE) {
                recentTickTimes.remove(0);
            }
        }

        public synchronized int calculateOptimalChunkSize(int currentSize) {
            if (recentTickTimes.size() < 20) {
                return currentSize;
            }

            long avgTickTime = (long) recentTickTimes.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(TARGET_AREA_TIME_NS);

            List<Long> sorted = new ArrayList<>(recentTickTimes);
            sorted.sort(Long::compareTo);
            long p95 = sorted.get((int) (sorted.size() * 0.95));

            System.out.println("Avg tick time: " + avgTickTime / 1_000_000.0 + "ms, " +
                    "P95: " + p95 / 1_000_000.0 + "ms, " +
                    "Target: " + TARGET_AREA_TIME_NS / 1_000_000.0 + "ms");

            int newSize = currentSize;

            if (p95 > TARGET_AREA_TIME_NS * 1.2) {
                newSize = (int) (currentSize * 0.8);
                System.out.println("Areas too slow, reducing chunk size");
            } else if (avgTickTime < TARGET_AREA_TIME_NS * 0.3) {
                newSize = (int) (currentSize * 1.2);
                System.out.println("Areas underutilized, increasing chunk size");
            }

            newSize = Math.max(MIN_CHUNKS_PER_AREA, Math.min(MAX_CHUNKS_PER_AREA, newSize));

            if (newSize != currentSize) {
                currentAreaSize = newSize;
                recentTickTimes.clear();
            }

            return newSize;
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

            // simulate workload
            if (id % 100 == 0) {
                try {
                    Thread.sleep(0, 10000); // 10μs
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}

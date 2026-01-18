package ing.boykiss.gearworks.server;

import ing.boykiss.gearworks.common.Greeting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ServerMain {
    private static final int TPS = 60;
    private static final long PERIOD = 1000 / TPS;
    private static final int AREA_SIZE_READJUST_PERIOD = 1000;
    private static final int CHUNK_COUNT = 100000;
    private static final int INITIAL_AREA_SIZE = 100;
    private static final List<Chunk> chunks = new ArrayList<>();
    private static final Map<Integer, Chunk> chunkMap = new ConcurrentHashMap<>();
    private static final List<ChunkArea> chunkAreas = new CopyOnWriteArrayList<>();
    private static final AdaptiveChunkManager adaptiveManager = new AdaptiveChunkManager();
    private static final ConcurrentLinkedQueue<CrossChunkMessage> crossChunkMessages = new ConcurrentLinkedQueue<>();
    private static final PerformanceMetrics metrics = new PerformanceMetrics();

    static void main(String[] args) {
        System.out.println(Greeting.getGreeting());

        Random random = new Random();
        for (int i = 0; i < CHUNK_COUNT; i++) {
            Chunk chunk = new Chunk(i, random);
            chunks.add(chunk);
            chunkMap.put(i, chunk);
        }

        partitionChunksIntoAreas(INITIAL_AREA_SIZE);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        ExecutorService workStealingExecutor = Executors.newWorkStealingPool();

        scheduler.scheduleAtFixedRate(() -> {
            long tickStart = System.nanoTime();
            tickAllChunkAreas(workStealingExecutor);
            processCrossChunkMessages();
            metrics.recordFullTickTime(System.nanoTime() - tickStart);
        }, 0, PERIOD, TimeUnit.MILLISECONDS);

        scheduler.scheduleAtFixedRate(() -> {
            adjustChunkAreaSizes();
            optimizeChunkSleeping();
        }, AREA_SIZE_READJUST_PERIOD, AREA_SIZE_READJUST_PERIOD, TimeUnit.MILLISECONDS);

        scheduler.scheduleAtFixedRate(metrics::printReport, 10000, 10000, TimeUnit.MILLISECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down gracefully...");
            scheduler.shutdown();
            workStealingExecutor.close();
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

        List<Chunk> activeChunks = chunks.stream()
                .filter(c -> !c.isSleeping())
                .toList();

        List<Chunk> sleepingChunks = chunks.stream()
                .filter(Chunk::isSleeping)
                .toList();

        for (int i = 0; i < activeChunks.size(); i += chunksPerArea) {
            int endIndex = Math.min(i + chunksPerArea, activeChunks.size());
            List<Chunk> areaChunks = new ArrayList<>(activeChunks.subList(i, endIndex));
            chunkAreas.add(new ChunkArea(i / chunksPerArea, areaChunks));
        }

        if (!sleepingChunks.isEmpty()) {
            int sleepingAreaSize = Math.max(chunksPerArea * 5, 500);
            for (int i = 0; i < sleepingChunks.size(); i += sleepingAreaSize) {
                int endIndex = Math.min(i + sleepingAreaSize, sleepingChunks.size());
                List<Chunk> areaChunks = new ArrayList<>(sleepingChunks.subList(i, endIndex));
                chunkAreas.add(new ChunkArea(10000 + i / sleepingAreaSize, areaChunks));
            }
        }

        System.out.println("Repartitioned: " +
                chunkAreas.stream().filter(a -> !a.isSleeping()).count() + " active areas, " +
                chunkAreas.stream().filter(ChunkArea::isSleeping).count() + " sleeping areas, " +
                "~" + chunksPerArea + " chunks/area");
    }

    private static void tickAllChunkAreas(ExecutorService executor) {
        List<CompletableFuture<Long>> futures = new ArrayList<>();

        for (ChunkArea area : chunkAreas) {
            CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                long start = System.nanoTime();
                area.tick();
                long duration = System.nanoTime() - start;
                adaptiveManager.recordTickTime(duration, area.chunks.size());
                return duration;
            }, executor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        for (CompletableFuture<Long> future : futures) {
            try {
                long duration = future.get();
                metrics.recordAreaTickTime(duration);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void processCrossChunkMessages() {
        CrossChunkMessage message;
        int processed = 0;
        while ((message = crossChunkMessages.poll()) != null) {
            Chunk targetChunk = chunkMap.get(message.targetChunkId);
            if (targetChunk != null) {
                message.apply(targetChunk);
            }
            processed++;
        }
        metrics.recordCrossChunkMessages(processed);
    }

    private static void optimizeChunkSleeping() {
        for (Chunk chunk : chunks) {
            chunk.updateSleepState();
        }
    }

    private static void adjustChunkAreaSizes() {
        int currentSize = chunkAreas.isEmpty() ? INITIAL_AREA_SIZE :
                chunkAreas.stream()
                        .filter(a -> !a.isSleeping())
                        .mapToInt(a -> a.chunks.size())
                        .findFirst()
                        .orElse(INITIAL_AREA_SIZE);

        int newSize = adaptiveManager.calculateOptimalChunkSize(currentSize);
        if (newSize != currentSize) {
            partitionChunksIntoAreas(newSize);
        }
    }

    enum ChunkDensity {
        EMPTY(0, 0, 0), LIGHT(5, 10, 4), MEDIUM(20, 50, 16), HEAVY(80, 200, 64);
        final int entityCount, storageSlots, gridSize;

        ChunkDensity(int e, int s, int g) {
            entityCount = e;
            storageSlots = s;
            gridSize = g;
        }
    }

    enum Direction {NORTH, SOUTH, EAST, WEST}

    static class AdaptiveChunkManager {
        private static final long TARGET_AREA_TIME_NS = (PERIOD * 1_000_000) / 2;
        private static final int MIN_CHUNKS_PER_AREA = 5;
        private static final int MAX_CHUNKS_PER_AREA = 500;
        private static final int SAMPLE_SIZE = 50;
        private static final double SCALE_UP_FACTOR = 1.15;
        private static final double SCALE_DOWN_FACTOR = 0.85;

        private final List<TickSample> recentSamples = new ArrayList<>();
        private int consecutiveSlowTicks = 0;
        private int consecutiveFastTicks = 0;

        public synchronized void recordTickTime(long nanos, int chunkCount) {
            recentSamples.add(new TickSample(nanos, chunkCount));
            if (recentSamples.size() > SAMPLE_SIZE) {
                recentSamples.remove(0);
            }
        }

        public synchronized int calculateOptimalChunkSize(int currentSize) {
            if (recentSamples.size() < 10) {
                return currentSize;
            }

            double weightedAvg = 0;
            double totalWeight = 0;
            for (int i = 0; i < recentSamples.size(); i++) {
                double weight = (i + 1);
                weightedAvg += recentSamples.get(i).duration * weight;
                totalWeight += weight;
            }
            weightedAvg /= totalWeight;

            List<Long> durations = recentSamples.stream()
                    .map(s -> s.duration)
                    .sorted()
                    .toList();
            long p95 = durations.get((int) (durations.size() * 0.95));
            long p50 = durations.get(durations.size() / 2);

            System.out.printf("Weighted avg: %.2fms, P50: %.2fms, P95: %.2fms, Target: %.2fms%n",
                    weightedAvg / 1_000_000.0,
                    p50 / 1_000_000.0,
                    p95 / 1_000_000.0,
                    TARGET_AREA_TIME_NS / 1_000_000.0);

            int newSize = currentSize;

            if (p50 > TARGET_AREA_TIME_NS * 1.1) {
                consecutiveSlowTicks++;
                consecutiveFastTicks = 0;

                if (consecutiveSlowTicks >= 2) {
                    newSize = (int) (currentSize * SCALE_DOWN_FACTOR);
                    System.out.println("[SLOW] Consistently slow, aggressively reducing chunk size");
                    consecutiveSlowTicks = 0;
                }
            } else if (p95 < TARGET_AREA_TIME_NS * 0.4) {
                consecutiveFastTicks++;
                consecutiveSlowTicks = 0;

                if (consecutiveFastTicks >= 3) {
                    newSize = (int) (currentSize * SCALE_UP_FACTOR);
                    System.out.println("[OK] Underutilized, increasing chunk size");
                    consecutiveFastTicks = 0;
                }
            } else {
                consecutiveSlowTicks = 0;
                consecutiveFastTicks = 0;
            }

            double finalWeightedAvg = weightedAvg;
            double variance = recentSamples.stream()
                    .mapToDouble(s -> Math.pow(s.duration - finalWeightedAvg, 2))
                    .average()
                    .orElse(0);
            double stdDev = Math.sqrt(variance);

            if (stdDev > weightedAvg * 0.5) {
                newSize = (int) (newSize * 0.95);
                System.out.println("[VAR] High variance detected, reducing for stability");
            }

            newSize = Math.max(MIN_CHUNKS_PER_AREA, Math.min(MAX_CHUNKS_PER_AREA, newSize));

            if (newSize != currentSize) {
                recentSamples.clear();
                consecutiveSlowTicks = 0;
                consecutiveFastTicks = 0;
            }

            return newSize;
        }

        static class TickSample {
            long duration;
            int chunkCount;

            TickSample(long duration, int chunkCount) {
                this.duration = duration;
                this.chunkCount = chunkCount;
            }

            double timePerChunk() {
                return chunkCount > 0 ? (double) duration / chunkCount : 0;
            }
        }
    }

    static class PerformanceMetrics {
        private final AtomicLong totalTicks = new AtomicLong(0);
        private final AtomicLong totalCrossChunkMessages = new AtomicLong(0);
        private final List<Long> recentFullTickTimes = new CopyOnWriteArrayList<>();
        private final List<Long> recentAreaTickTimes = new CopyOnWriteArrayList<>();

        public void recordFullTickTime(long nanos) {
            totalTicks.incrementAndGet();
            recentFullTickTimes.add(nanos);
            if (recentFullTickTimes.size() > 100) {
                recentFullTickTimes.remove(0);
            }
        }

        public void recordAreaTickTime(long nanos) {
            recentAreaTickTimes.add(nanos);
            if (recentAreaTickTimes.size() > 500) {
                recentAreaTickTimes.remove(0);
            }
        }

        public void recordCrossChunkMessages(int count) {
            totalCrossChunkMessages.addAndGet(count);
        }

        public void printReport() {
            if (recentFullTickTimes.isEmpty()) return;

            double avgTickTime = recentFullTickTimes.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0) / 1_000_000.0;

            long maxTickTime = recentFullTickTimes.stream()
                    .mapToLong(Long::longValue)
                    .max()
                    .orElse(0) / 1_000_000;

            long activeChunks = chunks.stream().filter(c -> !c.isSleeping()).count();
            long sleepingChunks = chunks.size() - activeChunks;

            System.out.println("\n═══════════════ PERFORMANCE REPORT ═══════════════");
            System.out.printf("Total ticks: %d | Avg tick time: %.2fms | Max: %dms%n",
                    totalTicks.get(), avgTickTime, maxTickTime);
            System.out.printf("Active chunks: %d | Sleeping: %d | Efficiency: %.1f%%%n",
                    activeChunks, sleepingChunks,
                    (double) sleepingChunks / chunks.size() * 100);
            System.out.printf("Cross-chunk messages/sec: %.1f%n",
                    totalCrossChunkMessages.get() / (totalTicks.get() / (double) TPS));
            System.out.printf("Target TPS: %d | Budget per tick: %.2fms%n",
                    TPS, PERIOD);
            System.out.println("═══════════════════════════════════════════════════\n");
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

        public boolean isSleeping() {
            return chunks.stream().allMatch(Chunk::isSleeping);
        }
    }

    static class Chunk {
        private final int id;
        private final ChunkDensity density;
        private final List<Entity> entities;
        private final ConcurrentHashMap<Integer, Integer> storage;
        private final double[] powerGrid;
        private final ConcurrentLinkedQueue<Entity> incomingEntities = new ConcurrentLinkedQueue<>();
        private final ConcurrentHashMap<Integer, Integer> incomingResources = new ConcurrentHashMap<>();
        private double storedPower = 0;
        private int tickCount = 0;
        private int inactiveTickCount = 0;
        private boolean sleeping = false;

        public Chunk(int id, Random random) {
            this.id = id;
            double densityRoll = random.nextDouble();
            if (densityRoll < 0.70) {
                this.density = ChunkDensity.EMPTY;
            } else if (densityRoll < 0.90) {
                this.density = ChunkDensity.LIGHT;
            } else if (densityRoll < 0.97) {
                this.density = ChunkDensity.MEDIUM;
            } else {
                this.density = ChunkDensity.HEAVY;
            }

            this.entities = new ArrayList<>(density.entityCount);
            this.storage = new ConcurrentHashMap<>();
            this.powerGrid = new double[density.gridSize];
            initializeChunk(random);
        }

        private void initializeChunk(Random random) {
            for (int i = 0; i < density.entityCount; i++) {
                entities.add(new Entity(random));
            }
            for (int i = 0; i < density.storageSlots; i++) {
                storage.put(i, random.nextInt(1000));
            }
        }

        public void tick() {
            tickCount++;

            if (!incomingEntities.isEmpty() || !incomingResources.isEmpty()) {
                sleeping = false;
                inactiveTickCount = 0;
            }

            if (sleeping && density == ChunkDensity.EMPTY) {
                return;
            }

            processIncomingTransfers();

            boolean hadActivity = false;
            switch (density) {
                case EMPTY:
                    hadActivity = tickEmpty();
                    break;
                case LIGHT:
                    hadActivity = tickLight();
                    break;
                case MEDIUM:
                    hadActivity = tickMedium();
                    break;
                case HEAVY:
                    hadActivity = tickHeavy();
                    break;
            }

            checkCrossChunkTransfers();

            if (!hadActivity) {
                inactiveTickCount++;
            } else {
                inactiveTickCount = 0;
            }
        }

        public void updateSleepState() {
            if (inactiveTickCount > TPS * 5 &&
                    (density == ChunkDensity.EMPTY || density == ChunkDensity.LIGHT)) {
                sleeping = true;
            }
        }

        public boolean isSleeping() {
            return sleeping;
        }

        private void processIncomingTransfers() {
            Entity entity;
            while ((entity = incomingEntities.poll()) != null) {
                entities.add(entity);
            }

            for (Map.Entry<Integer, Integer> entry : incomingResources.entrySet()) {
                storage.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
            incomingResources.clear();
        }

        private void checkCrossChunkTransfers() {
            List<Entity> toRemove = new ArrayList<>();
            for (Entity entity : entities) {
                if (entity.x >= 16) {
                    entity.x -= 16;
                    int targetChunk = getAdjacentChunk(Direction.EAST);
                    crossChunkMessages.offer(new EntityTransferMessage(targetChunk, entity));
                    toRemove.add(entity);
                } else if (entity.x < 0) {
                    entity.x += 16;
                    int targetChunk = getAdjacentChunk(Direction.WEST);
                    crossChunkMessages.offer(new EntityTransferMessage(targetChunk, entity));
                    toRemove.add(entity);
                } else if (entity.y >= 16) {
                    entity.y -= 16;
                    int targetChunk = getAdjacentChunk(Direction.NORTH);
                    crossChunkMessages.offer(new EntityTransferMessage(targetChunk, entity));
                    toRemove.add(entity);
                } else if (entity.y < 0) {
                    entity.y += 16;
                    int targetChunk = getAdjacentChunk(Direction.SOUTH);
                    crossChunkMessages.offer(new EntityTransferMessage(targetChunk, entity));
                    toRemove.add(entity);
                }
            }
            entities.removeAll(toRemove);

            if (density != ChunkDensity.EMPTY && tickCount % 10 == 0) {
                for (int slot = 0; slot < Math.min(3, storage.size()); slot++) {
                    Integer amount = storage.get(slot);
                    if (amount != null && amount > 10) {
                        int transfer = Math.min(amount / 4, 5);
                        storage.put(slot, amount - transfer);
                        int targetChunk = getAdjacentChunk(Direction.EAST);
                        crossChunkMessages.offer(new ResourceTransferMessage(targetChunk, slot, transfer));
                    }
                }
            }

            if (density == ChunkDensity.HEAVY && storedPower > 50) {
                double powerToShare = storedPower * 0.1;
                storedPower -= powerToShare;
                int targetChunk = getAdjacentChunk(Direction.EAST);
                crossChunkMessages.offer(new PowerTransferMessage(targetChunk, powerToShare));
            }
        }

        private int getAdjacentChunk(Direction direction) {
            switch (direction) {
                case EAST:
                    return (id + 1) % CHUNK_COUNT;
                case WEST:
                    return (id - 1 + CHUNK_COUNT) % CHUNK_COUNT;
                case NORTH:
                    return (id + 100) % CHUNK_COUNT;
                case SOUTH:
                    return (id - 100 + CHUNK_COUNT) % CHUNK_COUNT;
                default:
                    return id;
            }
        }

        public void receiveEntity(Entity entity) {
            incomingEntities.offer(entity);
        }

        public void receiveResource(int resourceType, int amount) {
            incomingResources.merge(resourceType, amount, Integer::sum);
        }

        public void receivePower(double power) {
            storedPower += power;
        }

        private boolean tickEmpty() {
            return false;
        }

        private boolean tickLight() {
            for (Entity entity : entities) {
                entity.updatePosition();
            }
            return !entities.isEmpty();
        }

        private boolean tickMedium() {
            for (Entity entity : entities) {
                entity.updatePosition();
                entity.processState();
            }

            for (int i = 0; i < Math.min(5, storage.size()); i++) {
                Integer current = storage.get(i);
                if (current != null && current > 0) {
                    storage.put(i, current - 1);
                    storage.merge(i + 1, 1, Integer::sum);
                }
            }
            return !entities.isEmpty() || !storage.isEmpty();
        }

        private boolean tickHeavy() {
            for (Entity entity : entities) {
                entity.updatePosition();
                entity.processState();
                entity.calculateInteractions();
            }

            for (Map.Entry<Integer, Integer> entry : storage.entrySet()) {
                int slot = entry.getKey();
                int amount = entry.getValue();
                if (amount > 0) {
                    int transfer = Math.min(amount, 10);
                    storage.put(slot, amount - transfer);
                    storage.merge((slot + 1) % storage.size(), transfer, Integer::sum);
                }
            }

            for (int i = 0; i < powerGrid.length; i++) {
                powerGrid[i] = Math.sin(tickCount * 0.1 + i) * 100;
                if (i > 0) {
                    powerGrid[i] += powerGrid[i - 1] * 0.1;
                }
                storedPower += powerGrid[i] * 0.01;
            }

            for (int i = 0; i < entities.size(); i++) {
                Entity e1 = entities.get(i);
                for (int j = i + 1; j < Math.min(i + 10, entities.size()); j++) {
                    Entity e2 = entities.get(j);
                    double dist = Math.sqrt(
                            Math.pow(e1.x - e2.x, 2) +
                                    Math.pow(e1.y - e2.y, 2)
                    );
                    if (dist < 5.0) {
                        e1.interactionCount++;
                    }
                }
            }
            return true;
        }
    }

    static abstract class CrossChunkMessage {
        final int targetChunkId;

        CrossChunkMessage(int targetChunkId) {
            this.targetChunkId = targetChunkId;
        }

        abstract void apply(Chunk chunk);
    }

    static class EntityTransferMessage extends CrossChunkMessage {
        final Entity entity;

        EntityTransferMessage(int targetChunkId, Entity entity) {
            super(targetChunkId);
            this.entity = entity;
        }

        @Override
        void apply(Chunk chunk) {
            chunk.receiveEntity(entity);
        }
    }

    static class ResourceTransferMessage extends CrossChunkMessage {
        final int resourceType;
        final int amount;

        ResourceTransferMessage(int targetChunkId, int resourceType, int amount) {
            super(targetChunkId);
            this.resourceType = resourceType;
            this.amount = amount;
        }

        @Override
        void apply(Chunk chunk) {
            chunk.receiveResource(resourceType, amount);
        }
    }

    static class PowerTransferMessage extends CrossChunkMessage {
        final double power;

        PowerTransferMessage(int targetChunkId, double power) {
            super(targetChunkId);
            this.power = power;
        }

        @Override
        void apply(Chunk chunk) {
            chunk.receivePower(power);
        }
    }

    static class Entity {
        double x, y, velocityX, velocityY;
        int state, processCounter, interactionCount;

        public Entity(Random random) {
            this.x = random.nextDouble() * 16;
            this.y = random.nextDouble() * 16;
            this.velocityX = (random.nextDouble() - 0.5) * 0.2;
            this.velocityY = (random.nextDouble() - 0.5) * 0.2;
            this.state = random.nextInt(5);
        }

        public void updatePosition() {
            x += velocityX;
            y += velocityY;
        }

        public void processState() {
            processCounter++;
            if (processCounter > 20) {
                state = (state + 1) % 5;
                processCounter = 0;
            }
        }

        public void calculateInteractions() {
            double efficiency = Math.sin(x) * Math.cos(y) * state;
            velocityX *= 0.99 + efficiency * 0.01;
            velocityY *= 0.99 + efficiency * 0.01;
        }
    }
}

package net.swofty.user;

import lombok.Getter;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.*;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.TaskSchedule;
import net.swofty.SkyBlock;
import net.swofty.data.mongodb.IslandDatabase;
import org.bson.Document;

import java.util.concurrent.CompletableFuture;

public class SkyBlockIsland {
    private static String ISLAND_TEMPLATE_NAME = "hypixel_island_template";
    private IslandDatabase database;
    private Boolean created = false;
    private SharedInstance islandInstance;

    public SkyBlockIsland(SkyBlockPlayer player) {
        this.database = new IslandDatabase(player.getUuid().toString());
    }

    public CompletableFuture<SharedInstance> getShareInstance() {
        InstanceManager manager = MinecraftServer.getInstanceManager();
        CompletableFuture<SharedInstance> future = new CompletableFuture<>();

        new Thread(() -> {
            if (created) {
                future.complete(islandInstance);
                return;
            }

            if (!database.exists()) {
                InstanceContainer temporaryInstance = manager.createInstanceContainer();
                temporaryInstance.setChunkLoader(new AnvilLoader(ISLAND_TEMPLATE_NAME));

                islandInstance = manager.createSharedInstance(temporaryInstance);
                this.created = true;

                future.complete(islandInstance);
                return;
            }

            InstanceContainer instance = manager.createInstanceContainer();
        }).start();

        return future;
    }

    public void runVacantCheck() {
        if (islandInstance == null) return;

        if (islandInstance.getPlayers().isEmpty()) {
            save();
            this.created = false;
            islandInstance.getChunks().forEach(chunk -> {
                islandInstance.unloadChunk(chunk);
            });
            this.islandInstance = null;
        }
    }

    private void save() {
        Document document = new Document();
    }

    public static void runVacantLoop(Scheduler scheduler) {
        scheduler.submitTask(() -> {
            SkyBlock.getLoadedPlayers().forEach(player -> {
                player.getSkyBlockIsland().runVacantCheck();
            });
            return TaskSchedule.tick(10);
        }, ExecutionType.ASYNC);
    }
}

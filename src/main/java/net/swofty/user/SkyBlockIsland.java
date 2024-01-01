package net.swofty.user;

import lombok.Getter;
import net.hollowcube.polar.*;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.*;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.NamespaceID;
import net.swofty.SkyBlock;
import net.swofty.data.mongodb.CoopDatabase;
import net.swofty.data.mongodb.IslandDatabase;
import net.swofty.event.SkyBlockEvent;
import net.swofty.event.custom.IslandFetchedFromDatabaseEvent;
import net.swofty.event.custom.IslandFirstCreatedEvent;
import net.swofty.event.custom.IslandPlayerLoadedEvent;
import net.swofty.event.custom.IslandSavedIntoDatabaseEvent;
import org.bson.types.Binary;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SkyBlockIsland {
    private static final String ISLAND_TEMPLATE_NAME = "hypixel_island_template";
    private static final Map<UUID, SkyBlockIsland> loadedIslands = new HashMap<>();

    @Getter
    private IslandDatabase database = null;
    @Getter
    private final UUID islandID;
    @Getter
    private Boolean created = false;
    @Getter
    private CoopDatabase.Coop coop = null;
    @Getter
    private SharedInstance islandInstance;
    private PolarWorld world;

    public SkyBlockIsland(UUID islandID, UUID profileID) {
        this.islandID = islandID;
        this.database = new IslandDatabase(islandID.toString());
        this.coop = CoopDatabase.getFromMemberProfile(profileID);

        loadedIslands.put(islandID, this);
    }

    public CompletableFuture<SharedInstance> getSharedInstance() {
        InstanceManager manager = MinecraftServer.getInstanceManager();
        CompletableFuture<SharedInstance> future = new CompletableFuture<>();

        new Thread(() -> {
            if (created) {
                future.complete(islandInstance);
                return;
            }

            InstanceContainer temporaryInstance = manager.createInstanceContainer(MinecraftServer.getDimensionTypeManager().getDimension(
                    NamespaceID.from("skyblock:island")
            ));

            List<SkyBlockPlayer> onlinePlayers;
            if (coop != null) {
                onlinePlayers = coop.getOnlineMembers();
            } else {
                // Island ID will be the same as the profile ID if the island is not a coop
                try {
                    onlinePlayers = List.of(SkyBlock.getPlayerFromProfileUUID(islandID));
                } catch (NullPointerException e) {
                    // Player doesn't have their data loaded yet
                    onlinePlayers = List.of();
                }
            }

            if (!database.exists()) {
                try {
                    world = AnvilPolar.anvilToPolar(Path.of(ISLAND_TEMPLATE_NAME), ChunkSelector.radius(3));
                } catch (IOException e) {
                    // TODO: Proper error handling
                    throw new RuntimeException(e);
                }

                List<SkyBlockPlayer> finalOnlinePlayers = onlinePlayers;
                MinecraftServer.getSchedulerManager().scheduleTask(() -> {
                    SkyBlockEvent.callSkyBlockEvent(new IslandFirstCreatedEvent(
                            this, coop != null, finalOnlinePlayers, coop != null ? coop.memberProfiles() : List.of(islandID)
                    ));
                }, TaskSchedule.tick(1), TaskSchedule.stop());
            } else {
                world = PolarReader.read(((Binary) database.get("data", Binary.class)).getData());
            }

            islandInstance = manager.createSharedInstance(temporaryInstance);
            temporaryInstance.setChunkLoader(new PolarLoader(world));

            this.created = true;

            SkyBlockEvent.callSkyBlockEvent(new IslandFetchedFromDatabaseEvent(
                    this, coop != null, onlinePlayers, coop != null ? coop.memberProfiles() : List.of(islandID))
            );

            future.complete(islandInstance);
        }).start();

        return future;
    }

    public void runVacantCheck() {
        if (islandInstance == null) return;

        if (islandInstance.getPlayers().isEmpty()) {
            SkyBlockEvent.callSkyBlockEvent(new IslandSavedIntoDatabaseEvent(
                    this, coop != null, coop != null ? coop.memberProfiles() : List.of(islandID)
            ));

            save();
            this.created = false;
            islandInstance.getChunks().forEach(chunk -> {
                islandInstance.unloadChunk(chunk);
            });
            this.islandInstance = null;
            this.world = null;
        }
    }

    private void save() {
        new PolarLoader(world).saveInstance(islandInstance);
        database.insertOrUpdate("data", new Binary(PolarWriter.write(world)));
    }

    public static @Nullable SkyBlockIsland getIsland(UUID islandID) {
        if (!loadedIslands.containsKey(islandID)) return null;
        return loadedIslands.get(islandID);
    }

    public static void runVacantLoop(Scheduler scheduler) {
        scheduler.submitTask(() -> {
            SkyBlock.getLoadedPlayers().forEach(player -> {
                player.getSkyBlockIsland().runVacantCheck();
            });
            return TaskSchedule.tick(4);
        }, ExecutionType.ASYNC);
    }
}

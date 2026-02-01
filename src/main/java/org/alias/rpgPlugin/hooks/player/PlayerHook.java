package org.alias.rpgPlugin.hooks.player;

import com.github.luben.zstd.Zstd;
import com.google.gson.JsonObject;
import com.google.protobuf.Any;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.protocol.Transform;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.damage.DamageDataComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.data.PlayerConfigData;
import com.hypixel.hytale.server.core.entity.entities.player.data.PlayerWorldData;
import com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.collision.BlockData;
import com.hypixel.hytale.server.core.modules.entity.component.*;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import org.alias.rpgPlugin.Configuration;
import org.alias.rpgPlugin.RPGPluginEntry;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalField;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerHook extends Thread
{

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static RPGPluginEntry PLUGIN_INSTANCE;
    // Player component containers
    public static final ConcurrentHashMap<UUID, PlayerComponentContainer> playerComponentContainers = new ConcurrentHashMap<>();
    public static final List<byte[]> compressedPayloads = new ArrayList<>();

    // Packet metrics tracker
    private final PacketMetrics packetMetrics;

    // Metrics logging interval (in milliseconds)
    private static final long METRICS_LOG_INTERVAL = 60_000L; // Log every 60 seconds
    private long lastMetricsLogTime;

    public PlayerHook(RPGPluginEntry pluginEntry) {
        PLUGIN_INSTANCE = pluginEntry;
        this.packetMetrics = new PacketMetrics();
        this.lastMetricsLogTime = System.currentTimeMillis();
        start();
    }

    private com.google.gson.JsonObject stringMapToJson(Map<String, String> map) {
        com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
        if (map == null) return obj;
        for (Map.Entry<String, String> e : map.entrySet()) {
            String k = e.getKey();
            String v = e.getValue();
            if (v == null) obj.add(k, com.google.gson.JsonNull.INSTANCE);
            else obj.addProperty(k, v);
        }
        return obj;
    }

    private com.google.gson.JsonObject numberMapToJson(Map<String, ? extends Number> map) {
        com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
        if (map == null) return obj;
        for (Map.Entry<String, ? extends Number> e : map.entrySet()) {
            String k = e.getKey();
            Number v = e.getValue();
            if (v == null) {
                obj.add(k, com.google.gson.JsonNull.INSTANCE);
            } else if (v instanceof Integer || v instanceof Long) {
                obj.addProperty(k, v.longValue());
            } else {
                obj.addProperty(k, v.doubleValue());
            }
        }
        return obj;
    }

    public Map<String, Integer> subSerializeMovementStates(MovementStates movementStatesObject) {
        Map<String, Integer> movementStates = new HashMap<>();

        movementStates.put("walking", movementStatesObject.walking ? 1 : 0);
        movementStates.put("flying", movementStatesObject.flying ? 1 : 0);
        movementStates.put("swimJumping", movementStatesObject.swimJumping ? 1 : 0);
        movementStates.put("swimming", movementStatesObject.swimming ? 1 : 0);
        movementStates.put("climbing", movementStatesObject.climbing ? 1 : 0);
        movementStates.put("crouching", movementStatesObject.crouching ? 1 : 0);
        movementStates.put("falling", movementStatesObject.falling ? 1 : 0);
        movementStates.put("forcedCrouching", movementStatesObject.forcedCrouching ? 1 : 0);
        movementStates.put("gliding", movementStatesObject.gliding ? 1 : 0);
        movementStates.put("horizontalIdle", movementStatesObject.horizontalIdle ? 1 : 0);
        movementStates.put("idle", movementStatesObject.idle ? 1 : 0);
        movementStates.put("inFluid", movementStatesObject.inFluid ? 1 : 0);
        movementStates.put("jumping", movementStatesObject.jumping ? 1 : 0);
        movementStates.put("mantling", movementStatesObject.mantling ? 1 : 0);
        movementStates.put("mounting", movementStatesObject.mounting ? 1 : 0);
        movementStates.put("onGround", movementStatesObject.onGround ? 1 : 0);
        movementStates.put("rolling", movementStatesObject.rolling ? 1 : 0);
        movementStates.put("running", movementStatesObject.running ? 1 : 0);
        movementStates.put("sitting", movementStatesObject.sitting ? 1 : 0);
        movementStates.put("sleeping", movementStatesObject.sleeping ? 1 : 0);
        movementStates.put("sliding", movementStatesObject.sliding ? 1 : 0);
        movementStates.put("sprinting", movementStatesObject.sprinting ? 1 : 0);

        return movementStates;
    }

    public Map<String, Double> subSerializeTransformPosition(TransformComponent transformObject) {
        Map<String, Double> transformPosition = new HashMap<>();
        Vector3d telemetryVector3d =  transformObject.getPosition();
        transformPosition.put("x", telemetryVector3d.getX());
        transformPosition.put("y", telemetryVector3d.getY());
        transformPosition.put("z", telemetryVector3d.getZ());

        return transformPosition;
    }

    public Map<String, Float> subSerializeRotation(TransformComponent transformObject) {
        Map<String, Float> rotationMap = new HashMap<>();
        Vector3f rotationVector3f = transformObject.getRotation();

        rotationMap.put("x", rotationVector3f.getX());
        rotationMap.put("y", rotationVector3f.getY());
        rotationMap.put("z", rotationVector3f.getZ());
        rotationMap.put("yaw", rotationVector3f.getYaw());
        rotationMap.put("pitch", rotationVector3f.getPitch());
        rotationMap.put("roll", rotationVector3f.getRoll());

        return rotationMap;
    }

    public Map<String, Double> subSerializeVelocity(Velocity velocityObject) {
        Map<String, Double> velocityMap = new HashMap<>();
        Vector3d velocityVector3f = velocityObject.getVelocity();

        velocityMap.put("x", velocityVector3f.getX());
        velocityMap.put("y", velocityVector3f.getY());
        velocityMap.put("z", velocityVector3f.getZ());

        return velocityMap;
    }

    public Map<String, Float> subSerializeStatMap(EntityStatMap statMap) {
        EntityStatValue health = statMap.get(DefaultEntityStatTypes.getHealth());
        EntityStatValue stamina = statMap.get(DefaultEntityStatTypes.getStamina());
        EntityStatValue mana = statMap.get(DefaultEntityStatTypes.getMana());
        EntityStatValue ammo = statMap.get(DefaultEntityStatTypes.getAmmo());
        EntityStatValue oxygen = statMap.get(DefaultEntityStatTypes.getOxygen());
        EntityStatValue signatureEnergy = statMap.get(DefaultEntityStatTypes.getSignatureEnergy());

        Map<String, Float> statsMap = new HashMap<>();
        statsMap.put("health", health.get());
        statsMap.put("stamina", stamina.get());
        statsMap.put("mana", mana.get());
        statsMap.put("ammo", ammo.get());
        statsMap.put("oxygen", oxygen.get());
        statsMap.put("signatureEnergy", signatureEnergy.get());

        statsMap.put("maxHealth", health.getMax());
        statsMap.put("maxStamina", stamina.getMax());
        statsMap.put("maxMana", mana.getMax());
        statsMap.put("maxAmmo", ammo.getMax());
        statsMap.put("maxOxygen", oxygen.getMax());
        statsMap.put("maxSignatureEnergy", signatureEnergy.getMax());

        return statsMap;

    }

    public Map<String, Integer> subSerializeDamageData(DamageDataComponent damageDataComponent) {
        Map<String, Integer> damageData = new HashMap<>();

        Instant lastDamageTime = damageDataComponent.getLastDamageTime();
        Instant lastChargeTime = damageDataComponent.getLastChargeTime();
        Instant lastCombatAction = damageDataComponent.getLastCombatAction();

        if(lastDamageTime == null) {
            damageData.put("lastDamageTimeEpochMilli", -1);
        } else {
            damageData.put("lastDamageTimeEpochMilli", lastDamageTime.getNano());
        }
        if(lastChargeTime == null) {
            damageData.put("lastChargeTimeEpochMilli", -1);
        } else {
            damageData.put("lastChargeTimeEpochMilli", lastChargeTime.getNano());
        }
        if(lastCombatAction == null) {
            damageData.put("lastCombatActionEpochMilli", -1);
        } else {
            damageData.put("lastCombatActionEpochMilli", lastCombatAction.getNano());
            return damageData;

        }

        return damageData;
    }


    public String serializedPlayerInformation() {
        com.google.gson.JsonArray playersArray = new com.google.gson.JsonArray();

        for(PlayerComponentContainer playerComponentContainer : playerComponentContainers.values()) {
            Player player = playerComponentContainer.getPlayer();
            MovementStatesComponent movementStatesComponent = playerComponentContainer.getMovementStatesComponent();
            TransformComponent transformComponent = playerComponentContainer.getTransformComponent();
            EntityStatMap entityStatMap = playerComponentContainer.getEntityStatMap();
            Velocity velocityMap = playerComponentContainer.getVelocity();
            DamageDataComponent damageDataComponent = playerComponentContainer.getDamageDataComponent();

            //store
            String currentWorldName = "null";
            if(player.getWorld() != null) {
                currentWorldName = player.getWorld().getName();
            }
            String currentGameMode = "null";
            if(player.getGameMode() != null) {
                currentGameMode = player.getGameMode().toString();
            }
            Map<String, String> playerInfo = new HashMap<>();
            playerInfo.put("currentWorldName", currentWorldName);
            playerInfo.put("currentGameMode", currentGameMode);

            Map<String, Integer> movementStates = movementStatesComponent != null && movementStatesComponent.getMovementStates() != null
                    ? subSerializeMovementStates(movementStatesComponent.getMovementStates())
                    : null;
            Map<String, Double> transformPosition = transformComponent != null ? subSerializeTransformPosition(transformComponent) : null;
            Map<String, Float> transformRotation = transformComponent != null ? subSerializeRotation(transformComponent) : null;
            Map<String, Float> statMap = entityStatMap != null ? subSerializeStatMap(entityStatMap) : null;
            Map<String, Double> velocity = velocityMap != null ? subSerializeVelocity(velocityMap) : null;
            Map<String, Integer> damageData = damageDataComponent != null ? subSerializeDamageData(damageDataComponent) : null;

            com.google.gson.JsonObject playerJson = new com.google.gson.JsonObject();
            playerJson.add("playerInfo", stringMapToJson(playerInfo));
            playerJson.add("movementStates", numberMapToJson(movementStates));
            playerJson.add("transformPosition", numberMapToJson(transformPosition));
            playerJson.add("transformRotation", numberMapToJson(transformRotation));
            playerJson.add("statMap", numberMapToJson(statMap));
            playerJson.add("velocity", numberMapToJson(velocity));
            playerJson.add("damageData", numberMapToJson(damageData));

            playersArray.add(playerJson);
        }

        // Return the JSON string for the array of players
        return playersArray.toString();
    }

    public void gatherPlayerInformation(Universe universe, PlayerRef playerRef) {
        if (playerRef == null) {
            return;
        }
        UUID worldUuid = playerRef.getWorldUuid();

        if (worldUuid == null) {
            return;
        }
        World currentPlayerWorld = universe.getWorld(worldUuid);

        if (currentPlayerWorld == null) {
            return;
        }
        currentPlayerWorld.execute(() -> {
            UUIDComponent uuidComponent =
                    playerRef.getComponent(UUIDComponent.getComponentType());
            TransformComponent transformComponent =
                    playerRef.getComponent(TransformComponent.getComponentType());
            Nameplate nameplate =
                    playerRef.getComponent(Nameplate.getComponentType());
            DisplayNameComponent displayNameComponent =
                    playerRef.getComponent(DisplayNameComponent.getComponentType());
            PositionDataComponent positionDataComponent =
                    playerRef.getComponent(PositionDataComponent.getComponentType());
            Velocity velocity =
                    playerRef.getComponent(Velocity.getComponentType());
            CollisionResultComponent collisionResultComponent =
                    playerRef.getComponent(CollisionResultComponent.getComponentType());
            MovementStatesComponent movementStatesComponent =
                    playerRef.getComponent(MovementStatesComponent.getComponentType());
            Player player =
                    playerRef.getComponent(Player.getComponentType());
            DamageDataComponent damageDataComponent =
                    playerRef.getComponent(DamageDataComponent.getComponentType());
            KnockbackComponent knockbackComponent =
                    playerRef.getComponent(KnockbackComponent.getComponentType());
            EntityStatMap entityStatMap =
                    playerRef.getComponent(EntityStatMap.getComponentType());

            PlayerComponentContainer container = playerComponentContainers.computeIfAbsent(
                    playerRef.getUuid(),
                    k -> new PlayerComponentContainer()
            );

            container.setUuidComponent(uuidComponent);
            container.setTransformComponent(transformComponent);
            container.setNameplate(nameplate);
            container.setDisplayNameComponent(displayNameComponent);
            container.setPositionDataComponent(positionDataComponent);
            container.setVelocity(velocity);
            container.setCollisionResultComponent(collisionResultComponent);
            container.setMovementStatesComponent(movementStatesComponent);
            container.setPlayer(player);
            container.setDamageDataComponent(damageDataComponent);
            container.setKnockbackComponent(knockbackComponent);
            container.setEntityStatMap(entityStatMap);
        });
    }

    /**
     * Log detailed packet metrics
     */
    private void logPacketMetrics() {
        long now = System.currentTimeMillis();

        // Only log at specified intervals
        if (now - lastMetricsLogTime < METRICS_LOG_INTERVAL) {
            return;
        }

        lastMetricsLogTime = now;

        // Get all metrics in one efficient call
        PacketMetrics.AllMetrics metrics = packetMetrics.getAllMetrics();

        // Log formatted metrics
        LOGGER.atInfo().log("=================================================");
        LOGGER.atInfo().log("PACKET METRICS REPORT");
        LOGGER.atInfo().log("=================================================");
        LOGGER.atInfo().log(" 1 Minute Window:");
        LOGGER.atInfo().log("   " + metrics.oneMinute.toString());
        LOGGER.atInfo().log("");
        LOGGER.atInfo().log(" 5 Minute Window:");
        LOGGER.atInfo().log("   " + metrics.fiveMinutes.toString());
        LOGGER.atInfo().log("");
        LOGGER.atInfo().log("10 Minute Window:");
        LOGGER.atInfo().log("   " + metrics.tenMinutes.toString());
        LOGGER.atInfo().log("");
        LOGGER.atInfo().log("Lifetime Stats:");
        LOGGER.atInfo().log("   " + metrics.lifetime.toString());
        LOGGER.atInfo().log("=================================================");
        LOGGER.atInfo().log("History Queue Size: " + packetMetrics.getHistorySize() + " records");
        LOGGER.atInfo().log("=================================================");
    }

    /**
     * Get packet metrics (for external access)
     */
    public PacketMetrics getPacketMetrics() {
        return packetMetrics;
    }

    public void runNetworkTick() {
        // Serialize player data
        String json_data = serializedPlayerInformation();
        byte[] compressed = Zstd.compress(json_data.getBytes(), 6);

        // Record packet in metrics
        packetMetrics.recordPacket(compressed.length);

        // Log detailed metrics (periodically)
        logPacketMetrics();

        System.out.println(packetMetrics.getOneMinuteMetrics());
    }

    @Override
    public void run() {
        Configuration.load();

        final long TICK_NANOS = 1_000_000_000L / Configuration.getPlayerHookFrequency();
        Universe serverUniverse = Universe.get();

        while (!Thread.currentThread().isInterrupted()) {
            long start = System.nanoTime();

            // Gather player information
            for (PlayerRef player : serverUniverse.getPlayers()) {
                gatherPlayerInformation(serverUniverse, player);
            }

            long elapsed = System.nanoTime() - start;
            long sleepNanos = TICK_NANOS - elapsed;

            if (sleepNanos > 0) {
                long sleepMillis = sleepNanos / 1_000_000L;
                int sleepNanoPart = (int) (sleepNanos % 1_000_000L);
                try {
                    Thread.sleep(sleepMillis, sleepNanoPart);
                } catch (InterruptedException e) {
                    // restore interrupted status and exit loop
                    Thread.currentThread().interrupt();
                    break;
                }
            } else {
                // We're behind schedule. Yield briefly to avoid burning CPU.
                Thread.yield();
            }
        }
    }
}


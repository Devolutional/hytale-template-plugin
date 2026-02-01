package org.alias.rpgPlugin.skilltable;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.ComponentUpdate;
import com.hypixel.hytale.protocol.DamageCause;
import com.hypixel.hytale.protocol.EntityUpdate;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.entities.EntityUpdates;
import com.hypixel.hytale.protocol.packets.player.ClientMovement;
import com.hypixel.hytale.protocol.packets.player.DamageInfo;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.handlers.game.GamePacketHandler;
import org.alias.rpgPlugin.RPGPluginEntry;
import org.alias.rpgPlugin.hooks.player.PlayerHook;

public class EventHooks {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final long threadInterval = 0; // run as fast as we can
    private static final boolean threadTermination = false;

    public static void createPlayerHooks(RPGPluginEntry plugin) {
        // Fix the lambda syntax and method name
        new PlayerHook(plugin);
    }


}
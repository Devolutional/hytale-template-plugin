package org.alias.rpgPlugin.hooks.player;

import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.damage.DamageDataComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.CollisionResultComponent;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PositionDataComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;

public class PlayerComponentContainer {
    private UUIDComponent uuidComponent;
    private TransformComponent transformComponent;
    private Nameplate nameplate;
    private DisplayNameComponent displayNameComponent;
    private PositionDataComponent positionDataComponent;
    private Velocity velocity;
    private CollisionResultComponent collisionResultComponent;
    private MovementStatesComponent movementStatesComponent;
    private Player player;
    private DamageDataComponent damageDataComponent;
    private KnockbackComponent knockbackComponent;
    private EntityStatMap entityStatMap;

    public PlayerComponentContainer(UUIDComponent uuidComponent, TransformComponent transformComponent, Nameplate nameplate, DisplayNameComponent displayNameComponent, PositionDataComponent positionDataComponent, Velocity velocity, CollisionResultComponent collisionResultComponent, MovementStatesComponent movementStatesComponent, Player player, DamageDataComponent damageDataComponent, KnockbackComponent knockbackComponent, EntityStatMap entityStatMap) {
        this.uuidComponent = uuidComponent;
        this.transformComponent = transformComponent;
        this.nameplate = nameplate;
        this.displayNameComponent = displayNameComponent;
        this.positionDataComponent = positionDataComponent;
        this.velocity = velocity;
        this.collisionResultComponent = collisionResultComponent;
        this.movementStatesComponent = movementStatesComponent;
        this.player = player;
        this.damageDataComponent = damageDataComponent;
        this.knockbackComponent = knockbackComponent;
        this.entityStatMap = entityStatMap;
    }

    public PlayerComponentContainer() {

    }

    // Getters
    public UUIDComponent getUuidComponent() {
        return uuidComponent;
    }

    public TransformComponent getTransformComponent() {
        return transformComponent;
    }

    public Nameplate getNameplate() {
        return nameplate;
    }

    public DisplayNameComponent getDisplayNameComponent() {
        return displayNameComponent;
    }

    public PositionDataComponent getPositionDataComponent() {
        return positionDataComponent;
    }

    public Velocity getVelocity() {
        return velocity;
    }

    public CollisionResultComponent getCollisionResultComponent() {
        return collisionResultComponent;
    }

    public MovementStatesComponent getMovementStatesComponent() {
        return movementStatesComponent;
    }

    public Player getPlayer() {
        return player;
    }

    public DamageDataComponent getDamageDataComponent() {
        return damageDataComponent;
    }

    public KnockbackComponent getKnockbackComponent() {
        return knockbackComponent;
    }

    public EntityStatMap getEntityStatMap() {
        return entityStatMap;
    }

    // Setters
    public void setUuidComponent(UUIDComponent uuidComponent) {
        this.uuidComponent = uuidComponent;
    }

    public void setTransformComponent(TransformComponent transformComponent) {
        this.transformComponent = transformComponent;
    }

    public void setNameplate(Nameplate nameplate) {
        this.nameplate = nameplate;
    }

    public void setDisplayNameComponent(DisplayNameComponent displayNameComponent) {
        this.displayNameComponent = displayNameComponent;
    }

    public void setPositionDataComponent(PositionDataComponent positionDataComponent) {
        this.positionDataComponent = positionDataComponent;
    }

    public void setVelocity(Velocity velocity) {
        this.velocity = velocity;
    }

    public void setCollisionResultComponent(CollisionResultComponent collisionResultComponent) {
        this.collisionResultComponent = collisionResultComponent;
    }

    public void setMovementStatesComponent(MovementStatesComponent movementStatesComponent) {
        this.movementStatesComponent = movementStatesComponent;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void setDamageDataComponent(DamageDataComponent damageDataComponent) {
        this.damageDataComponent = damageDataComponent;
    }

    public void setKnockbackComponent(KnockbackComponent knockbackComponent) {
        this.knockbackComponent = knockbackComponent;
    }

    public void setEntityStatMap(EntityStatMap entityStatMap) {
        this.entityStatMap = entityStatMap;
    }
}
package net.swofty.swm.api.events;

import lombok.Getter;
import net.swofty.swm.api.world.SlimeWorld;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Objects;

public class PreGenerateWorldEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean isCancelled;
    @Getter
    private SlimeWorld slimeWorld;

    public PreGenerateWorldEvent(SlimeWorld slimeWorld) {
        super(false);
        this.slimeWorld = Objects.requireNonNull(slimeWorld, "slimeWorld cannot be null");
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return this.isCancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.isCancelled = cancelled;
    }

    public void setSlimeWorld(SlimeWorld slimeWorld) {
        this.slimeWorld = Objects.requireNonNull(slimeWorld, "slimeWorld cannot be null");
    }
}
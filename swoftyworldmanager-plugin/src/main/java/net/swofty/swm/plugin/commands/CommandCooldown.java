package net.swofty.swm.plugin.commands;

public interface CommandCooldown {
    long cooldownSeconds();

    default long getCooldown() {
        return cooldownSeconds() * 1000;
    }
}
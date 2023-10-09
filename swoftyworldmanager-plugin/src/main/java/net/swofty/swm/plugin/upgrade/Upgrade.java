package net.swofty.swm.plugin.upgrade;

import net.swofty.swm.nms.CraftSlimeWorld;

public interface Upgrade {

    void upgrade(CraftSlimeWorld world);
    void downgrade(CraftSlimeWorld world);
}

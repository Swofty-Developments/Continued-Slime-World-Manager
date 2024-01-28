package net.swofty.swm.clsm;

import com.mojang.datafixers.util.Either;

import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

/**
 * This class serves as a bridge between the SWM and the Minecraft server.
 *
 * As plugins are loaded using a different ClassLoader, their code cannot
 * be accessed from a NMS method. Because of this, it's impossible to make
 * any calls to any method when rewriting the bytecode of a NMS class.
 *
 * As a workaround, this bridge simply calls a method of the {@link CLSMBridge} interface,
 * which is implemented by the SWM plugin when loaded.
 */
public class ClassModifier {

    private static CLSMBridge customLoader;

    public static boolean skipWorldAdd(Object world) {
        return customLoader != null && customLoader.skipWorldAdd(world);
    }

    public static void setLoader(CLSMBridge loader) {
        customLoader = loader;
    }

    public static Object[] getDefaultWorlds() {
        return customLoader != null ? customLoader.getDefaultWorlds() : null;
    }
}

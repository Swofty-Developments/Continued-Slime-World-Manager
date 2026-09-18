package net.swofty.swm.plugin.loader;

import net.swofty.swm.api.exceptions.NewerFormatException;
import net.swofty.swm.api.exceptions.UnsupportedWorldVersionException;
import net.swofty.swm.api.utils.NibbleArray;
import net.swofty.swm.api.utils.SlimeFormat;
import net.swofty.swm.api.world.SlimeChunk;
import net.swofty.swm.api.world.SlimeChunkSection;
import net.swofty.swm.api.world.SlimeWorld;
import net.swofty.swm.api.world.properties.SlimeProperties;
import net.swofty.swm.api.world.properties.SlimePropertyMap;
import net.swofty.swm.nms.craft.CraftSlimeWorld;
import net.swofty.swm.plugin.world.importer.ImporterImpl;
import org.junit.Assume;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.Assert.*;

public class LegacyFormatTest {

    private static final int FORMAT_VERSION_OFFSET = 2;
    private static final int WORLD_VERSION_OFFSET = 3;

    @Test
    public void readsFormatV5WrittenByGrinderwolf1x() throws Exception {
        assertLimbo(deserialize(fixture("limbo-v5.slime")));
    }

    @Test
    public void readsFormatV9WrittenByGrinderwolf2x() throws Exception {
        assertLimbo(deserialize(fixture("limbo-v9.slime")));
    }

    @Test
    public void readsWorldsSavedByTheGrinderwolf2xPlugin() throws Exception {
        CraftSlimeWorld world = deserialize(fixture("limbo-v9-plugin.slime"));
        SlimePropertyMap properties = world.getPropertyMap();

        assertLimbo(world);
        assertEquals(64, (int) properties.getValue(SlimeProperties.SPAWN_Y));
        assertEquals("peaceful", properties.getValue(SlimeProperties.DIFFICULTY));
        assertFalse(properties.getValue(SlimeProperties.ALLOW_MONSTERS));
        assertSameBlocks(deserialize(fixture("limbo-v9.slime")), world);
    }

    @Test
    public void legacyFormatsDecodeToTheSameWorld() throws Exception {
        assertSameWorld(deserialize(fixture("limbo-v5.slime")), deserialize(fixture("limbo-v9.slime")));
    }

    @Test
    public void legacyWorldIsRewrittenInTheCurrentFormat() throws Exception {
        for (String name : new String[] { "limbo-v5.slime", "limbo-v9.slime" }) {
            CraftSlimeWorld legacy = deserialize(fixture(name));
            byte[] rewritten = legacy.serialize();

            assertEquals(SlimeFormat.SLIME_VERSION, rewritten[FORMAT_VERSION_OFFSET]);
            assertSameWorld(legacy, deserialize(rewritten));
        }
    }

    @Test
    public void rejectsPost113WorldsStoredWithAWorldVersionByte() throws Exception {
        byte[] data = fixture("limbo-v9.slime");
        data[WORLD_VERSION_OFFSET] = 4;

        try {
            deserialize(data);
            fail();
        } catch (UnsupportedWorldVersionException ex) {
            assertEquals(4, ex.getWorldVersion());
        }
    }

    @Test
    public void rejectsPost113WorldsStoredWithABooleanFlag() throws Exception {
        byte[] data = fixture("limbo-v5.slime");
        data[WORLD_VERSION_OFFSET] = 1;

        try {
            deserialize(data);
            fail();
        } catch (UnsupportedWorldVersionException ex) {
            assertEquals(4, ex.getWorldVersion());
        }
    }

    @Test
    public void rejectsNewerFormats() throws Exception {
        byte[] data = fixture("limbo-v9.slime");
        data[FORMAT_VERSION_OFFSET] = (byte) (SlimeFormat.SLIME_VERSION + 1);

        try {
            deserialize(data);
            fail();
        } catch (NewerFormatException ex) {
            assertFalse(ex instanceof UnsupportedWorldVersionException);
        }
    }

    @Test
    public void legacyFormatsMatchTheAnvilWorldTheyWereImportedFrom() throws Exception {
        String anvilDir = System.getProperty("cswm.test.anvil");
        Assume.assumeNotNull(anvilDir);

        SlimeWorld imported = new ImporterImpl().readFromDirectory(new File(anvilDir));

        assertSameWorld((CraftSlimeWorld) imported, deserialize(fixture("limbo-v5.slime")));
        assertSameWorld((CraftSlimeWorld) imported, deserialize(fixture("limbo-v9.slime")));
    }

    private static void assertLimbo(CraftSlimeWorld world) {
        assertEquals(1, world.getChunks().size());

        SlimeChunk chunk = world.getChunks().values().iterator().next();
        int blocks = 0;

        for (SlimeChunkSection section : chunk.getSections()) {
            if (section == null) {
                continue;
            }

            for (byte block : section.getBlocks()) {
                if (block != 0) {
                    blocks++;
                }
            }
        }

        assertTrue("chunk has no blocks", blocks > 0);
    }

    private static void assertSameBlocks(CraftSlimeWorld expected, CraftSlimeWorld actual) {
        assertEquals(expected.getChunks().keySet(), actual.getChunks().keySet());

        for (Map.Entry<Long, SlimeChunk> entry : expected.getChunks().entrySet()) {
            SlimeChunk a = entry.getValue();
            SlimeChunk b = actual.getChunks().get(entry.getKey());

            assertEquals(a.getTileEntities(), b.getTileEntities());

            for (int i = 0; i < 16; i++) {
                SlimeChunkSection sa = a.getSections()[i];
                SlimeChunkSection sb = b.getSections()[i];

                if (sa == null || sb == null) {
                    assertNull(sa);
                    assertNull(sb);
                    continue;
                }

                assertArrayEquals(sa.getBlocks(), sb.getBlocks());
                assertArrayEquals(backing(sa.getData()), backing(sb.getData()));
            }
        }
    }

    private static void assertSameWorld(CraftSlimeWorld expected, CraftSlimeWorld actual) {
        assertEquals(expected.getChunks().keySet(), actual.getChunks().keySet());

        for (Map.Entry<Long, SlimeChunk> entry : expected.getChunks().entrySet()) {
            SlimeChunk a = entry.getValue();
            SlimeChunk b = actual.getChunks().get(entry.getKey());

            assertEquals(a.getX(), b.getX());
            assertEquals(a.getZ(), b.getZ());
            assertArrayEquals(a.getBiomes(), b.getBiomes());
            assertEquals(a.getHeightMaps(), b.getHeightMaps());
            assertEquals(a.getTileEntities(), b.getTileEntities());
            assertEquals(a.getEntities(), b.getEntities());

            for (int i = 0; i < 16; i++) {
                SlimeChunkSection sa = a.getSections()[i];
                SlimeChunkSection sb = b.getSections()[i];

                if (sa == null || sb == null) {
                    assertNull(sa);
                    assertNull(sb);
                    continue;
                }

                assertArrayEquals(sa.getBlocks(), sb.getBlocks());
                assertArrayEquals(backing(sa.getData()), backing(sb.getData()));
                assertArrayEquals(backing(sa.getBlockLight()), backing(sb.getBlockLight()));
                assertArrayEquals(backing(sa.getSkyLight()), backing(sb.getSkyLight()));
            }
        }

        assertEquals(expected.getWorldMaps(), actual.getWorldMaps());
    }

    private static byte[] backing(NibbleArray array) {
        return array == null ? null : array.getBacking();
    }

    public static CraftSlimeWorld deserialize(byte[] data) throws Exception {
        return LoaderUtils.deserializeWorld(null, "limbo", data, new SlimePropertyMap(), false);
    }

    public static byte[] fixture(String name) throws IOException {
        try (InputStream in = LegacyFormatTest.class.getResourceAsStream("/legacy/" + name)) {
            assertNotNull(name, in);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;

            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }

            return out.toByteArray();
        }
    }
}

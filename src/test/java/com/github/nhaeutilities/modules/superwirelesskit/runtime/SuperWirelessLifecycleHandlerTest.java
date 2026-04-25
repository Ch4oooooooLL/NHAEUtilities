package com.github.nhaeutilities.modules.superwirelesskit.runtime;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;

import org.junit.After;
import org.junit.Test;

import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import sun.misc.Unsafe;

public class SuperWirelessLifecycleHandlerTest {

    @After
    public void tearDownDebugLog() {
        SuperWirelessDebugLog.resetForTests();
    }

    @Test
    public void onChunkLoadWritesDebugEntryWhenEnabled() throws Exception {
        World world = allocateWorld(0, false, 40L);
        Chunk chunk = new Chunk(world, 2, 3);
        CapturingRuntimeManager manager = new CapturingRuntimeManager();
        SuperWirelessLifecycleHandler handler = new SuperWirelessLifecycleHandler(manager);

        CapturingDebugSink sink = new CapturingDebugSink();
        SuperWirelessDebugLog.installSinkForTests(sink);
        SuperWirelessDebugLog.configure(true);

        handler.onChunkLoad(new ChunkEvent.Load(chunk));

        assertTrue(manager.refreshChunkCalled);
        assertTrue(sink.contents().contains("LIFECYCLE_CHUNK_LOAD"));
        assertTrue(sink.contents().contains("chunk=0:2,3"));
    }

    @Test
    public void onWorldTickWritesDeferredRefreshDebugEntryOnInterval() throws Exception {
        World world = allocateWorld(0, false, 40L);
        CapturingRuntimeManager manager = new CapturingRuntimeManager();
        SuperWirelessLifecycleHandler handler = new SuperWirelessLifecycleHandler(manager);

        CapturingDebugSink sink = new CapturingDebugSink();
        SuperWirelessDebugLog.installSinkForTests(sink);
        SuperWirelessDebugLog.configure(true);

        handler.onWorldTick(new TickEvent.WorldTickEvent(Side.SERVER, TickEvent.Phase.END, world));

        assertTrue(manager.refreshDeferredBindingsCalled);
        assertTrue(sink.contents().contains("LIFECYCLE_WORLD_TICK_REFRESH"));
        assertTrue(sink.contents().contains("world=0"));
        assertTrue(sink.contents().contains("time=40"));
    }

    @Test
    public void onWorldTickSkipsDebugEntryOutsideRefreshInterval() throws Exception {
        World world = allocateWorld(0, false, 41L);
        CapturingRuntimeManager manager = new CapturingRuntimeManager();
        SuperWirelessLifecycleHandler handler = new SuperWirelessLifecycleHandler(manager);

        CapturingDebugSink sink = new CapturingDebugSink();
        SuperWirelessDebugLog.installSinkForTests(sink);
        SuperWirelessDebugLog.configure(true);

        handler.onWorldTick(new TickEvent.WorldTickEvent(Side.SERVER, TickEvent.Phase.END, world));

        assertFalse(manager.refreshDeferredBindingsCalled);
        assertFalse(sink.contents().contains("LIFECYCLE_WORLD_TICK_REFRESH"));
    }

    @Test
    public void onBlockBreakWritesDebugEntryWhenEnabled() throws Exception {
        World world = allocateWorld(0, false, 0L);
        CapturingRuntimeManager manager = new CapturingRuntimeManager();
        SuperWirelessLifecycleHandler handler = new SuperWirelessLifecycleHandler(manager);

        CapturingDebugSink sink = new CapturingDebugSink();
        SuperWirelessDebugLog.installSinkForTests(sink);
        SuperWirelessDebugLog.configure(true);

        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(4, 5, 6, world, Block.getBlockById(1), 0, null);
        handler.onBlockBreak(event);

        assertTrue(manager.onBlockBrokenCalled);
        assertTrue(sink.contents().contains("LIFECYCLE_BLOCK_BREAK"));
        assertTrue(sink.contents().contains("block=0:4,5,6"));
    }

    @Test
    public void onWorldUnloadWritesDebugEntryWhenEnabled() throws Exception {
        World world = allocateWorld(0, false, 0L);
        CapturingRuntimeManager manager = new CapturingRuntimeManager();
        SuperWirelessLifecycleHandler handler = new SuperWirelessLifecycleHandler(manager);

        CapturingDebugSink sink = new CapturingDebugSink();
        SuperWirelessDebugLog.installSinkForTests(sink);
        SuperWirelessDebugLog.configure(true);

        handler.onWorldUnload(new WorldEvent.Unload(world));

        assertTrue(manager.onWorldUnloadCalled);
        assertTrue(sink.contents().contains("LIFECYCLE_WORLD_UNLOAD"));
        assertTrue(sink.contents().contains("world=0"));
    }

    private static World allocateWorld(int dimensionId, boolean remote, long totalWorldTime) throws Exception {
        Unsafe unsafe = getUnsafe();
        World world = (World) unsafe.allocateInstance(WorldServer.class);
        WorldProvider provider = (WorldProvider) unsafe.allocateInstance(WorldProviderSurface.class);
        provider.dimensionId = dimensionId;

        Field providerField = World.class.getDeclaredField("provider");
        providerField.setAccessible(true);
        providerField.set(world, provider);

        Field remoteField = World.class.getDeclaredField("isRemote");
        remoteField.setAccessible(true);
        remoteField.setBoolean(world, remote);

        Field chunkProviderField = World.class.getDeclaredField("chunkProvider");
        chunkProviderField.setAccessible(true);
        chunkProviderField.set(world, new TestChunkProvider());

        Field worldInfoField = World.class.getDeclaredField("worldInfo");
        worldInfoField.setAccessible(true);
        worldInfoField.set(world, new net.minecraft.world.storage.WorldInfo(new net.minecraft.nbt.NBTTagCompound()));

        Field totalTimeField = net.minecraft.world.storage.WorldInfo.class.getDeclaredField("totalTime");
        totalTimeField.setAccessible(true);
        totalTimeField.setLong(worldInfoField.get(world), totalWorldTime);
        return world;
    }

    private static Unsafe getUnsafe() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }

    private static final class CapturingRuntimeManager extends SuperWirelessRuntimeManager {

        private boolean refreshChunkCalled;
        private boolean refreshDeferredBindingsCalled;
        private boolean onBlockBrokenCalled;
        private boolean onWorldUnloadCalled;

        private CapturingRuntimeManager() {
            super(new BindingDataStore(), new BindingNodeResolver());
        }

        @Override
        public void refreshChunk(Chunk chunk) {
            refreshChunkCalled = true;
        }

        @Override
        public void refreshDeferredBindings(World world) {
            refreshDeferredBindingsCalled = true;
        }

        @Override
        public void onBlockBroken(World world, int x, int y, int z) {
            onBlockBrokenCalled = true;
        }

        @Override
        public void onWorldUnload(World world) {
            onWorldUnloadCalled = true;
        }
    }

    private static final class TestChunkProvider implements IChunkProvider {

        @Override
        public boolean chunkExists(int x, int z) {
            return true;
        }

        @Override
        public Chunk provideChunk(int x, int z) {
            return null;
        }

        @Override
        public Chunk loadChunk(int x, int z) {
            return null;
        }

        @Override
        public void populate(IChunkProvider provider, int x, int z) {}

        @Override
        public boolean saveChunks(boolean saveAll, net.minecraft.util.IProgressUpdate progressUpdate) {
            return true;
        }

        @Override
        public boolean unloadQueuedChunks() {
            return false;
        }

        @Override
        public boolean canSave() {
            return true;
        }

        @Override
        public String makeString() {
            return "test";
        }

        @Override
        public java.util.List<net.minecraft.world.biome.BiomeGenBase.SpawnListEntry> getPossibleCreatures(
            net.minecraft.entity.EnumCreatureType type, int x, int y, int z) {
            return java.util.Collections.emptyList();
        }

        @Override
        public net.minecraft.world.ChunkPosition func_147416_a(World world, String structureName, int x, int y, int z) {
            return null;
        }

        @Override
        public int getLoadedChunkCount() {
            return 1;
        }

        @Override
        public void recreateStructures(int x, int z) {}

        @Override
        public void saveExtraData() {}
    }

    private static final class CapturingDebugSink implements SuperWirelessDebugLog.DebugSink {

        private final StringBuilder contents = new StringBuilder();

        @Override
        public void write(String line) {
            contents.append(line).append('\n');
        }

        @Override
        public void close() {}

        private String contents() {
            return contents.toString();
        }
    }

}

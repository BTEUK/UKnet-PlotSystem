package me.bteuk.plotsystem.utils.plugins;

import java.util.List;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.bukkit.World;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.session.ClipboardHolder;

import static me.bteuk.network.utils.Constants.MAX_Y;
import static me.bteuk.network.utils.Constants.MIN_Y;

public class WorldEditor {

    public static boolean updateWorld(List<BlockVector2> copyVector, List<BlockVector2> pasteVector, World copy, World paste) {

        //Get the worlds in worldEdit format
        com.sk89q.worldedit.world.World copyWorld = new BukkitWorld(copy);
        com.sk89q.worldedit.world.World pasteWorld = new BukkitWorld(paste);

        Polygonal2DRegion copyRegion = new Polygonal2DRegion(copyWorld, copyVector, MIN_Y, MAX_Y - 1);
        Polygonal2DRegion pasteRegion = new Polygonal2DRegion(pasteWorld, pasteVector, MIN_Y, MAX_Y - 1);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(copyRegion);

        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                .world(copyWorld).fastMode(true).checkMemory(false).limitUnlimited().changeSetNull().build()) {
            ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(
                    editSession, copyRegion, clipboard, copyRegion.getMinimumPoint()
            );
            forwardExtentCopy.setCopyingBiomes(true);
            // configure here
            Operations.complete(forwardExtentCopy);
        } catch (WorldEditException e) {
            e.printStackTrace();
            return false;
        }

        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                .world(pasteWorld).fastMode(true).checkMemory(false).limitUnlimited().changeSetNull().build()) {
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(pasteRegion.getMinimumPoint())
                    .copyBiomes(true)
                    // configure here
                    .build();
            Operations.complete(operation);
            editSession.flushQueue();
        } catch (WorldEditException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static boolean largeCopy(BlockVector3 copyMin, BlockVector3 copyMax, BlockVector3 pasteMin, World copy, World paste) {

        //Get the worlds in worldEdit format
        com.sk89q.worldedit.world.World copyWorld = new BukkitWorld(copy);
        com.sk89q.worldedit.world.World pasteWorld = new BukkitWorld(paste);

        CuboidRegion copyRegion = new CuboidRegion(copyWorld, copyMin, copyMax);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(copyRegion);

        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                .world(copyWorld).fastMode(false).checkMemory(true).limitUnlimited().changeSetNull().build()) {
            ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(
                    editSession, copyRegion, clipboard, copyRegion.getMinimumPoint()
            );
            forwardExtentCopy.setCopyingBiomes(true);
            Operations.complete(forwardExtentCopy);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                .world(pasteWorld).fastMode(false).checkMemory(true).limitUnlimited().changeSetNull().build()) {
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(pasteMin)
                    .ignoreAirBlocks(true)
                    .copyBiomes(true)
                    // configure here
                    .build();
            Operations.complete(operation);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        clipboard.close();

        return true;

    }
}
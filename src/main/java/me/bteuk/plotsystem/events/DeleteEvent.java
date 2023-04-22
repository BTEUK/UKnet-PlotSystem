package me.bteuk.plotsystem.events;

import com.sk89q.worldedit.math.BlockVector2;
import me.bteuk.network.utils.Utils;
import me.bteuk.plotsystem.PlotSystem;
import me.bteuk.plotsystem.sql.PlotSQL;
import me.bteuk.plotsystem.utils.plugins.WorldEditor;
import me.bteuk.plotsystem.utils.plugins.WorldGuardFunctions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class DeleteEvent {

    public static void event(String uuid, String[] event) {

        //Events for deleting
        if (event[1].equals("plot")) {

            //PlotSQL
            PlotSQL plotSQL = PlotSystem.getInstance().plotSQL;

            //Convert the string id to int id.
            int id = Integer.parseInt(event[2]);

            //Get location which is the world.
            String location = plotSQL.getString("SELECT location FROM plot_data WHERE id=" + id + ";");

            //Get worlds of plot and save location.
            String save_world = PlotSystem.getInstance().getConfig().getString("save_world");
            if (save_world == null) {
                PlotSystem.getInstance().getLogger().warning("Save World is not defined in config, plot delete event has therefore failed!");
                return;
            }

            World copyWorld = Bukkit.getWorld(save_world);
            //Location name is the same as the world name.
            World pasteWorld = Bukkit.getWorld(location);

            if (copyWorld == null || pasteWorld == null) {

                //Send error to console.
                Bukkit.getLogger().severe("Plot delete event failed!");
                Bukkit.getLogger().severe("Event details:" + Arrays.toString(event));
                return;

            }

            int minusXTransform = -plotSQL.getInt("SELECT xTransform FROM location_data WHERE name='" + location + "';");
            int minusZTransform = -plotSQL.getInt("SELECT zTransform FROM location_data WHERE name='" + location + "';");

            //Get the plot bounds.
            List<BlockVector2> pasteVector = WorldGuardFunctions.getPoints(String.valueOf(id), pasteWorld);

            if (pasteVector == null) {
                return;
            }

            //Create the copyVector by transforming the points in the paste vector with the negative transform.
            //The negative transform is used because the coordinates by default are transformed from the save to the paste world, which in this case it reversed.
            List<BlockVector2> copyVector = new ArrayList<>();
            for (BlockVector2 bv : pasteVector) {
                copyVector.add(BlockVector2.at(bv.getX() + minusXTransform, bv.getZ() + minusZTransform));
            }

            //Revert plot to original state.
            WorldEditor.updateWorld(copyVector, pasteVector, copyWorld, pasteWorld);

            //Remove all members from the worldguard plot.
            WorldGuardFunctions.clearMembers(event[2], pasteWorld);

            //Remove all members of plot in database.
            PlotSystem.getInstance().plotSQL.update("DELETE FROM plot_members WHERE id=" + id + ";");

            //Set plot status to unclaimed.
            PlotSystem.getInstance().plotSQL.update("UPDATE plot_data SET status='unclaimed' WHERE id=" + id + ";");

            //Send message to plot owner.
            Player p = Bukkit.getPlayer(UUID.fromString(uuid));

            //If the player is on this server send them a message.
            if (p != null) {

                p.sendMessage(Utils.success("Plot ")
                        .append(Component.text(id, NamedTextColor.DARK_AQUA))
                        .append(Utils.success(" deleted")));

            } else {

                //Add the message to the database so it can be sent wherever they are currently.
                PlotSystem.getInstance().globalSQL.update("INSERT INTO messages(recipient,message) VALUES('" + uuid + "','&cPlot &4" + id + "&cdeleted');");

            }
        } else if (event[1].equals("zone")) {

            //PlotSQL
            PlotSQL plotSQL = PlotSystem.getInstance().plotSQL;

            //Convert the string id to int id.
            int id = Integer.parseInt(event[2]);

            //Get location which is the world.
            String location = plotSQL.getString("SELECT location FROM zones WHERE id=" + id + ";");

            //Get worlds of plot and save location.
            String save_world = PlotSystem.getInstance().getConfig().getString("save_world");
            if (save_world == null) {
                PlotSystem.getInstance().getLogger().warning("Save World is not defined in config, plot delete event has therefore failed!");
                PlotSystem.getInstance().getLogger().severe("Event details:" + Arrays.toString(event));
                return;
            }

            World copyWorld = Bukkit.getWorld(save_world);
            //Location name is the same as the world name.
            World pasteWorld = Bukkit.getWorld(location);

            if (copyWorld == null || pasteWorld == null) {

                //Send error to console.
                PlotSystem.getInstance().getLogger().severe("Zone delete event failed!");
                PlotSystem.getInstance().getLogger().severe("Event details:" + Arrays.toString(event));
                return;

            }

            int minusXTransform = -plotSQL.getInt("SELECT xTransform FROM location_data WHERE name='" + location + "';");
            int minusZTransform = -plotSQL.getInt("SELECT zTransform FROM location_data WHERE name='" + location + "';");

            //Get the zone bounds.
            List<BlockVector2> pasteVector = WorldGuardFunctions.getPoints("z" + event[2], pasteWorld);

            if (pasteVector == null) {
                return;
            }

            //Create the copyVector by transforming the points in the paste vector with the negative transform.
            //The negative transform is used because the coordinates by default are transformed from the save to the paste world, which in this case it reversed.
            List<BlockVector2> copyVector = new ArrayList<>();
            for (BlockVector2 bv : pasteVector) {
                copyVector.add(BlockVector2.at(bv.getX() + minusXTransform, bv.getZ() + minusZTransform));
            }

            //Revert zone to original state.
            WorldEditor.updateWorld(copyVector, pasteVector, copyWorld, pasteWorld);

            //Remove the zone from worldguard.
            WorldGuardFunctions.delete("z" + event[2], pasteWorld);
            WorldGuardFunctions.clearMembers(event[2], pasteWorld);

            //Remove all members of plot in database.
            PlotSystem.getInstance().plotSQL.update("DELETE FROM plot_members WHERE id=" + id + ";");

            //Set plot status to unclaimed.
            PlotSystem.getInstance().plotSQL.update("UPDATE plot_data SET status='unclaimed' WHERE id=" + id + ";");

            //Send message to plot owner.
            Player p = Bukkit.getPlayer(UUID.fromString(uuid));

            //If the player is on this server send them a message.
            if (p != null) {

                p.sendMessage(Utils.success("Zone ")
                        .append(Component.text(id, NamedTextColor.DARK_AQUA))
                        .append(Utils.success(" deleted")));

            } else {

                //Add the message to the database so it can be sent wherever they are currently.
                PlotSystem.getInstance().globalSQL.update("INSERT INTO messages(recipient,message) VALUES('" + uuid + "','&cZone &4" + id + "&cdeleted');");

            }
        }
    }
}

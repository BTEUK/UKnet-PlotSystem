package me.bteuk.plotsystem.commands;

import com.sk89q.worldedit.math.BlockVector3;
import me.bteuk.network.Network;
import me.bteuk.network.events.EventManager;
import me.bteuk.network.utils.NetworkUser;
import me.bteuk.network.utils.Utils;
import me.bteuk.plotsystem.PlotSystem;
import me.bteuk.plotsystem.gui.CreatePlotGui;
import me.bteuk.plotsystem.sql.GlobalSQL;
import me.bteuk.plotsystem.sql.PlotSQL;
import me.bteuk.plotsystem.utils.User;
import me.bteuk.plotsystem.utils.plugins.Multiverse;
import me.bteuk.plotsystem.utils.plugins.WorldEditor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreateCommand {

    private final GlobalSQL globalSQL;
    private final PlotSQL plotSQL;

    public CreateCommand(GlobalSQL globalSQL, PlotSQL plotSQL) {

        this.globalSQL = globalSQL;
        this.plotSQL = plotSQL;

    }

    public void create(CommandSender sender, String[] args) {

        if (args.length < 2) {

            sender.sendMessage(Utils.error("/plotsystem create [plot, location, zone]"));
            return;

        }

        switch (args[1]) {

            case "plot":

                createPlot(sender);
                break;

            case "location":

                createLocation(sender, args);
                break;

            case "zone":

                break;

            default:

                sender.sendMessage(Utils.error("/plotsystem create [plot, location, zone]"));

        }


    }

    private void createPlot(CommandSender sender) {

        //Check if the sender is a player
        if (!(sender instanceof Player)) {

            sender.sendMessage(Utils.error("This command can only be used by players!"));
            return;

        }

        //Get the user
        User u = PlotSystem.getInstance().getUser((Player) sender);

        //Check if the user has permission to use this command
        if (!u.player.hasPermission("uknet.plots.create.plot")) {

            u.player.sendMessage(Utils.error("You do not have permission to use this command!"));
            return;

        }

        //Check if the plot is valid, meaning that at least 3 points are selected with the selection tool.
        if (u.selectionTool.size() < 3) {

            u.player.sendMessage(Utils.error("You must select at least 3 points for a valid plot!"));
            return;

        }

        //Open the plot creation menu
        //Calculate the area of the plot and set a default size estimate.
        u.selectionTool.area();
        u.selectionTool.setDefaultSize();

        //Get the user from the network plugin, this plugin handles all guis.
        NetworkUser user = Network.getInstance().getUser(u.player);

        //Open the create gui.
        u.createGui = new CreatePlotGui(u);
        u.createGui.open(user);

    }

    private void createLocation(CommandSender sender, String[] args) {

        //Check if the sender is a player.
        //If so, check if they have permission.
        if (sender instanceof Player p) {
            if (!p.hasPermission("uknet.plots.create.location")) {

                p.sendMessage(Utils.error("You do not have permission to use this command!"));
                return;

            }
        }

        //Check if they have enough args.
        if (args.length < 7) {

            sender.sendMessage(Utils.error("/plotsystem create location [name] <Xmin> <Zmin> <Xmax> <Zmax>"));
            return;

        }

        int xmin;
        int zmin;

        int xmax;
        int zmax;

        //Check if the coordinates are actual numbers.
        try {

            xmin = Integer.parseInt(args[3]);
            zmin = Integer.parseInt(args[4]);

            xmax = Integer.parseInt(args[5]);
            zmax = Integer.parseInt(args[6]);

        } catch (NumberFormatException e) {

            sender.sendMessage(Utils.error("/plotsystem create location [name] <Xmin> <Zmin> <Xmax> <Zmax>"));
            return;

        }

        //Check if the location name is unique.
        if (plotSQL.hasRow("SELECT name FROM location_data WHERE name='" + args[2] + "';")) {

            sender.sendMessage(Utils.error("The location &4" + args[2] + " &calready exists."));
            return;

        }

        //Get the exact regions of the selected coordinates.
        int regionXMin = Math.floorDiv(xmin, 512);
        int regionZMin = Math.floorDiv(zmin, 512);

        int regionXMax = Math.floorDiv(xmax, 512);
        int regionZMax = Math.floorDiv(zmax, 512);

        //Calculate the coordinate transformation.
        int xTransform = -(regionXMin * 512);
        int zTransform = -(regionZMin * 512);

        //Create the world and add the regions.
        Multiverse.createVoidWorld(args[2]);

        String saveWorld = PlotSystem.getInstance().getConfig().getString("save_world");

        if (saveWorld == null) {
            sender.sendMessage(Utils.error("The save world is not set in config."));
            return;
        }

        //Get worlds.
        World copy = Bukkit.getWorld(saveWorld);
        World paste = Bukkit.getWorld(args[2]);

        //Copy paste the regions in the save world.
        //Iterate through the regions one-by-one.
        //Run it asynchronously to not freeze the server.
        sender.sendMessage(Utils.success("Transferring terrain, this may take a while."));
        Bukkit.getScheduler().runTaskAsynchronously(PlotSystem.getInstance(), () -> {

            for (int i = regionXMin; i <= regionXMax; i++) {
                for (int j = regionZMin; j <= regionZMax; j++) {

                    if (!WorldEditor.largeCopy(BlockVector3.at(i * 512, -60, j * 512),
                            BlockVector3.at(i * 512 + 511, 319, j * 512 + 511),
                            BlockVector3.at(i * 512 + xTransform, -60, j * 512 + zTransform),
                            BlockVector3.at(i * 512 + 511 + xTransform, 319, j * 512 + 511 + zTransform)
                            , copy, paste)) {
                        sender.sendMessage(Utils.error("An error occured while transferring the terrain."));
                        return;
                    } else {
                        sender.sendMessage(Utils.success("&aCopied region &3" + i + "," + j + " &ato &3" + args[2]));
                    }

                }
            }

            sender.sendMessage(Utils.success("Terrain transfer has been completed."));

            int coordMin = globalSQL.addCoordinate(new Location(
                    Bukkit.getWorld(args[2]),
                    (regionXMin * 512), -60, (regionZMin * 512), 0, 0));

            int coordMax = globalSQL.addCoordinate(new Location(
                    Bukkit.getWorld(args[2]),
                    ((regionXMax * 512) + 511), 319, ((regionZMax * 512) + 511), 0, 0));

            //Add the location to the database.
            if (plotSQL.update("INSERT INTO location_data(name, alias, server, coordMin, coordMax, xTransform, zTransform) VALUES('"
                    + args[2] + "','" + args[2] + "','" + PlotSystem.SERVER_NAME + "'," + coordMin + "," + coordMax + "," + xTransform + "," + zTransform + ");")) {

                sender.sendMessage(Utils.success("Created new location " + args[2]));

                //Set the status of all effected regions in the region database.
                for (int i = regionXMin; i <= regionXMax; i++) {

                    for (int j = regionZMin; j <= regionZMax; j++) {

                        String region = i + "," + j;

                        //Change region status in region database.
                        //If it already exists remove members.
                        globalSQL.update("INSERT INTO server_events(uuid,type,server,event) VALUES(NULL,'network','"
                                + globalSQL.getString("SELECT name FROM server_data WHERE type='EARTH';") + "'," +
                                "'region set plotsystem " + region + "');");

                        //Add region to database.
                        plotSQL.update("INSERT INTO regions(region,server,location) VALUES('" + region + "','" + PlotSystem.SERVER_NAME + "','" + args[2] + "');");

                    }
                }

            } else {

                sender.sendMessage(Utils.error("An error occurred, please check the console for more info."));
                Bukkit.getLogger().warning("An error occured while adding new location!");

            }

            //If sender is a player teleport them to the location.
            if (sender instanceof Player p) {

                //Get middle.
                double x = ((globalSQL.getDouble("SELECT x FROM coordinates WHERE id=" + coordMax + ";") +
                        globalSQL.getDouble("SELECT x FROM coordinates WHERE id=" + coordMin + ";"))/2) +
                        plotSQL.getInt("SELECT xTransform FROM location_data WHERE name='" + args[2] + "';");

                double z = ((globalSQL.getDouble("SELECT z FROM coordinates WHERE id=" + coordMax + ";") +
                        globalSQL.getDouble("SELECT z FROM coordinates WHERE id=" + coordMin + ";"))/2) +
                        plotSQL.getInt("SELECT zTransform FROM location_data WHERE name='" + args[2] + "';");

                //Teleport to the location.
                World world = Bukkit.getWorld(args[2]);
                double y = world.getHighestBlockYAt((int) x, (int) z);
                y++;

                EventManager.createTeleportEvent(false, p.getUniqueId().toString(), "network", "teleport " + args[2] + " " + x + " " + y + " " + z + " "
                                + p.getLocation().getYaw() + " " + p.getLocation().getPitch(),
                        "&aTeleported to location &3" + plotSQL.getString("SELECT alias FROM location_data WHERE name='" + args[2] + "';"), p.getLocation());
            }
        });
    }
}

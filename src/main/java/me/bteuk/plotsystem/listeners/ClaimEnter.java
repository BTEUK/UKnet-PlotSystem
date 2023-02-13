package me.bteuk.plotsystem.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import me.bteuk.network.utils.Time;
import me.bteuk.network.utils.Utils;
import me.bteuk.plotsystem.PlotSystem;
import me.bteuk.plotsystem.sql.GlobalSQL;
import me.bteuk.plotsystem.sql.PlotSQL;
import me.bteuk.plotsystem.utils.User;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class ClaimEnter implements Listener {

    PlotSQL plotSQL;
    GlobalSQL globalSQL;

    public ClaimEnter(PlotSystem plugin, PlotSQL plotSQL, GlobalSQL globalSQl) {

        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
        this.plotSQL = plotSQL;
        this.globalSQL = globalSQl;

    }


    @EventHandler
    public void joinEvent(PlayerJoinEvent e) {

        Bukkit.getScheduler().scheduleSyncDelayedTask(PlotSystem.getInstance(), () -> {
            User u = PlotSystem.getInstance().getUser(e.getPlayer());
            checkRegion(u);
        },20L);
    }

    @EventHandler
    public void moveEvent(PlayerMoveEvent e) {
        User u = PlotSystem.getInstance().getUser(e.getPlayer());
        checkRegion(u);
    }

    @EventHandler
    public void teleportEvent(PlayerTeleportEvent e) {
        User u = PlotSystem.getInstance().getUser(e.getPlayer());
        //Delay this so the teleport has taken place.
        Bukkit.getScheduler().runTask(PlotSystem.getInstance(), () -> checkRegion(u));
    }

    public void checkRegion(User u) {

        Location l = u.player.getLocation();

        RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        ApplicableRegionSet applicableRegionSet = query.getApplicableRegions(BukkitAdapter.adapt(l));

        for (ProtectedRegion regions : applicableRegionSet) {
            if (regions.contains(BlockVector3.at(l.getX(), l.getY(), l.getZ()))) {
                try {

                    int plot = tryParse(regions.getId());

                    if (plot == 0) {
                        continue;
                    }

                    if (u.inPlot != plot) {

                        //If the plot is claimed, send the relevant message.
                        if (!plotSQL.hasRow("SELECT id FROM plot_members WHERE id=" + plot + ";")) {

                            //Set the claimed value to false to indicate the plot is not claimed.
                            u.isClaimed = false;
                            u.plotOwner = false;
                            u.plotMember = false;
                            u.player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                    TextComponent.fromLegacyText(Utils.success("You have entered plot &3" + plot + "&a, it is unclaimed.")));

                        } else {

                            //Set the claimed value to true to indicate the plot is already claimed.
                            u.isClaimed = true;

                            //If you are the owner of the plot send the relevant message.
                            if (plotSQL.hasRow("SELECT id FROM plot_members WHERE id=" + plot + " AND uuid='" + u.uuid + "' AND is_owner=1;")) {

                                u.plotOwner = true;
                                u.plotMember = false;
                                plotSQL.update("UPDATE plot_members SET last_enter=" + Time.currentTime() + " WHERE id=" + u.inPlot + " AND uuid='" + u.uuid + "';");
                                u.player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                        TextComponent.fromLegacyText(Utils.success("You have entered plot &3" + plot + "&a, you are the owner of this plot.")));

                                //If you are a member of the plot send the relevant message.
                            } else if (plotSQL.hasRow("SELECT id FROM plot_members WHERE id=" + plot + " AND uuid='" + u.uuid + "' AND is_owner=0;")) {

                                u.plotOwner = false;
                                u.plotMember = true;
                                plotSQL.update("UPDATE plot_members SET last_enter=" + Time.currentTime() + " WHERE id=" + u.inPlot + " AND uuid='" + u.uuid + "';");
                                u.player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                        TextComponent.fromLegacyText(Utils.success("You have entered plot &3" + plot + "&a, you are a member of this plot.")));

                            } else {

                                //If you are not an owner or member send the relevant message.
                                u.plotOwner = false;
                                u.plotMember = false;
                                u.player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                        TextComponent.fromLegacyText(Utils.success("You have entered plot &3" + globalSQL.getString("SELECT name FROM player_data WHERE uuid = '" +
                                                plotSQL.getString("SELECT uuid FROM plot_members WHERE id=" + plot + " AND is_owner=1;") + "';") + "'s &aplot.")));

                            }
                        }

                        u.inPlot = plot;

                    } else {

                        //If you are the owner or member of this plot update your last enter time.
                        if (u.plotMember || u.plotOwner) {

                            plotSQL.update("UPDATE plot_members SET last_enter=" + Time.currentTime() + " WHERE id=" + u.inPlot + " AND uuid='" + u.uuid + "';");

                        }

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }

        if (applicableRegionSet.size() < 1 && u.inPlot != 0) {

            //If the plot is claimed, send the relevant message.
            if (!plotSQL.hasRow("SELECT id FROM plot_members WHERE id=" + u.inPlot + ";")) {

                u.player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        TextComponent.fromLegacyText(Utils.success("You have left plot &3" + u.inPlot)));

            } else {

                //If you are the owner of the plot send the relevant message.
                if (plotSQL.hasRow("SELECT id FROM plot_members WHERE id=" + u.inPlot + " AND uuid='" + u.uuid + "' AND is_owner=1;")) {

                    u.plotOwner = false;
                    u.player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            TextComponent.fromLegacyText(Utils.success("You have left your plot")));

                    //If you are a member of the plot send the relevant message.
                } else if (plotSQL.hasRow("SELECT id FROM plot_members WHERE id=" + u.inPlot + " AND uuid='" + u.uuid + "' AND is_owner=0;")) {

                    u.plotMember = false;
                    u.player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            TextComponent.fromLegacyText(Utils.success("You have left plot &3" + plotSQL.getString("SELECT uuid FROM plot_members WHERE id=" + u.inPlot + " AND is_owner=1;") + "';")
                                    + "'s &aplot."));

                } else {

                    //If you are not an owner or member send the relevant message.
                    u.player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            TextComponent.fromLegacyText(Utils.success("You have left plot &3" + plotSQL.getString("SELECT uuid FROM plot_members WHERE id=" + u.inPlot + " AND is_owner=1;") + "';")
                                    + "'s &aplot."));

                }
            }

            u.inPlot = 0;
            u.isClaimed = true;

        }
    }

    public int tryParse(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
package me.bteuk.plotsystem.listeners;

import me.bteuk.plotsystem.navigation.SwitchServer;
import me.bteuk.plotsystem.sql.GlobalSQL;
import me.bteuk.plotsystem.sql.PlotSQL;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import me.bteuk.plotsystem.Main;
import me.bteuk.plotsystem.utils.User;

/*
This class will be a global class, used for all server types.
It will create the initial user class with basic information, such as uuid, name, player.
Additionally, the tutorial data will be loaded to check whether the player needs to complete the tutorial first.
If this server does not have a tutorial, but it has not been completed, then the player will be sent to
an alternative server which does have a tutorial.
 */
public class JoinServer implements Listener {

	private final GlobalSQL globalSQL;
	private final PlotSQL plotSQL;

	public JoinServer(Main plugin, GlobalSQL globalSQL, PlotSQL plotSQL) {

		Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
		this.globalSQL = globalSQL;
		this.plotSQL = plotSQL;

	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void joinEvent(PlayerJoinEvent e) {
		
		//Create instance of User and add it to list.
		User u = new User(e.getPlayer(), globalSQL, plotSQL);
		Main.getInstance().getUsers().add(u);

		//If the player has a join event, execute it.
		if (globalSQL.hasRow("SELECT uuid FROM join_events WHERE uuid=?;")) {

			//Get the event from the database.
			String event = globalSQL.getString("SELECT event FROM join_events WHERE uuid=?");

			//Clear the events.
			globalSQL.update("DELETE FROM join_events WHERE uuid=?;");




		}


	}
}

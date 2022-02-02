package me.bteuk.plotsystem.serverconfig;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import me.bteuk.plotsystem.Main;
import me.bteuk.plotsystem.sql.NavigationSQL;
import me.bteuk.plotsystem.sql.PlotSQL;
import me.bteuk.plotsystem.utils.User;

public class InventoryClicked implements Listener {

	Main instance;
	NavigationSQL navigationSQL;
	PlotSQL plotSQL;

	public InventoryClicked(Main plugin, NavigationSQL navigationSQL, PlotSQL plotSQL) {

		instance = plugin;
		this.navigationSQL = navigationSQL;
		this.plotSQL = plotSQL;

		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onClick(InventoryClickEvent e) {

		if (e.getCurrentItem() == null) {
			return;
		}

		String title = e.getView().getTitle();

		User u = Main.getInstance().getUser((Player) e.getWhoClicked());

		if (title.equals(SetupGui.inventory_name)) {

			e.setCancelled(true);
			if (e.getCurrentItem() == null){

				return;

			}

			SetupGui.clicked(u, e.getSlot(), e.getCurrentItem(), e.getInventory(), navigationSQL, plotSQL);

		} else {

		}
	}
}

package com.HollowPlugins.InventoryInterfaceAPI;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class InventoryInterfaceAPI extends JavaPlugin {
	
	private InventoryInterfaceAPI plugin=null;
	private InventoryInterfaceListener listener;
	
	@Override
	public void onEnable() {
		
		if(plugin==null) plugin=this;
		
		listener = new InventoryInterfaceListener(this);
		this.getServer().getPluginManager().registerEvents(listener, this);
	}
	
	public void registerInterface(Player player, InventoryInterface newInterface) {
		listener.registerInterface(player, newInterface);
	}
	
}
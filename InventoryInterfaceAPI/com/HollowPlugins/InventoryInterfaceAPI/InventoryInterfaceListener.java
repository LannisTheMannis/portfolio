package com.HollowPlugins.InventoryInterfaceAPI;

import java.util.HashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.plugin.Plugin;
import com.HollowPlugins.InventoryInterfaceAPI.InventoryInterface.CallableFunction;

public class InventoryInterfaceListener implements Listener{
	
	private InventoryInterfaceAPI _plugin;
	private HashMap<Player, InventoryInterface> currentInterfaces;


	public InventoryInterfaceListener(InventoryInterfaceAPI plugin) {
		_plugin =  plugin;
		currentInterfaces = new HashMap<>();
	}
	
	public void registerInterface(Player player, InventoryInterface newInterface) {
		currentInterfaces.put(player, newInterface);
	}
	
	
	
	@EventHandler(priority=EventPriority.NORMAL)
	public void onInventoryClose(final InventoryCloseEvent event) {
		Player player = (Player)event.getPlayer();
		if(currentInterfaces.containsKey(player)) {
			if(currentInterfaces.get(player).hasClosingMethod()) {
				CallableFunction closeMethod = currentInterfaces.get(player).getClosingAction();
				currentInterfaces.remove(player);
				if(closeMethod!=null) {
//						currentInterfaces.remove((Player)event.getPlayer());
					
					Bukkit.getServer().getScheduler().scheduleSyncDelayedTask((Plugin)this._plugin, new Runnable() {

						@Override
						public void run() {
							try {
								closeMethod.call();
								currentInterfaces.remove(player);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}, 1L);			
				}
			}
			currentInterfaces.remove(player);
		}
	}
	
	@EventHandler(priority=EventPriority.NORMAL)
	public void onInventoryClick(final InventoryClickEvent event) {
		Player player = (Player)event.getWhoClicked();
		if(event.getSlot()<0)return;
//		if(event.getClickedInventory()
		
		if(currentInterfaces.containsKey(player)) {
			try {
				if(!currentInterfaces.get(player).isInterface(event.getClickedInventory())) return;
				
				if(currentInterfaces.get(player).isRegisteredButton(event.getSlot())) {
					if(event.getClick().equals(ClickType.LEFT)) {
						CallableFunction clickMethod = currentInterfaces.get(player).getButtonLeftClickAction(event.getSlot());
						if(clickMethod!=null) {
							clickMethod.call();
						}
					} else if(event.getClick().equals(ClickType.RIGHT)) {
						CallableFunction clickMethod = currentInterfaces.get(player).getButtonRightClickAction(event.getSlot());
						if(clickMethod!=null) {
							clickMethod.call();
						}
					}

					event.setCancelled(true);
				}
				
				
			} catch (Exception e) {
				_plugin.getLogger().log(Level.INFO, "No valid event for clicked slot");
				e.printStackTrace();
			}
		}
	}
	


	
}
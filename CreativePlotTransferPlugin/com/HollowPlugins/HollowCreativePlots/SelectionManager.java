package com.HollowPlugins.HollowCreativePlots;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.hollowPlugins.HollowPluginBase.HollowPluginBase;
import com.hollowPlugins.HollowPluginBase.HollowPluginBaseListener;

import net.md_5.bungee.api.ChatColor;

public class SelectionManager extends HollowPluginBaseListener{
	public SelectionManager(HollowPluginBase plugin) {
		super(plugin);
		this.plugin = (HollowCreativePlots)plugin;
		activeSessions = new HashMap<>();
	}

	private HollowCreativePlots plugin;
	private HashMap<UUID,SelectionPair> activeSessions;
	

	@EventHandler(priority=EventPriority.NORMAL)
	public void onBlockClick(final PlayerInteractEvent event) {
		if(!event.getPlayer().getWorld().getName().equals(plugin.conf.getString("main_world", "NewWorld"))) {
			return;
		}
		if(event.getHand()!=EquipmentSlot.HAND) {
			return;
		}
		ItemStack itemInMainHand = event.getPlayer().getInventory().getItemInMainHand();
		if(!plugin.matchesSelectionTool(itemInMainHand)) {
			return;
		}
		if(event.getAction()==Action.RIGHT_CLICK_BLOCK) {
			activeSessions.putIfAbsent(event.getPlayer().getUniqueId(), new SelectionPair());
			Vector clickedPosition=new Vector(event.getClickedBlock().getX(),0,event.getClickedBlock().getZ());
			activeSessions.get(event.getPlayer().getUniqueId()).selectSecond(clickedPosition);
			event.getPlayer().sendMessage(ChatColor.BLUE+"Second point set to "+clickedPosition.getBlockX()+","+clickedPosition.getBlockZ());
			event.setCancelled(true);
		} else if(event.getAction()==Action.LEFT_CLICK_BLOCK) {
			activeSessions.putIfAbsent(event.getPlayer().getUniqueId(), new SelectionPair());
			Vector clickedPosition=new Vector(event.getClickedBlock().getX(),0,event.getClickedBlock().getZ());
			activeSessions.get(event.getPlayer().getUniqueId()).selectFirst(clickedPosition);
			event.getPlayer().sendMessage(ChatColor.BLUE+"First point set to "+clickedPosition.getBlockX()+","+clickedPosition.getBlockZ());
			event.setCancelled(true);
		} else if(event.getAction()==Action.LEFT_CLICK_AIR) {
			activeSessions.putIfAbsent(event.getPlayer().getUniqueId(), new SelectionPair());
			Vector clickedPosition=event.getPlayer().getLocation().toVector();
			activeSessions.get(event.getPlayer().getUniqueId()).selectFirst(clickedPosition);
			event.getPlayer().sendMessage(ChatColor.BLUE+"First point set to "+clickedPosition.getBlockX()+","+clickedPosition.getBlockZ());
			event.setCancelled(true);
		}else if(event.getAction()==Action.RIGHT_CLICK_AIR) {
			activeSessions.putIfAbsent(event.getPlayer().getUniqueId(), new SelectionPair());
			Vector clickedPosition=event.getPlayer().getLocation().toVector();
			activeSessions.get(event.getPlayer().getUniqueId()).selectSecond(clickedPosition);
			event.getPlayer().sendMessage(ChatColor.BLUE+"Second point set to "+clickedPosition.getBlockX()+","+clickedPosition.getBlockZ());
			event.setCancelled(true);
		}
	}
	
	public boolean finishSelection(Player player) {
		if(!activeSessions.containsKey(player.getUniqueId())) {
			player.sendMessage(plugin.formatCommandResponse()+ChatColor.BLUE+"No current selection found");
			return false;
		}
		SelectionPair currentSelection = activeSessions.get(player.getUniqueId());
		if(!currentSelection.firstSet()) {
			player.sendMessage(plugin.formatCommandResponse()+ChatColor.BLUE+"No first point selected");
			currentSelection.print(player);
			return false;
		}
		if(!currentSelection.secondSet()) {
			player.sendMessage(plugin.formatCommandResponse()+ChatColor.BLUE+"No second point selected");
			currentSelection.print(player);
			return false;
		}
		Vector diagonal = CreativePlot.calculateSpan(plugin, currentSelection.positionOne, currentSelection.positionTwo);
		if(diagonal.getBlockX()==0||diagonal.getBlockZ()==0) {
			player.sendMessage(plugin.formatCommandResponse()+ChatColor.BLUE+"Selection only has one dimension");
			currentSelection.print(player);
			return false;
		} else if(diagonal.getBlockX()>300||diagonal.getBlockZ()>300) {
			player.sendMessage(plugin.formatCommandResponse()+ChatColor.BLUE+"Selection is too large! Max 300 blocks in any direction.");
			currentSelection.print(player);
			return false;
		}
		
		List<CreativePlot> ownedPlots = plugin.getDatabaseManager().getPlayerPlots(player.getUniqueId().toString());
		if(ownedPlots.size()>4) {
			player.sendMessage(plugin.formatCommandResponse()+ChatColor.BLUE+"Max plots exceeded! You can only have five at a time.");
			return false;
		}
		
		Vector overworldCorner = CreativePlot.calculateOverworldCorner(plugin, currentSelection.positionOne, currentSelection.positionTwo);
		plugin.log("Corner: "+overworldCorner.getBlockX()+","+overworldCorner.getBlockZ());
		plugin.log("Span: "+diagonal.getBlockX()+","+diagonal.getBlockZ());

		CreativePlot plot =  new CreativePlot(plugin,player.getUniqueId(), diagonal,overworldCorner);
		if(plot.canOwnerAffordUpkeep()) {
			plot.subtractCost();
		} else {
			player.sendMessage(plugin.formatCommandResponse()+ChatColor.BLUE+"You can't afford to claim this plot!");
			return false;
		}
		activeSessions.remove(player.getUniqueId());
		if(plot.register()) {
			return plot.transferToCreative(player);
		} else {
			player.sendMessage(plugin.formatCommandResponse()+ChatColor.BLUE+"Could not find room for this in the creative map.");
			currentSelection.print(player);

		}
		return false;
	}
	
	
	class SelectionPair{
		private Vector positionOne=null;
		private Vector positionTwo=null;
		
		public boolean selectFirst(Vector pos) {
			positionOne=pos;
			return true;
		}
		public boolean selectSecond(Vector pos) {
			positionTwo=pos;
			return true;
		}
		public boolean firstSet() {
			return positionOne!=null;
		}
		public boolean secondSet() {
			return positionTwo!=null;
		}
		
		public void print(Player player) {
			if(firstSet()) {
				player.sendMessage(ChatColor.BLUE+"First position: "+positionOne.getBlockX()+","+positionOne.getBlockZ());
			}
			if(secondSet()) {
				player.sendMessage(ChatColor.BLUE+"Second position: "+positionTwo.getBlockX()+","+positionTwo.getBlockZ());
			}
		}
	}
}

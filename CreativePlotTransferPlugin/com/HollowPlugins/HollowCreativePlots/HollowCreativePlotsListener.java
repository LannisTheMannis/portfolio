package com.HollowPlugins.HollowCreativePlots;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.util.Vector;

import com.hollowPlugins.HollowPluginBase.HollowPluginBase;
import com.hollowPlugins.HollowPluginBase.HollowPluginBaseListener;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.SessionKey;

public class HollowCreativePlotsListener extends HollowPluginBaseListener{
	private HollowCreativePlots plugin;
	
	public HollowCreativePlotsListener(HollowPluginBase plugin) {
		super(plugin);
		this.plugin=(HollowCreativePlots)plugin;
	}
	
	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent event) {
		double currentBalance = plugin.getEconomy().getBalance(event.getPlayer());
		boolean runningOutOfCash=false;
		List<String> messagesToSend = new ArrayList<>();
		for(CreativePlot plot:plugin.getDatabaseManager().getPlayerPlots(event.getPlayer().getUniqueId().toString())) {
			double cost = 4*plot.calculateDailyCost();
			if(currentBalance<cost) {
				if(!runningOutOfCash)messagesToSend.add(plugin.formatCommandResponse()+ChatColor.BLUE+"You're about to run out of money for your creative plots! They will be deleted if you can't afford the upkeep!");
				runningOutOfCash=true;
				messagesToSend.add(ChatColor.BLUE+"   Daily cost: "+plot.calculateDailyCost());
			}
			
		}
		if(runningOutOfCash)messagesToSend.add(ChatColor.BLUE+"   Your balance: "+(int)currentBalance);
		
		Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {

			@Override
			public void run() {
				for(String message:messagesToSend) {
					event.getPlayer().sendMessage(message);
				}
				
			}
			
		}, 30);
	}
		
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		if(!event.getPlayer().getWorld().getName().equals(plugin.conf.getString("creative_world", "creative_plots"))) return;
		if(plugin.isBlockBlacklisted(event.getBlock().getType())){
			if(event.getPlayer().hasPermission("hollowCreativePlots.admin")) {
				event.getPlayer().sendMessage(plugin.formatCommandResponse()+ChatColor.RED+"Override: Placing forbidden block for this world.");
			} else {
				event.getPlayer().sendMessage(plugin.formatCommandResponse()+ChatColor.BLUE+"You aren't allowed to place that block in this creative world.");
				event.setCancelled(true);
				return;
			}
		}
		Vector blockPlacement = event.getBlock().getLocation().toVector();
		CreativePlot currentPlot = plugin.getPlotWithBlock(blockPlacement);
		if(currentPlot==null) {
			event.getPlayer().sendMessage(plugin.formatCommandResponse()+ChatColor.BLUE+"Warning: You aren't building in a plot, and it won't be saved.");
		} else {
			Vector currentOffset = currentPlot.getBlockOffsetFromCorner(blockPlacement);
			String blockData = event.getBlock().getBlockData().getAsString();
			if(!plugin.getDatabaseManager().putCreativeBlockChange(currentPlot.getRowID(), currentOffset, blockData)) {
				plugin.log("Failed to store block placement");
				event.getPlayer().sendMessage(plugin.formatCommandResponse()+ChatColor.BLUE+"Failed to store block placement. Notify an admin!");
				event.setCancelled(true);
			}
		}	
	}
	
	@EventHandler
	public void onContainerOpen(InventoryOpenEvent event) {
		if(!event.getPlayer().getWorld().getName().equals(plugin.conf.getString("creative_world", "creative_plots"))) return;
		if(!event.getView().getType().equals(InventoryType.PLAYER)){
			if(event.getPlayer().hasPermission("hollowCreativePlots.admin")) {
				event.getPlayer().sendMessage(plugin.formatCommandResponse()+ChatColor.RED+"Override: opening container.");
			} else {
				event.getPlayer().sendMessage(plugin.formatCommandResponse()+ChatColor.BLUE+"You aren't allowed to open containers here.");
				event.setCancelled(true);
				return;
			}
		}
	}
	
	@EventHandler
	public void onRightClick(HangingPlaceEvent event) {
		if(!event.getPlayer().getWorld().getName().equals(plugin.conf.getString("creative_world", "creative_plots"))) return;
		if(event.getEntity().getType().equals(EntityType.ITEM_FRAME)) {
			if(event.getPlayer().hasPermission("hollowCreativePlots.admin")) {
				event.getPlayer().sendMessage(plugin.formatCommandResponse()+ChatColor.RED+"Override: Placing forbidden block for this world.");
			} else {
				event.getPlayer().sendMessage(plugin.formatCommandResponse()+ChatColor.BLUE+"You aren't allowed to place that block in this creative world.");
				event.setCancelled(true);
				return;
			}
		}
		Vector blockPlacement = event.getBlock().getLocation().toVector();
		CreativePlot currentPlot = plugin.getPlotWithBlock(blockPlacement);
		if(currentPlot==null) {
			event.getPlayer().sendMessage(plugin.formatCommandResponse()+ChatColor.BLUE+"Warning: You aren't building in a plot, and it won't be saved.");
		} else {
			Vector currentOffset = currentPlot.getBlockOffsetFromCorner(blockPlacement);
			String blockData = event.getBlock().getBlockData().getAsString();
			if(!plugin.getDatabaseManager().putCreativeBlockChange(currentPlot.getRowID(), currentOffset, blockData)) {
				plugin.log("Failed to store block placement");
				event.getPlayer().sendMessage(plugin.formatCommandResponse()+ChatColor.BLUE+"Failed to store block placement. Notify an admin!");
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if(!event.getPlayer().getWorld().getName().equals(plugin.conf.getString("creative_world", "creative_plots"))) return;
		
		Vector blockPlacement = event.getBlock().getLocation().toVector();
		CreativePlot currentPlot = plugin.getPlotWithBlock(blockPlacement);
		if(currentPlot==null) {
			event.getPlayer().sendMessage(plugin.formatCommandResponse()+ChatColor.BLUE+"Warning: You aren't building in a plot, and it won't be saved.");
		} else {
			Vector currentOffset = currentPlot.getBlockOffsetFromCorner(blockPlacement);
			
			String blockData = Material.AIR.createBlockData().getAsString();
			if(!plugin.getDatabaseManager().putCreativeBlockChange(currentPlot.getRowID(), currentOffset, blockData)) {
				plugin.log("Failed to store block placement");
				event.getPlayer().sendMessage(plugin.formatCommandResponse()+ChatColor.BLUE+"Failed to store block placement. Notify an admin!");
				event.setCancelled(true);
			}
		}	
	}
//	
//	@EventHandler
//	public void onWorldEdit(EditSessionEvent event) {
//		plugin.log("WE: "+event.getWorld().getName());
//		if(!event.getWorld().getName().equals(plugin.conf.getString("creative_world", "creative_plots"))) return;
////		if(!event.getStage().equals(EditSession.Stage.BEFORE_HISTORY)) return;
//		
//		plugin.log("Worldedit event caught, "+event.getStage().toString());
//		BlockVector3 maxPoint = event.getExtent().getMaximumPoint();
//		BlockVector3 minPoint = event.getExtent().getMinimumPoint();
//		int count=0;
//		for(BlockVector3 block:new CuboidRegion(minPoint,maxPoint)){
//			Vector blockPlacement = new Vector(block.getBlockX(),block.getBlockY(),block.getBlockZ());
//			CreativePlot currentPlot = plugin.getPlotWithBlock(blockPlacement);
//			if(currentPlot!=null) {
//				Vector currentOffset = currentPlot.getBlockOffsetFromCorner(blockPlacement);
//				String blockData = BukkitAdapter.adapt(event.getWorld()).getBlockAt(currentOffset.getBlockX(),currentOffset.getBlockY(),currentOffset.getBlockZ()).getBlockData().getAsString();
//				if(!plugin.getDatabaseManager().putCreativeBlockChange(currentPlot.getRowID(), currentOffset, blockData)) {
//					plugin.log("Failed to store block placement");
//				} else {
//					count++;
//				}
//			}	
//			
//		}
//		plugin.log("Stored "+count+" blocks in db from WE");
//	}
//	

}

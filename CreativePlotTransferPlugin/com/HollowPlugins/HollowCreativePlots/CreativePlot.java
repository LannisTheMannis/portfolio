package com.HollowPlugins.HollowCreativePlots;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;

import net.md_5.bungee.api.ChatColor;

public class CreativePlot {
	private HollowCreativePlots plugin;
	private UUID ownerUUID;
	private Vector overworldCorner;
	private Vector diagonal;
	private Vector creativeCorner;
	private LocalDateTime creationTime;
	private int rowID;
	
	private static List<QueueEntry> transferQueue = new ArrayList<>();
	private List<DatabaseBlockEntry> modifiedBlocks = new ArrayList<>();
	
	public CreativePlot(HollowCreativePlots plugin, UUID ownerUUID, Vector bottomCorner, Vector diagonal, Vector creativeCorner,LocalDateTime creationTime, int ID) {
		this.plugin=plugin;
		this.ownerUUID=ownerUUID;
		this.overworldCorner=bottomCorner;
		this.diagonal=diagonal;
		this.creativeCorner=creativeCorner;
		this.creationTime=creationTime;
		this.rowID=ID;
	}
	
	public CreativePlot(HollowCreativePlots plugin, UUID ownerUUID, Vector diagonal, Vector overworldCorner) {
		this.plugin=plugin;
		this.ownerUUID=ownerUUID;
		this.overworldCorner= overworldCorner;
		this.creativeCorner = plugin.getBestPlacement(diagonal);
		this.diagonal=diagonal;
		this.creationTime=LocalDateTime.now();
	}
	
	public boolean register() {
		if(this.overworldCorner==null) return false;
		if(this.diagonal==null) return false;
		if(this.creativeCorner==null) return false;
		if(this.ownerUUID==null) return false;
		if(this.diagonal.getBlockX()==0||this.diagonal.getBlockZ()==0) return false;
		
		boolean addedToDatabase = plugin.getDatabaseManager().addPlot(this);
		if(addedToDatabase) {
			plugin.log("Added to database");
			this.rowID = plugin.getDatabaseManager().getRowID(this);
			boolean addedToLists = plugin.addToPlotList(this);
			if(addedToLists) {
				plugin.log("Added to lists");
			} else {
				plugin.getDatabaseManager().removePlot(this);
				addedToDatabase = false;
			}
		}
		return addedToDatabase;
	}
	
	public String getOwnerUUID() {
		return this.ownerUUID.toString();
	}
	
	public String getOverworldCorner() {
		return serializeVector(overworldCorner);
	}
	
	public String getCreativedCorner() {
		return serializeVector(creativeCorner);
	}
	
	public String getSpan() {
		return serializeVector(diagonal);
	}
	
	public String getCreationTime() {
		return creationTime.format(DateTimeFormatter.ISO_DATE_TIME).toString();
	}
	
	public int getRowID() {
		return this.rowID;
	}
	
	public Vector [] getExteriorCorners() {
		Vector [] corners = new Vector[4];
		corners[0] = creativeCorner.clone().add(diagonal);
		corners[0].add(new Vector(16,0,16));
		corners[1] = new Vector(creativeCorner.getX(),0,corners[0].getZ());
		corners[2] = new Vector(corners[0].getX(),0,creativeCorner.getZ());
		corners[3] = creativeCorner.clone();
		return corners;
	}
	
	public boolean intersects(Vector corner, Vector span) {
		Vector topCorner = creativeCorner.clone().add(diagonal);
		topCorner.add(new Vector(16,0,16));
		Vector candidateTopCorner = corner.clone().add(span);
	
		if(topCorner.getBlockX()==creativeCorner.getBlockX()||topCorner.getBlockZ()==creativeCorner.getBlockZ()
				||candidateTopCorner.getBlockX()==corner.getBlockX()||candidateTopCorner.getBlockZ()==corner.getBlockZ()) {
			return false;
		}
		if(corner.getBlockX()>topCorner.getBlockX()||creativeCorner.getBlockX()>candidateTopCorner.getBlockX()) return false;
		if(corner.getBlockZ()>topCorner.getBlockZ()||creativeCorner.getBlockZ()>candidateTopCorner.getBlockZ()) return false;
		return true;
	}
	
	public boolean isBlockWithinPlot(Vector position) {
		//8 block padding from creative corner
		Vector bottomCorner = creativeCorner.clone().add(new Vector(8,0,8));
		Vector topCorner = bottomCorner.clone().add(diagonal);
		
		if(position.getBlockX()>topCorner.getBlockX()||bottomCorner.getBlockX()>position.getBlockX()) return false;
		if(position.getBlockZ()>topCorner.getBlockZ()||bottomCorner.getBlockZ()>position.getBlockZ()) return false;
		return true;
	}
	
	public Vector getBlockOffsetFromCorner(Vector block) {
		Vector origin = creativeCorner.clone().add(new Vector(8,0,8));
		return block.clone().subtract(origin);
	}
	
	public static String serializeVector(Vector vec) {
		return vec.getBlockX()+","+vec.getBlockZ();
	}
	public static Vector deserializeVector(String serialized) {
		try {
			int x = Integer.parseInt(serialized.split(",")[0]);
			int z = Integer.parseInt(serialized.split(",")[1]);
			return new Vector(x,0,z);
		} catch (Exception e) {}
		return null;
	}
		
	public int calculateDailyCost() {
		int numberOfChunks = diagonal.getBlockX()/16*diagonal.getBlockZ()/16;
		return 25*numberOfChunks;
	}
	
	public boolean canOwnerAffordUpkeep() {
		double currentBalance = plugin.getEconomy().getBalance(Bukkit.getServer().getOfflinePlayer(ownerUUID));
		int cost = calculateDailyCost();
		return currentBalance>cost;
	}
	
	public void subtractCost() {
		int cost = calculateDailyCost();
		plugin.log("Trying to withdraw "+cost+" from "+ownerUUID);
		plugin.getEconomy().withdrawPlayer(Bukkit.getServer().getOfflinePlayer(ownerUUID), cost);
	}
	
	public void teleportToCreative(Player player) {
		String creativeWorldName = plugin.conf.getString("creative_world", "creative_plots");
		World creativeWorld = Bukkit.getServer().getWorld(creativeWorldName);
		if(creativeWorld==null) return;
		
		Vector midpoint = creativeCorner.clone().add(new Vector(diagonal.getBlockX()/2+8,0,diagonal.getBlockZ()/2+8));
		Location teleportLocation = midpoint.toLocation(creativeWorld);
		
		player.sendMessage(plugin.formatCommandResponse()+ChatColor.BLUE+"Teleporting to creative plot!");
		
		player.teleport(teleportLocation.toHighestLocation().add(0, 1, 0));
	}
	
	public void teleportToMain(Player player) {
		String worldName = plugin.conf.getString("main_world", "NewWorld");
		World mainWorld = Bukkit.getServer().getWorld(worldName);
		if(mainWorld==null) return;
		
		Vector midpoint = overworldCorner.clone().add(new Vector(diagonal.getBlockX()/2,0,diagonal.getBlockZ()/2));
		Location teleportLocation = midpoint.toLocation(mainWorld);
		
		player.sendMessage(plugin.formatCommandResponse()+ChatColor.BLUE+"Teleporting to main map!");
		
		player.teleport(teleportLocation.toHighestLocation().add(0, 1, 0));
	}
	
	private synchronized void drawCreativeBorder() {
		String creativeWorldName = plugin.conf.getString("creative_world", "creative_plots");
		World creativeWorld = Bukkit.getServer().getWorld(creativeWorldName);
		if(creativeWorld==null) return;
		Location cornerLocation = creativeCorner.clone().toLocation(creativeWorld);
		cornerLocation.setY(3);
		Block startingBlock = cornerLocation.getBlock().getRelative(BlockFace.UP);
		Material fillMaterial = Material.BLUE_WOOL;
		for(int i=0;i<=diagonal.getBlockX()+16;i++) {
			startingBlock.getRelative(i, 0, 0).setType(fillMaterial);
			startingBlock.getRelative(i, 0, diagonal.getBlockZ()+16).setType(fillMaterial);
		}
		for(int i=0;i<=diagonal.getBlockZ()+16;i++) {
			startingBlock.getRelative(0, 0, i).setType(fillMaterial);
			startingBlock.getRelative(diagonal.getBlockX()+16, 0,i).setType(fillMaterial);
		}
		startingBlock.setType(Material.GLOWSTONE);

	}
	
	public synchronized boolean transferToCreative(Player caller) {
		String creativeWorldName = plugin.conf.getString("creative_world", "creative_plots");
		String overworldWorldName = plugin.conf.getString("main_world", "NewWorld");
		World mainWorld = Bukkit.getServer().getWorld(overworldWorldName);
		World creativeWorld = Bukkit.getServer().getWorld(creativeWorldName);
		if(mainWorld==null || creativeWorld==null) return false;
		List<QueueEntry> transferEntries = new ArrayList<>();
		boolean canBuildHere=caller.hasPermission("hollowCreativePlots.admin");

		for(int x=0;x<diagonal.getBlockX();x+=16) {
			for(int z=0;z<diagonal.getBlockZ();z+=16) {
				Vector destination = creativeCorner.clone().add(new Vector(8+x,0,8+z));
				Vector source = overworldCorner.clone().add(new Vector(x,0,z)); 
				if(!canBuildHere) {
					Location testCanBuild = source.toLocation(mainWorld);
					canBuildHere = plugin.getUtilityPlugin().canPlayerBuildHere(caller, testCanBuild);
				}
				QueueEntry entry = new QueueEntry(mainWorld,creativeWorld,source,destination);
//				DatabaseAdditionEntry databaseEntry = new DatabaseAdditionEntry(entry);
//				transferQueue.add(databaseEntry);
				transferEntries.add(entry);
			}
		}
		
		if(!canBuildHere) {
			caller.sendMessage(plugin.formatCommandResponse()+ChatColor.BLUE+"You can't build in any of these plots!");
			return false;
		}
		transferQueue.addAll(transferEntries);
		drawCreativeBorder();
		return plugin.getScheduler().notifyQueue(transferQueue);
	}
	
	public synchronized boolean transferToOverworld(Player caller) {
		String creativeWorldName = plugin.conf.getString("creative_world", "creative_plots");
		String overworldWorldName = plugin.conf.getString("main_world", "NewWorld");
		World mainWorld = Bukkit.getServer().getWorld(overworldWorldName);
		World creativeWorld = Bukkit.getServer().getWorld(creativeWorldName);
		if(mainWorld==null || creativeWorld==null) return false;
		modifiedBlocks = plugin.getDatabaseManager().getProjectBlockChanges(rowID);
		
		modifiedBlocks.forEach(entry->entry.position.add(overworldCorner));
		Bukkit.getScheduler().runTask(plugin, new Runnable() {

			@Override
			public void run() {
				World main = Bukkit.getServer().getWorld(plugin.conf.getString("main_world", "NewWorld"));
				int errorCount=0;
				if(main==null) return;
				while(modifiedBlocks.size()>0) {
					DatabaseBlockEntry entry = modifiedBlocks.get(0);
					Location blockLocation = entry.position.toLocation(main);
					boolean canBuildHere=caller.hasPermission("hollowCreativePlots.admin");
					if(!canBuildHere) {
						canBuildHere = plugin.getUtilityPlugin().canPlayerBuildHere(caller, blockLocation);
					}
					
					if(canBuildHere) {
						Block candidate = blockLocation.getBlock();
						if(!candidate.getBlockData().equals(entry.blockData)) candidate.setBlockData(entry.blockData);
//						plugin.getDatabaseManager().deleteCreativeBlock(entry);
					} else {
						plugin.log("Tried to place block where player couldn't build");
						errorCount++;
					}
					modifiedBlocks.remove(0);
				}
				if(errorCount>0) {
					caller.sendMessage(plugin.formatCommandResponse()+ChatColor.BLUE+"Failed to place "+errorCount+" blocks. Can you build here in the main map?");
				}
			}
			
			
		});
		return true;
		
//		List<QueueEntry> transferEntries = new ArrayList<>();
//
//		for(int x=0;x<diagonal.getBlockX();x+=16) {
//			for(int z=0;z<diagonal.getBlockZ();z+=16) {
//				Vector source = creativeCorner.clone().add(new Vector(8+x,0,8+z));
//				Vector destination = overworldCorner.clone().add(new Vector(x,0,z)); 
//				QueueEntry entry = new QueueEntry(creativeWorld,mainWorld,source,destination);
//				DatabaseAdditionEntry databaseEntry = new DatabaseAdditionEntry(entry);
//				transferQueue.add(databaseEntry);
//				transferEntries.add(entry);
//			}
//		}
//		transferQueue.addAll(transferEntries);
//
//		return plugin.getScheduler().notifyQueue(transferQueue);
	}
	
	public boolean cleanupCreativePlot() {
		String creativeWorldName = plugin.conf.getString("creative_world", "creative_plots");
		World creativeWorld = Bukkit.getServer().getWorld(creativeWorldName);
		if(creativeWorld==null) return false;
		List<QueueEntry> transferEntries = new ArrayList<>();

		for(int x=0;x<diagonal.getBlockX()+16;x+=16) {
			for(int z=0;z<diagonal.getBlockZ()+16;z+=16) {
				Vector destination = creativeCorner.clone().add(new Vector(x,0,z));
				QueueEntry entry = new CleanupChunkEntry(creativeWorld,creativeWorld,destination,destination);
				DatabaseAdditionEntry databaseEntry = new DatabaseAdditionEntry(entry);
				transferQueue.add(entry);
				transferEntries.add(databaseEntry);
			}
		}
		transferQueue.addAll(transferEntries);

		return plugin.getScheduler().notifyQueue(transferQueue);
		}
		
	
	public static Vector calculateSpan(HollowCreativePlots plugin,Vector posOne, Vector posTwo) {
		//always positive
		Vector bottomCorner = calculateOverworldCorner(plugin,posOne,posTwo);
		Vector topCorner  = calculateOverworldTopCorner(plugin,posOne,posTwo);
		Vector initialSpan = topCorner.clone().subtract(bottomCorner);	
		int xDelta = Math.abs(initialSpan.getBlockX());
		int zDelta = Math.abs(initialSpan.getBlockZ());
		//Round up to nearest chunk size;
//		xDelta = (xDelta+15)/16;
//		zDelta = (zDelta+15)/16;
//		xDelta = Math.round((float)xDelta/16f);
//		zDelta = Math.round((float)zDelta/16f);

		//Add 8 blocks of padding
		return new Vector(xDelta,0,zDelta);
	}
	
	public static Vector calculateOverworldCorner(HollowCreativePlots plugin,Vector posOne, Vector posTwo) {
		//always bottom left corner
		int xCoordinate=0;
		int zCoordinate=0;
		int minX = Math.min(posOne.getBlockX(), posTwo.getBlockX());
		int minZ = Math.min(posOne.getBlockZ(), posTwo.getBlockZ());

		if(minX>0) {
			xCoordinate = minX/16; //round down
		} else {
			xCoordinate = (minX-15)/16; //also round down
		}
		if(minZ>0) {
			zCoordinate = minZ/16;
		} else {
			zCoordinate = (minZ-15)/16;
		}
		
		return new Vector(16*xCoordinate,0,16*zCoordinate);

	}
	
	public static Vector calculateOverworldTopCorner(HollowCreativePlots plugin,Vector posOne, Vector posTwo) {
		//always bottom left corner
		int xCoordinate=0;
		int zCoordinate=0;
		int maxX = Math.max(posOne.getBlockX(), posTwo.getBlockX());
		int maxZ = Math.max(posOne.getBlockZ(), posTwo.getBlockZ());

			xCoordinate = (maxX+15)/16; //round down
		
			zCoordinate = (maxZ+15)/16;
		
		
		return new Vector(16*xCoordinate,0,16*zCoordinate);

	}
}

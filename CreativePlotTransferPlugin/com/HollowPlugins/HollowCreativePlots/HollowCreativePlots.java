package com.HollowPlugins.HollowCreativePlots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import com.HollowPlugins.HollowCreativePlots.commands.TeleportToPlot;
import com.HollowPlugins.HollowCreativePlots.commands.AbandonPlot;
import com.HollowPlugins.HollowCreativePlots.commands.CleanWithoutMoving;
import com.HollowPlugins.HollowCreativePlots.commands.DeleteWithoutMoving;
import com.HollowPlugins.HollowCreativePlots.commands.GetSelectionTool;
import com.HollowPlugins.HollowCreativePlots.commands.ParentCommand;
import com.HollowPlugins.HollowCreativePlots.commands.ReturnPlot;
import com.HollowPlugins.HollowCreativePlots.commands.TransferRegion;
import com.HollowPlugins.HollowUtilities.HollowUtilities;
import com.hollowPlugins.HollowPluginBase.HollowPluginBase;
import com.hollowPlugins.HollowPrestige.HollowPrestige;
import com.hollowPlugins.database.HollowPluginDatabase;
import com.wimbli.WorldBorder.BorderData;
import com.wimbli.WorldBorder.WorldBorder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import net.milkbowl.vault.economy.Economy;

public class HollowCreativePlots extends HollowPluginBase {
	private HollowPluginDatabase database;
	private HollowPluginDatabase queueDatabase;
	private DatabaseAccessManager manager;

	private HollowPrestige prestigePlugin;
	private WorldBorder borderPlugin;
	private HollowUtilities utilityPlugin;
	
	private HollowCreativePlotsListener listener;
	private TransferScheduler scheduler;
	private SelectionManager selections;
	
	
	//Plot db
	public final String databaseTableName = "creative_plots";
	public final String plotOwnerUUIDColumn = "owner_uuid";
	public final String creativeCornerColumn = "bottom_corner";
	public final String diagonalVectorColumn = "diagonal_vector";
	public final String overworldCornerColumn = "original_corner";
	public final String creationDateColumn = "creation_date";
	
	
	//Chunk transfer db
	public final String queueDatabaseName = "transfer_queue";
	public final String queueSourceWorld = "source_world";
	public final String queueDestinationWorld = "destination_world";
	public final String queueSourceCorner = "source_corner";
	public final String queueDestinationCorner = "destination_corner";

	//block place db
	public final String blockModificationTable = "blocks_changed";
	public final String projectIDColumn = "block_plot";
	public final String coordinateColumn = "block_position_offset";
	public final String blockDataColumn = "block_data";
	
	
	private List<CreativePlot> currentPlots=new ArrayList<>();
	private List<Vector> corners=new ArrayList<>();
	private List<Material> blockBlacklist=new ArrayList<>();
	
	private ItemStack selectionTool;


	public HollowCreativePlots() {	
		this.hasConfig = true;
	}

	@Override
	protected void _onPluginReady() {
		
		prestigePlugin=null;
		if(Bukkit.getServer().getPluginManager().isPluginEnabled("HollowPrestige")) {
			prestigePlugin=(HollowPrestige)Bukkit.getServer().getPluginManager().getPlugin("HollowPrestige");
			this.log("Loaded HollowPrestige plugin");
		} else {
			this.log("Could not load HollowPrestige");	
		}
		
		borderPlugin=null;
		if(Bukkit.getServer().getPluginManager().isPluginEnabled("WorldBorder")) {
			borderPlugin=(WorldBorder)Bukkit.getServer().getPluginManager().getPlugin("WorldBorder");
			this.log("Loaded WorldBorder plugin");
		} else {
			this.log("Could not load WorldBorder");	
		}
		
		utilityPlugin=null;
		if(Bukkit.getServer().getPluginManager().isPluginEnabled("HollowUtilities")) {
			utilityPlugin=(HollowUtilities)Bukkit.getServer().getPluginManager().getPlugin("HollowUtilities");
			this.log("Loaded HollowUtilities plugin");
		} else {
			this.log("Could not load HollowUtilities");	
		}
		loadDatabase();

		this.manager = new DatabaseAccessManager(this);
		manager.printTables();
		
		this.scheduler = new TransferScheduler(this);
		this.selections = new SelectionManager(this);
		this.listener = new HollowCreativePlotsListener(this);
		registerListener(scheduler);
		registerListener(selections);
		registerListener(listener);

		currentPlots = manager.getAllPlots();
		List<CreativePlot> plotsToDelete = new ArrayList<>();
		Set<Integer> plotIDs = new HashSet<>();
		currentPlots.forEach(plot->{
			plotIDs.add(plot.getRowID());
			if(!plot.canOwnerAffordUpkeep()) {
				plotsToDelete.add(plot);
			} else {
				plot.subtractCost();
			}
		});
		
		plotsToDelete.forEach(plot->{
			deleteAndCleanPlot(plot);
		});
		plotsToDelete.clear();
		
		int cleanedBlocks = manager.cleanupOrphanedBlockEntries(plotIDs);
		log("Cleaned up "+cleanedBlocks+" orphaned database block entries");
		calculateCorners();
				
		selectionTool = new ItemStack(Material.STICK);
		ItemMeta toolMeta = selectionTool.getItemMeta();
		toolMeta.displayName(Component.text(ChatColor.GOLD+"Creative Transfer Selection Tool"));
		toolMeta.lore(Arrays.asList(Component.text(ChatColor.GRAY+"Left click to select first point."),Component.text(ChatColor.GRAY+"Right click to select second point.")));
		selectionTool.setItemMeta(toolMeta);
		
		List<String> ignoredItems = conf.getStringList("IgnoredMaterials");
		
		for(Material mat:Material.values()) {
			String materialName = mat.name();
			boolean ignoredMaterial=false;
			for(String ignored:ignoredItems) {
				if(materialName.contains(ignored)) {
					ignoredMaterial=true;
					break;
				}
			}
			if(!ignoredMaterial&&mat.isBlock()) blockBlacklist.add(mat);	
		}
		
		
		log("Blacklist: ");
		blockBlacklist.forEach(mat->log(mat.name()));
	}

	@Override
	public void onDisable() {
		this.log("Saving current plot transfer queue");
		scheduler.shutdown();
	}
	
	@Override
	protected void _addCommands() {
		commander.addCommand("plot", new ParentCommand(this));
		commander.addSubCommand("plot", "teleport", new TeleportToPlot(this));
		commander.addSubCommand("plot", "wand", new GetSelectionTool(this));
		commander.addSubCommand("plot", "transfer", new TransferRegion(this));
		commander.addSubCommand("plot", "return", new ReturnPlot(this));
		commander.addSubCommand("plot", "clean", new CleanWithoutMoving(this));
		commander.addSubCommand("plot", "abandon", new AbandonPlot(this));

	}

	
	private void loadDatabase() {
		this.database = new HollowPluginDatabase(this,"plots");
				
	    List<String> textColumns = Arrays.asList(plotOwnerUUIDColumn,creativeCornerColumn,diagonalVectorColumn,
	    		overworldCornerColumn,creationDateColumn);
	    HashMap<String,List<String>> regionTableColumns = new HashMap<>();
	    regionTableColumns.put("TEXT", textColumns);
	    this.database.createTable(regionTableColumns,databaseTableName);
	    
	    this.queueDatabase= new HollowPluginDatabase(this,"queue");
	    
	    List<String> queueTextColumns = Arrays.asList(this.queueSourceWorld,this.queueDestinationWorld,this.queueSourceCorner,this.queueDestinationCorner);
	    HashMap<String,List<String>> queueTableColumns = new HashMap<>();
	    queueTableColumns.put("TEXT", queueTextColumns);
	    this.queueDatabase.createTable(queueTableColumns,this.queueDatabaseName);
	    	    
	    List<String> blockTextColumns = Arrays.asList(this.coordinateColumn,this.blockDataColumn);
	    List<String> blockIntColumns = Arrays.asList(this.projectIDColumn);
	    HashMap<String,List<String>> blockTableColumns = new HashMap<>();
	    blockTableColumns.put("TEXT", blockTextColumns);
	    blockTableColumns.put("INTEGER", blockIntColumns);
	    this.queueDatabase.createTable(blockTableColumns,this.blockModificationTable);
	    
    }
	
	public boolean addToPlotList(CreativePlot newPlot) {
		boolean added = currentPlots.add(newPlot);
		calculateCorners();
		return added;
	}
	
	public void calculateCorners() {
		corners.clear();
		for(CreativePlot plot:currentPlots) {
			for(Vector corner:plot.getExteriorCorners()) {
				if(duplicateVector(corners,corner)) {
					corners.removeIf(vec -> vec.getBlockX()==corner.getBlockX()&&vec.getBlockZ()==corner.getBlockZ());
				} else {
					corners.add(corner);
				}
			}
		}
	}
	
//	public void updateCorners(CreativePlot plot) {
//		log("Before corner update:");
//		corners.forEach(corner->log("Vertex: "+corner.getBlockX()+","+corner.getBlockZ()));
//
//		for(Vector corner:plot.getExteriorCorners()) {
//			if(duplicateVector(corners,corner)) {
//				corners.removeIf(vec -> vec.getBlockX()==corner.getBlockX()&&vec.getBlockZ()==corner.getBlockZ());
//			} else {
//				corners.add(corner);
//			}
//		}
//		
//		log("After: "+corners.size());
//		corners.forEach(corner->log("Vertex: "+corner.getBlockX()+","+corner.getBlockZ()));
//
//	}
	
	
	public boolean moveBuildToMainWorld(CreativePlot plot, Player player) {
//		currentPlots.remove(plot);
//		calculateCorners();
		boolean value = plot.transferToOverworld(player);
//		value &= manager.removePlot(plot);
		return value;
	}
	
	public boolean deleteAndCleanPlot(CreativePlot plot) {
		boolean value = plot.cleanupCreativePlot();
		value &= deletePlotWithoutTransfer(plot);
		return value;
	}
	
	public boolean deletePlotWithoutTransfer(CreativePlot plot) {
		currentPlots.remove(plot);
		manager.removeProjectBlockEntries(plot.getRowID());
	
		calculateCorners();
		return manager.removePlot(plot);
	}
	
	public Vector getBestPlacement(Vector span) {
		
		Vector paddedSpan = span.clone().add(new Vector(16,0,16));
		
		Vector bottomCorner=getBottomCorner();
		if(!duplicateVector(corners,bottomCorner)) {
			corners.add(bottomCorner);
		}
		Vector candidatePosition = getBottomCorner();
		boolean foundSpot=false;
		double currentBestDistance = 9000*9000;
		if(candidatePosition==null) {
			log("Couldn't find border");
			return null;
		}
		if(currentPlots.size()==0) {
			//first plot, return corner
			log("Returning bottom corner: "+CreativePlot.serializeVector(candidatePosition));			
			return candidatePosition;
		}
		
		for(Vector corner:corners) {
			for(Vector candidate:getValidSpotsAround(corner,paddedSpan)) {
					boolean validPosition=true;
					for(CreativePlot plot:currentPlots) {
						if(plot.intersects(candidate, paddedSpan)) {
							validPosition=false;
							break;
						}
					}
					if(!validPosition) continue;
					double distance = candidate.distanceSquared(bottomCorner);
					if(distance<currentBestDistance && isWithinBorder(candidate,paddedSpan)) {
						currentBestDistance=distance;
						candidatePosition=candidate;
						foundSpot=true;
					}
					
				}
			}
			
		
		if(!foundSpot) {
			log("No space found for plot!");
			return null;
		}
		
		log("Best position: "+candidatePosition.getBlockX()+","+candidatePosition.getBlockZ());
		return candidatePosition;
	}
	
	public DatabaseAccessManager getDatabaseManager() {
		return this.manager;
	}
	
	public HollowPrestige getPrestigePlugin() {
		return prestigePlugin;
	}
	
	public HollowUtilities getUtilityPlugin() {
		return utilityPlugin;
	}
	
	public Economy getEconomy() {
		return prestigePlugin.getTool().getEconomy();
	}
	
	public TransferScheduler getScheduler() {
		return scheduler;
	}
	
	public SelectionManager getSelectionManager() {
		return this.selections;
	}
	
	public HollowPluginDatabase getDatabase() {
		return this.database;
	}
	
	public HollowPluginDatabase getQueueDatabase() {
		return this.queueDatabase;
	}
	
	public CreativePlot getCreativePlot(int id) {
		for(CreativePlot plot:this.currentPlots) {
			if(plot.getRowID()==id) return plot;
		}
		return null;
	}
	
	public CreativePlot getPlotWithBlock(Vector blockPosition) {
		for(CreativePlot plot:this.currentPlots) {
			if(plot.isBlockWithinPlot(blockPosition)) return plot;
		}
		return null;
		
	}
	
	private boolean isWithinBorder(Vector corner, Vector span) {
		String worldName = conf.getString("creative_world", "creative_plots");
		BorderData border = borderPlugin.getWorldBorder(worldName);
		if(border==null) return false;
		return border.insideBorder(corner.getBlockX(), corner.getBlockZ())&&border.insideBorder(corner.clone().add(span).getBlockX(), corner.clone().add(span).getBlockZ());
	}
	
	public ItemStack getSelectionTool() {
		return new ItemStack(this.selectionTool);
	}
	
	public boolean isBlockBlacklisted(Material block) {
		return blockBlacklist.contains(block);
	}
	
	public boolean matchesSelectionTool(ItemStack item) {
		try {
			String itemName = PlainComponentSerializer.plain().serialize(item.getItemMeta().displayName()).trim();
			String wandName = PlainComponentSerializer.plain().serialize(this.selectionTool.getItemMeta().displayName()).trim();
			itemName = ChatColor.stripColor(itemName);
			wandName = ChatColor.stripColor(wandName);
			return itemName.equals(wandName);
		} catch (Exception e) {}	
		return false;
	}
	
	private Vector getBottomCorner() {
		String worldName = conf.getString("creative_world", "creative_plots");
		BorderData border = borderPlugin.getWorldBorder(worldName);
		if(border==null) return null;
		return new Vector(1+border.getX()-border.getRadiusX(),0,1+border.getZ()-border.getRadiusZ());
	}
	
	private boolean duplicateVector(final List<Vector> list, final Vector corner){
	    return list.stream().filter(vec -> vec.getBlockX()==corner.getBlockX()&&vec.getBlockZ()==corner.getBlockZ()).findFirst().isPresent();
	}
	
	private List<Vector> getValidSpotsAround(Vector initialCorner, Vector span){
		List<Vector> corners = new ArrayList<>();
//		corners.add(initialCorner.clone().add(new Vector(1,0,1)));
		corners.add(initialCorner.clone().add(new Vector(1,0,0)));
		
		corners.add(initialCorner.clone().add(new Vector(1,0,-span.getBlockZ())));
//		corners.add(initialCorner.clone().add(new Vector(1,0,-span.getBlockZ()-1)));

		corners.add(initialCorner.clone().add(new Vector(-span.getBlockX(),0,1)));
		corners.add(initialCorner.clone().add(new Vector(0,0,1)));
		corners.add(initialCorner.clone().add(new Vector(0,0,-span.getBlockZ()-1)));
		corners.add(initialCorner.clone().add(new Vector(-span.getBlockX(),0,-span.getBlockZ()-1)));

//		corners.add(initialCorner.clone().add(new Vector(-span.getBlockX()-1,0,1)));
		corners.add(initialCorner.clone().add(new Vector(-span.getBlockX()-1,0,-span.getBlockZ())));
		corners.add(initialCorner.clone().add(new Vector(-span.getBlockX()-1,0,0)));
//		corners.add(initialCorner.clone().add(new Vector(-span.getBlockX()-1,0,-span.getBlockZ()-1)));


		return corners;
	}
}

package com.HollowPlugins.HollowCreativePlots;

import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitTask;
import com.hollowPlugins.HollowPluginBase.HollowPluginBase;
import com.hollowPlugins.HollowPluginBase.HollowPluginBaseListener;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

public class TransferScheduler extends HollowPluginBaseListener{
	
	private HollowCreativePlots plugin;
	private List<QueueEntry> transferQueue;
	private BukkitTask queueTask;
	
	public TransferScheduler(HollowPluginBase plugin) {
		super(plugin);
		this.plugin=(HollowCreativePlots)plugin;
		transferQueue = this.plugin.getDatabaseManager().getTransferQueue();
		this.plugin.getDatabaseManager().clearTransferQueue();
		
		
		plugin.log("Loaded queue size: "+transferQueue.size());
		
		queueTask = Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
			@Override
			public void run() {
				if(transferQueue.size()>0)
					loopTask(5);
			}
		},200);
		
		if(transferQueue.size()==0) queueTask.cancel();
		
		
	}
	
	
//	@EventHandler(priority=EventPriority.NORMAL)
//	public void onQueueAddition(final TransferQueueAdditionEvent event) {
//		if(queueTask.isCancelled()) {
//			queueTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
//				@Override
//				public void run() {
//					processQueue();
//				}
//			},0,10);
//		}
//	}
	
	
//	public boolean addToQueue(QueueEntry entry) {
//		boolean added= transferQueue.add(entry);
//		processQueue();
//		return added;
//	}
	
	public boolean shutdown() {
		queueTask.cancel();
		return plugin.getDatabaseManager().addToTransferQueue(transferQueue);
	}
	
	public boolean addToQueue(List<QueueEntry> entry) {
		boolean added= transferQueue.addAll(entry);
		plugin.log("Added "+entry.size()+" plots to queue");
		processQueue();
		return added;
	}
	
	public boolean notifyQueue(List<QueueEntry> classQueue) {
		transferQueue = classQueue;
		
		if(transferQueue.size()>0) processQueue();
		return true;
	}
	
	public void processQueue() {
		if(queueTask.isCancelled()) {
			loopTask(5);
		}	
	}
	
	private void loopTask(long delay) {
		queueTask = Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
			@Override
			public void run() {
				synchronized(this) {
					try {
						long recursiveDelay=5;
						if(transferQueue.size()>0) {
							if(!transferQueue.get(0).validEntry()) {
								plugin.log("Invalid queue entry! World is null");
								transferQueue.remove(0);
							} else {
								if(transferQueue.size()%25==0) {
									plugin.log("Transfer queue size: "+transferQueue.size());
								}
								if(transferQueue.get(0) instanceof CleanupChunkEntry) {
									cleanupChunk(transferQueue.get(0));
								} else if(transferQueue.get(0) instanceof QueueEntry) {
									transfer(transferQueue.get(0));
								} 
								transferQueue.remove(0);
							}
						}
						if(transferQueue.size()==0) {
							queueTask.cancel();
						} else {
							loopTask(recursiveDelay);
						}
					}catch (Exception e) {plugin.log("Error in queue process: "+e);}
				}
			}
		},delay);
	}
	
	private void transfer(QueueEntry entry) {
		if(entry.destinationWorld==null||entry.sourceWorld==null) {
			plugin.log("Tried to transfer from unloaded world");
			return;
		}
		if(entry.destinationWorld.getName().equalsIgnoreCase(plugin.conf.getString("main_world", "NewWorld"))) {
			plugin.log("Canceling WE transfer to main map");
			return;
		}
		
		if(!entry.sourceWorld.isChunkLoaded(entry.sourceCorner.toLocation(entry.sourceWorld).getChunk())){
			entry.sourceWorld.loadChunk(entry.sourceCorner.toLocation(entry.sourceWorld).getChunk());
			if(!entry.sourceWorld.isChunkLoaded(entry.sourceCorner.toLocation(entry.sourceWorld).getChunk())) return;
		}
		
		if(!entry.destinationWorld.isChunkLoaded(entry.destinationCorner.toLocation(entry.destinationWorld).getChunk())){
			entry.destinationWorld.loadChunk(entry.destinationCorner.toLocation(entry.destinationWorld).getChunk());
			if(!entry.destinationWorld.isChunkLoaded(entry.destinationCorner.toLocation(entry.destinationWorld).getChunk())) return;
		}
		
		CuboidRegion sampleRegion = new CuboidRegion(BlockVector3.at(entry.sourceCorner.getBlockX(), 5, entry.sourceCorner.getBlockZ()),
				BlockVector3.at(entry.sourceCorner.getBlockX()+16, 5, entry.sourceCorner.getBlockZ()+16));
		sampleRegion.setWorld(BukkitAdapter.adapt(entry.sourceWorld));
		Iterator<BlockVector3> regionIterator = sampleRegion.iterator();
		boolean allAir=true;
		while(regionIterator.hasNext()) {
			BlockVector3 point = regionIterator.next();
			allAir &= entry.sourceWorld.getBlockAt(point.getX(),point.getY(),point.getZ()).getType().equals(Material.AIR);
		}
		//Don't copy blank creative plots back to main
		if(allAir) return;

		
		CuboidRegion region = new CuboidRegion(BlockVector3.at(entry.sourceCorner.getBlockX(), 0, entry.sourceCorner.getBlockZ()),
				BlockVector3.at(entry.sourceCorner.getBlockX()+16, 256, entry.sourceCorner.getBlockZ()+16));
		region.setWorld(BukkitAdapter.adapt(entry.sourceWorld));
		Clipboard clipboard = new BlockArrayClipboard(region);
		
		try (EditSession copySession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(entry.sourceWorld))) {
		    ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(
		    	copySession, region, clipboard, region.getMinimumPoint()
		    );
		    forwardExtentCopy.setCopyingEntities(false);
		    Operations.complete(forwardExtentCopy);
		    

		} catch (WorldEditException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		    // configure here
		try (EditSession pasteSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(entry.destinationWorld))){
		  	Operation operation = new ClipboardHolder(clipboard)
		                .createPaste(pasteSession)
		                .to(BlockVector3.at(entry.destinationCorner.getBlockX(), 0, entry.destinationCorner.getBlockZ()))
		                .ignoreAirBlocks((entry.destinationWorld.getName().equals(plugin.conf.getString("creative_world", "creative_plots"))))
		                .build();
		    Operations.complete(operation);
		        
		    
		} catch (WorldEditException e) {
			e.printStackTrace();
		}
	}
	
	private void cleanupChunk(QueueEntry entry) {
		String creativeWorldName = plugin.conf.getString("creative_world", "creative_plots");
		World creativeWorld = Bukkit.getServer().getWorld(creativeWorldName);
		if(creativeWorld==null) return;
		if(!creativeWorld.isChunkLoaded(entry.sourceCorner.toLocation(creativeWorld).getChunk())){
			creativeWorld.loadChunk(entry.sourceCorner.toLocation(creativeWorld).getChunk());
			if(!creativeWorld.isChunkLoaded(entry.sourceCorner.toLocation(creativeWorld).getChunk())) return;
		}
		
		CuboidRegion region = new CuboidRegion(BlockVector3.at(entry.sourceCorner.getBlockX(), 0, entry.sourceCorner.getBlockZ()),
				BlockVector3.at(entry.sourceCorner.getBlockX()+16, 256, entry.sourceCorner.getBlockZ()+16));
		region.setWorld(BukkitAdapter.adapt(creativeWorld));
		BlockType bedrock = BlockTypes.BEDROCK;
		BlockType air = BlockTypes.AIR;
		BlockType grass = BlockTypes.GRASS_BLOCK;
		BlockType dirt = BlockTypes.DIRT;
		

		try (EditSession copySession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(creativeWorld))) {
			Iterator<BlockVector3> regionIterator = region.iterator();
			
			while(regionIterator.hasNext()) {
				BlockVector3 blockVector = regionIterator.next();
				if(blockVector.getBlockY()==0) {
					copySession.smartSetBlock(blockVector, bedrock.getDefaultState());
				} else if(blockVector.getBlockY()==1||blockVector.getBlockY()==2) {
					copySession.smartSetBlock(blockVector, dirt.getDefaultState());
				} else if(blockVector.getBlockY()==3) {
					copySession.smartSetBlock(blockVector, grass.getDefaultState());
				} else {
					copySession.smartSetBlock(blockVector, air.getDefaultState());
				}
			}
			copySession.getEntities(region).forEach(ent->
			{
				if(!ent.getState().getType().equals(BukkitAdapter.adapt(EntityType.PLAYER)))  ent.remove();
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
 

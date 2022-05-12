package com.HollowPlugins.HollowCreativePlots;

import org.bukkit.World;
import org.bukkit.util.Vector;

public class CleanupChunkEntry extends QueueEntry {

	public CleanupChunkEntry(World sourceWorld, World destinationWorld, Vector sourceCorner, Vector destinationCorner) {
		super(sourceWorld, destinationWorld, sourceCorner, destinationCorner);
	}

}

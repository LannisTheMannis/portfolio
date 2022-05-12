package com.HollowPlugins.HollowCreativePlots;

import org.bukkit.World;
import org.bukkit.util.Vector;

class DatabaseAdditionEntry extends QueueEntry{
	public DatabaseAdditionEntry(World sourceWorld, World destinationWorld, Vector sourceCorner, Vector destinationCorner) {
		super(sourceWorld, destinationWorld, sourceCorner, destinationCorner);
	}
	
	public DatabaseAdditionEntry(QueueEntry copy) {
		super(copy);
	}
}


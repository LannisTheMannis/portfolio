package com.HollowPlugins.HollowCreativePlots;

import java.util.List;

import org.bukkit.World;
import org.bukkit.util.Vector;

public class TransferBlockEntry extends QueueEntry {
	public List<DatabaseBlockEntry> entry;
	public TransferBlockEntry(World sourceWorld, World destinationWorld, Vector sourceCorner,
			Vector destinationCorner, List<DatabaseBlockEntry> entries) {
		super(sourceWorld, destinationWorld, sourceCorner, destinationCorner);
		this.entry=entries;
	}

}

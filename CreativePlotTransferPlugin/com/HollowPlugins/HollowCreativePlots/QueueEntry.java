package com.HollowPlugins.HollowCreativePlots;

import org.bukkit.World;
import org.bukkit.util.Vector;

class QueueEntry{
	protected World sourceWorld;
	protected World destinationWorld;
	protected Vector sourceCorner; //Always bottom left
	protected Vector destinationCorner;
	
	public QueueEntry(World sourceWorld, World destinationWorld, Vector sourceCorner, Vector destinationCorner) {
		this.sourceWorld = sourceWorld;
		this.destinationWorld = destinationWorld;
		this.sourceCorner = sourceCorner;
		this.destinationCorner = destinationCorner;
	}
	
	public QueueEntry(QueueEntry copy) {
		this.sourceWorld=copy.sourceWorld;
		this.destinationWorld=copy.destinationWorld;
		this.sourceCorner=copy.sourceCorner.clone();
		this.destinationCorner=copy.destinationCorner.clone();
	}
	
	public boolean validEntry() {
		boolean validWorld = (sourceWorld!=null)&&(destinationWorld!=null);
		return validWorld;
	}
}


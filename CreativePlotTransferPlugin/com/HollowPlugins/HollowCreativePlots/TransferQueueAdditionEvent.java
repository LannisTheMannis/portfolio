package com.HollowPlugins.HollowCreativePlots;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TransferQueueAdditionEvent extends Event{
	private int count;
	private static final HandlerList handlers = new HandlerList();

	public TransferQueueAdditionEvent(int count) {
		this.count=count;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	public static HandlerList getHandlerList() {
	    return handlers;
	}
}

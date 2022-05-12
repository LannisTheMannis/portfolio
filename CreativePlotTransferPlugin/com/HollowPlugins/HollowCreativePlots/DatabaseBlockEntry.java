package com.HollowPlugins.HollowCreativePlots;

import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

public class DatabaseBlockEntry {
	public int projectID=0;
	public Vector position=new Vector();
	public BlockData blockData;
	
	DatabaseBlockEntry(int id, Vector vect, BlockData data){
		projectID=id;
		position=vect.clone();
		blockData=data.clone();
	}
}

package com.HollowPlugins.HollowCreativePlots.commands;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.HollowPlugins.HollowCreativePlots.CreativePlot;
import com.HollowPlugins.HollowCreativePlots.HollowCreativePlots;
import com.hollowPlugins.HollowPluginBase.HollowPluginBase;
import com.hollowPlugins.HollowPluginBase.HollowPluginBaseAbstractCommandExecutor;
import com.hollowPlugins.HollowPluginBase.HollowPluginBaseCommandRequires;
import com.hollowPlugins.HollowPluginBase.HollowPluginBaseDoCommandData;

public class CleanWithoutMoving extends HollowPluginBaseAbstractCommandExecutor {

	private HollowCreativePlots _plugin;

	public CleanWithoutMoving(HollowPluginBase plugin) {
		super(plugin);
		_plugin = (HollowCreativePlots) plugin;
		require(new HollowPluginBaseCommandRequires(HollowPluginBaseCommandRequires.TYPE_PLAYER));
		require(new HollowPluginBaseCommandRequires(HollowPluginBaseCommandRequires.TYPE_PERMISSION,
				"hollowCreativePlots.admin"));
	}

	@Override
	public boolean execute(CommandSender sender, HollowPluginBaseDoCommandData data, String commandName, String label,
			String[] args) {
		Player player = (Player) sender;
		
		ArrayList<String> parsedArgs = _removeModifiers(args, "-");
		if(parsedArgs.size()==0) return true;
		try {
			int rowID = Integer.parseInt(parsedArgs.get(0));
			CreativePlot currentPlot = _plugin.getCreativePlot(rowID);
			if(currentPlot==null) {
				_plugin.log("No matching plot");
				return true;
			}
			currentPlot.cleanupCreativePlot();
		} catch(Exception e) {}

		return true;
	}

}
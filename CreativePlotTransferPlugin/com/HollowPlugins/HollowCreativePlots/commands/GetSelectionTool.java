package com.HollowPlugins.HollowCreativePlots.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.HollowPlugins.HollowCreativePlots.HollowCreativePlots;
import com.hollowPlugins.HollowPluginBase.HollowPluginBase;
import com.hollowPlugins.HollowPluginBase.HollowPluginBaseAbstractCommandExecutor;
import com.hollowPlugins.HollowPluginBase.HollowPluginBaseCommandRequires;
import com.hollowPlugins.HollowPluginBase.HollowPluginBaseDoCommandData;

public class GetSelectionTool extends HollowPluginBaseAbstractCommandExecutor {

	private HollowCreativePlots _plugin;

	public GetSelectionTool(HollowPluginBase plugin) {
		super(plugin);
		_plugin = (HollowCreativePlots) plugin;
		require(new HollowPluginBaseCommandRequires(HollowPluginBaseCommandRequires.TYPE_PLAYER));
		require(new HollowPluginBaseCommandRequires(HollowPluginBaseCommandRequires.TYPE_PERMISSION,
				"hollowCreativePlots.use"));
	}

	@Override
	public boolean execute(CommandSender sender, HollowPluginBaseDoCommandData data, String commandName, String label,
			String[] args) {
		Player player = (Player) sender;
		ItemStack wand = _plugin.getSelectionTool();
		if(player.getInventory().contains(wand)){
			player.sendMessage(_plugin.formatCommandResponse()+ChatColor.BLUE+"You already have a selection wand in your inventory!");
			return true;
		}
		
		if(player.getInventory().addItem(wand).size()>0) {
			player.sendMessage(_plugin.formatCommandResponse()+ChatColor.BLUE+"No space in inventory!");
		}
		

		return true;
	}

}
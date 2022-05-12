package com.HollowPlugins.HollowCreativePlots.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.HollowPlugins.HollowCreativePlots.HollowCreativePlots;
import com.hollowPlugins.HollowPluginBase.HollowPluginBase;
import com.hollowPlugins.HollowPluginBase.HollowPluginBaseAbstractCommandExecutor;
import com.hollowPlugins.HollowPluginBase.HollowPluginBaseCommandRequires;
import com.hollowPlugins.HollowPluginBase.HollowPluginBaseDoCommandData;

public class TransferRegion extends HollowPluginBaseAbstractCommandExecutor {

	private HollowCreativePlots _plugin;

	public TransferRegion(HollowPluginBase plugin) {
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
		
		if(!_plugin.getSelectionManager().finishSelection(player)) {
			player.sendMessage(_plugin.formatCommandResponse()+ChatColor.BLUE+"Failed to transfer!");
			return true;
		}
		
		player.sendMessage(_plugin.formatCommandResponse()+ChatColor.BLUE+"Successfully transfered region!");

		

		return true;
	}

}
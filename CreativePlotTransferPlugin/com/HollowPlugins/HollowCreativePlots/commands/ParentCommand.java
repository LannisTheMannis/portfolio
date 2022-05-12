package com.HollowPlugins.HollowCreativePlots.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import com.HollowPlugins.HollowCreativePlots.CreativePlot;
import com.HollowPlugins.HollowCreativePlots.HollowCreativePlots;
import com.hollowPlugins.HollowPluginBase.HollowPluginBase;
import com.hollowPlugins.HollowPluginBase.HollowPluginBaseCommandRequires;
import com.hollowPlugins.HollowPluginBase.HollowPluginBaseDoCommandData;
import com.hollowPlugins.HollowPluginBase.HollowPluginBaseTabCompleteCommandExecutor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.md_5.bungee.api.ChatColor;

public class ParentCommand extends HollowPluginBaseTabCompleteCommandExecutor {

	private HollowCreativePlots _plugin;

	public ParentCommand(HollowPluginBase plugin) {
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
		boolean isAdmin = player.hasPermission("hollowCreativePlots.admin");
		List<CreativePlot> ownedPlots = _plugin.getDatabaseManager().getPlayerPlots(player.getUniqueId().toString());
		if(ownedPlots.size()==0&& !isAdmin) {
			player.sendMessage(ChatColor.BLUE+"You don't have any creative plots!");
			return true;
		}
		
		player.sendMessage(ChatColor.BLUE+"=={ Owned Plots }==");
		for(CreativePlot plot:ownedPlots) {
			player.sendMessage(displayPlot(plot,isAdmin));
		}
		if(isAdmin) {
			player.sendMessage(ChatColor.GOLD+"=={ All Plots }==");
			List<CreativePlot> allPlots = _plugin.getDatabaseManager().getAllPlots();
			for(CreativePlot plot:allPlots) {
				if(plot.getOwnerUUID().equals(player.getUniqueId().toString()))continue;
				player.sendMessage(displayPlot(plot,isAdmin));
			}
		}

		return true;
	}
	
	private Component displayPlot(CreativePlot plot, boolean isAdmin) {
		Component message = Component.text(ChatColor.BLUE+"["+plot.getRowID()+"]"+"(Size: "+plot.getSpan()+") (Cost: "+plot.calculateDailyCost()+"r per day) \n");
		Component teleport = Component.text(ChatColor.BLUE+"    [Creative TP] ").clickEvent(ClickEvent.runCommand("/plot teleport "+plot.getRowID()));
		teleport=teleport.hoverEvent(Component.text(ChatColor.GRAY+"Click to teleport to this plot in creative").asHoverEvent());
		Component teleportMain = Component.text(ChatColor.BLUE+"[Main Map TP] ").clickEvent(ClickEvent.runCommand("/plot teleport "+plot.getRowID()+" m"));
		teleportMain=teleportMain.hoverEvent(Component.text(ChatColor.GRAY+"Click to teleport to this plot in the main map").asHoverEvent());
		Component delete = Component.text(ChatColor.BLUE+"[Transfer] ").clickEvent(ClickEvent.runCommand("/plot return "+plot.getRowID()));
		delete=delete.hoverEvent(Component.text(ChatColor.GRAY+"Click to move this plot back to the main map\n"+ChatColor.GRAY+"This does not delete the plot!").asHoverEvent());
		Component onlyDelete = Component.text(ChatColor.RED+"[Delete] ").clickEvent(ClickEvent.runCommand("/plot abandon "+plot.getRowID()));
		onlyDelete=onlyDelete.hoverEvent(Component.text(ChatColor.GRAY+"Click to delete this plot without transfer.\n"+ChatColor.GRAY+"Also removes build in creative."));

		message = message.append(teleport).append(teleportMain).append(delete).append(onlyDelete);
		if(isAdmin) {
			Component clean = Component.text(ChatColor.GOLD+"(Clean)").clickEvent(ClickEvent.runCommand("/plot clean "+plot.getRowID()));
			clean=clean.hoverEvent(Component.text(ChatColor.GRAY+"Click to remove build in creative. Does not delete plot."));
			message=message.append(clean);
		}
		return message;
	}

	@Override
	public @Nullable List<String> onTabComplete(CommandSender sender, Command command,
			 String label, String[] args) {
		List<String> tabs = new ArrayList<>();
		
		if(args.length==1) {
			for(String subcommand:_plugin.getSubCommands(command.getName())){
				if(_plugin.canPlayerExecuteSubCommand(sender, command.getName(), subcommand)) {
					tabs.add(subcommand);
				}
			}
			
			return tabs;
		}
		return null;
	}

}
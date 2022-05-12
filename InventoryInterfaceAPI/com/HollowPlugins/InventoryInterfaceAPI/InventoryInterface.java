package com.HollowPlugins.InventoryInterfaceAPI;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;

public class InventoryInterface{
	private Player player;
	private Inventory inventoryInterface;
	private InventoryType interfaceType;
	private int inventorySize;
	private String inventoryTitle;
	private HashMap<Integer, CallableFunction> registeredLeftClickButtons;
	private HashMap<Integer, CallableFunction> registeredRightClickButtons;

	private CallableFunction closingFunction=null;
	
	public InventoryInterface(Player _player, int size,String title) {
		player = _player;
		inventoryInterface=Bukkit.getServer().createInventory(null,size, Component.text(title));
		inventorySize = size;
		inventoryTitle = title;
		interfaceType = InventoryType.CHEST;
		registeredLeftClickButtons = new HashMap<>();
		registeredRightClickButtons = new HashMap<>();

	}
	public InventoryInterface(Player _player, InventoryType type, String title) {
		player = _player;
		inventoryInterface=Bukkit.getServer().createInventory(null,type, Component.text(title));
		inventoryTitle = title;
		interfaceType = type;
		registeredLeftClickButtons = new HashMap<>();
		registeredRightClickButtons = new HashMap<>();

		
	}
	
	public boolean isInterface(Inventory inv) {
		return this.inventoryInterface.equals(inv);
	}
	
	public void open() {
		player.openInventory(inventoryInterface);
	}
	
	public void clear() {
		registeredLeftClickButtons.clear();
		registeredRightClickButtons.clear();
//		closingFunction=null;
		inventoryInterface.clear();
	}
	
	public void setContents(ItemStack [] contents) {
		inventoryInterface.setContents(contents);
	}
	
	public void addButton(int index, ItemStack icon) {
		inventoryInterface.setItem(index, icon);
	}
	
	public ItemStack getButton(int index) {
		return inventoryInterface.getItem(index);
	}
	
	public void buildButton(int index, String name, List<String> lore) {
		if(inventoryInterface.getItem(index)==null) return;
		
		ItemMeta buttonMeta = inventoryInterface.getItem(index).getItemMeta();
		buttonMeta.displayName(Component.text(name));
		List<Component> componentLore = new ArrayList<>();
		if(lore!=null)lore.forEach(str->componentLore.add(Component.text(str)));
		buttonMeta.lore(componentLore);
		inventoryInterface.getItem(index).setItemMeta(buttonMeta);
	}
	
	public void fillWithPlaceholder() {
		ItemStack spaceFiller = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
		ItemMeta spaceMeta = spaceFiller.getItemMeta();
		spaceMeta.displayName(Component.text(" "));
		spaceFiller.setItemMeta(spaceMeta);
		fillWithPlaceholder(spaceFiller);
	}
	
	public void fillWithPlaceholder(ItemStack item) {
		for(int i=0;i<inventorySize;i++) {
			if(getButton(i)!=null) continue;
			addButton(i, item);
			registeredLeftClickButtons.put(i, null);
		}
	}
	
	public boolean isRegisteredButton(int index) {
		return registeredLeftClickButtons.containsKey(index)||registeredRightClickButtons.containsKey(index);
	}
	
	public void addLeftClickElement(int index, Object parentClass, Method clickFunction, Object clickParameters ) {
		registeredLeftClickButtons.put(index, new CallableFunction(parentClass,clickFunction,clickParameters));
	}
	
	public void addRightClickElement(int index, Object parentClass, Method clickFunction, Object clickParameters ) {
		registeredRightClickButtons.put(index, new CallableFunction(parentClass,clickFunction,clickParameters));
	}
	
	public void addClosingMethod(Object parentClass, Method closeFunction, Object closeParameters) {
		closingFunction = new CallableFunction(parentClass,closeFunction,closeParameters);
	}
	
	public boolean hasClosingMethod() {
		return closingFunction!=null;
	}
	
	public CallableFunction getClosingAction() {
		return closingFunction;
	}
	
	public CallableFunction getButtonLeftClickAction(int index) {
		return registeredLeftClickButtons.get(index);		
	}
	
	public CallableFunction getButtonRightClickAction(int index) {
		return registeredRightClickButtons.get(index);
	}
	
	class CallableFunction{
		private Method func;
		private Object params;
		private Object parentClass;
		public CallableFunction(Object parent, Method function, Object parameters) {
			func = function;
			params = parameters;
			parentClass = parent;
		}
		
		public void call() throws Exception {
			if(params!=null)
				func.invoke(parentClass, params);
			else
				func.invoke(parentClass);
		}
		
	}
}

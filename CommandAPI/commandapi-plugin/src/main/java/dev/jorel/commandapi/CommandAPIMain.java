package dev.jorel.commandapi;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import dev.jorel.commandapi.arguments.AdvancementArgument;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntityTypeArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.IntegerRangeArgument;
import dev.jorel.commandapi.arguments.ItemStackArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.wrappers.IntegerRange;

public class CommandAPIMain extends JavaPlugin implements Listener {
	
	@Override
	public void onLoad() {
		//Config loading
		saveDefaultConfig();
		CommandAPI.config = new Config(getConfig());
		CommandAPI.dispatcherFile = new File(getDataFolder(), "command_registration.json");
		CommandAPI.logger = getLogger();
		
		//Check dependencies for CommandAPI
		CommandAPIHandler.getInstance().checkDependencies();
		
		//Convert all plugins to be converted
		for(Entry<Plugin, String[]> pluginToConvert : CommandAPI.config.getPluginsToConvert()) {
			if(pluginToConvert.getValue().length == 0) {
				Converter.convert(pluginToConvert.getKey());
			} else {
				for(String command : pluginToConvert.getValue()) {
					new AdvancedConverter(pluginToConvert.getKey(), command).convert();
				}
			}
		}
		
		// Convert all arbitrary commands		
		for(String commandName : CommandAPI.config.getCommandsToConvert()) {
			new AdvancedConverter(commandName).convertCommand();
		}
	}
	
	@Override
	public void onEnable() {
		CommandAPI.onEnable(this);
		getServer().getPluginManager().registerEvents(this, this);
		
		/* TODO: Test stuff, do not use in production! */
		
		{
			List<Argument> arguments = new ArrayList<>();
			arguments.add(new IntegerRangeArgument("range"));
			arguments.add(new ItemStackArgument("item"));

			new CommandAPICommand("searchrange")
			    .withArguments(arguments)
			    .executesPlayer((player, args) -> {
			        // Retrieve the range from the arguments
			        IntegerRange range = (IntegerRange) args[0];
			        ItemStack itemStack = (ItemStack) args[1];

			        // Store the locations of chests with certain items
			        List<Location> locations = new ArrayList<>();

			        // Iterate through all chunks, and then all tile entities within each chunk
			        for(Chunk chunk : player.getWorld().getLoadedChunks()) {
			            for(BlockState blockState : chunk.getTileEntities()) {

			                // The distance between the block and the player
			                int distance = (int) blockState.getLocation().distance(player.getLocation());

			                // Check if the distance is within the specified range 
			                if(range.isInRange(distance)) {

			                    // Check if the tile entity is a chest
			                    if(blockState instanceof Chest) {
			                        Chest chest = (Chest) blockState;

			                        // Check if the chest contains the item specified by the player
			                        if(chest.getInventory().contains(itemStack.getType())) {
			                            locations.add(chest.getLocation());
			                        }
			                    }
			                }

			            }
			        }

			        // Output the locations of the chests, or whether no chests were found
			        if(locations.isEmpty()) {
			            player.sendMessage("No chests were found");
			        } else {
			            player.sendMessage("Found " + locations.size() + " chests:");
			            locations.forEach(location -> {
			                player.sendMessage("  Found at: " 
			                        + location.getX() + ", " 
			                        + location.getY() + ", " 
			                        + location.getZ());
			            });
			        }
			    })
			    .register();
		}
		
		{
			/* ANCHOR: locationarguments */
			new CommandAPICommand("break")
			    //We want to target blocks in particular, so use BLOCK_POSITION
			    .withArguments(new LocationArgument("block", LocationType.BLOCK_POSITION))
			    .executesPlayer((player, args) -> {
			        ((Location) args[0]).getBlock().setType(Material.AIR);
			    })
			    .register();
			/* ANCHOR_END: locationarguments */
			}
		
		{
			/* ANCHOR: entitytypearguments */
			new CommandAPICommand("spawnmob")
			    .withArguments(new EntityTypeArgument("entity"))
			    .withArguments(new IntegerArgument("amount", 1, 100)) //Prevent spawning too many entities
			    .executesPlayer((Player player, Object[] args) -> {
			        for(int i = 0; i < (int) args[1]; i++) {
			            player.getWorld().spawnEntity(player.getLocation(), (EntityType) args[0]);
			        }
			    })
			    .register();
			/* ANCHOR_END: entitytypearguments */
			}
		
		{
			/* ANCHOR: advancementarguments */
			new CommandAPICommand("award")
			    .withArguments(new PlayerArgument("player"))
			    .withArguments(new AdvancementArgument("advancement"))
			    .executes((sender, args) -> {
			        Player target = (Player) args[0];
			        Advancement advancement = (Advancement) args[1];
			        
			        //Award all criteria for the advancement
			        AdvancementProgress progress = target.getAdvancementProgress(advancement);
			        for(String criteria : advancement.getCriteria()) {
			            progress.awardCriteria(criteria);
			        }
			    })
			    .register();
			/* ANCHOR_END: advancementarguments */
			}
	}
}

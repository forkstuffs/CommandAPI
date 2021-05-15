package dev.jorel.commandapi.nms;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.advancement.Advancement;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.loot.LootTable;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.destroystokyo.paper.brigadier.BukkitBrigadierCommandSource;
import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;

import de.tr7zw.changeme.nbtapi.NBTContainer;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.ICustomProvidedArgument.SuggestionProviders;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.wrappers.FloatRange;
import dev.jorel.commandapi.wrappers.FunctionWrapper;
import dev.jorel.commandapi.wrappers.IntegerRange;
import dev.jorel.commandapi.wrappers.Location2D;
import dev.jorel.commandapi.wrappers.MathOperation;
import dev.jorel.commandapi.wrappers.NativeProxyCommandSender;
import dev.jorel.commandapi.wrappers.Rotation;
import dev.jorel.commandapi.wrappers.ScoreboardSlot;
import dev.jorel.commandapi.wrappers.SimpleFunctionWrapper;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.chat.BaseComponent;

public interface NMS {

	/**
	 * Resends the command dispatcher's set of commands to a player.
	 * 
	 * @param player the player to send the command graph packet to
	 */
	void resendPackets(Player player);

	/**
	 * A String array of Minecraft versions that this NMS implementation is
	 * compatible with. For example, ["1.14", "1.14.1", "1.14.2", "1.14.3"]. This
	 * can be found by opening a Minecraft jar file, viewing the version.json file
	 * and reading the object "name".
	 * 
	 * @return A String array of compatible Minecraft versions
	 */
	String[] compatibleVersions();

	/**
	 * Creates a JSON file that describes the hierarchical structure of the commands
	 * that have been registered by the server.
	 * 
	 * @param file       The JSON file to write to
	 * @param dispatcher The Brigadier CommandDispatcher
	 * @throws IOException When the file fails to be written to
	 */
	void createDispatcherFile(File file, CommandDispatcher<BukkitBrigadierCommandSource> dispatcher) throws IOException;

	record Completion(String suggestion, Component tooltip) {
		public AsyncTabCompleteEvent.Completion toCompletion() {
			return new AsyncTabCompleteEvent.Completion() {

				@Override
				public @NotNull String suggestion() {
					return suggestion;
				}

				@Override
				public @Nullable Component tooltip() {
					return tooltip;
				}
				
			};
		}
	};
	
	/**
	 * Retrieve a specific NMS implemented SuggestionProvider
	 * 
	 * @param provider The SuggestionProvider type to retrieve
	 * @return A SuggestionProvider that matches the SuggestionProviders input
	 */
	default SuggestionProvider<BukkitBrigadierCommandSource> getSuggestionProvider(SuggestionProviders provider) {
		
		// TODO: This mess
		switch (provider) {
		case FUNCTION:
//			return (context, builder) -> {
//				CustomFunctionData functionData = getCLW(context).getServer().getFunctionData();
//				ICompletionProvider.a(functionData.g(), builder, "#");
//				return ICompletionProvider.a(functionData.f(), builder);
//			};
		case RECIPES:
//			return CompletionProviders.b;
		case SOUNDS:
//			return CompletionProviders.c;
		case ADVANCEMENTS:
			List<AsyncTabCompleteEvent.Completion> completions = new ArrayList<>();
			Bukkit.getServer().advancementIterator().forEachRemaining(a -> {
				completions.add(new Completion(a.getKey().toString(), null).toCompletion());
			});
			return (cmdCtx, builder) -> {
				String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
				for (int i = 0; i < completions.size(); i++) {
					AsyncTabCompleteEvent.Completion str = completions.get(i);
					if (str.suggestion().toLowerCase(Locale.ROOT).startsWith(remaining)) {
						Message tooltipMsg = null;
						if(str.tooltip() != null) {
							tooltipMsg = new LiteralMessage(PaperAdventure.PLAIN.serialize(str.tooltip()));
						}
						builder.suggest(str.suggestion(), tooltipMsg);
					}
				}
				return builder.buildFuture();
			};
		case LOOT_TABLES:
//			return (context, builder) -> {
//				LootTableRegistry lootTables = getCLW(context).getServer().getLootTableRegistry();
//				return ICompletionProvider.a(lootTables.a(), builder);
//			};
		case BIOMES:
//			return CompletionProviders.d;
		case ENTITIES:
//			return CompletionProviders.e;
		default:
			return (context, builder) -> Suggestions.empty();
		}
	}

	/**
	 * Retrieves a CommandSender, given some CommandContext. This method should
	 * handle Proxied CommandSenders for entities if a Proxy is being used.
	 * 
	 * @param cmdCtx      The
	 *                    <code>CommandContext&lt;BukkitBrigadierCommandSource&gt;</code>
	 *                    for a given command
	 * @param forceNative whether or not the CommandSender should be a
	 *                    NativeProxyCommandSender or not
	 * @return A CommandSender instance (such as a ProxiedNativeCommandSender or
	 *         Player)
	 */
	default CommandSender getSenderForCommand(CommandContext<BukkitBrigadierCommandSource> cmdCtx, boolean forceNative) {
		BukkitBrigadierCommandSource commandSource = cmdCtx.getSource();

		CommandSender sender = commandSource.getBukkitSender();
		Location location = commandSource.getBukkitLocation();

		Entity proxyEntity = commandSource.getBukkitEntity();
		CommandSender proxy = proxyEntity;
		if (forceNative || (proxy != null && !sender.equals(proxy))) {
			sender = new NativeProxyCommandSender(sender, proxy, location, commandSource.getBukkitWorld());
		}

		return sender;
	}

	/**
	 * Returns a CommandSender of a given BukkitBrigadierCommandSource object
	 * 
	 * @param clw The BukkitBrigadierCommandSource object
	 * @return A CommandSender (not proxied) from the command listener wrapper
	 */
	@Deprecated
	default CommandSender getCommandSenderForCLW(BukkitBrigadierCommandSource clw) {
		return clw.getBukkitSender();
	}

	/**
	 * Converts a CommandSender into a CLW
	 * 
	 * @param sender the command sender to convert
	 * @return a CLW.
	 */
	default BukkitBrigadierCommandSource getCLWFromCommandSender(CommandSender sender) {
		return new BukkitBrigadierCommandSource() {

			@Override
			public @Nullable Entity getBukkitEntity() {
				if(sender instanceof Entity entity) {
					return entity;
				} else {
					return null;
				}
			}

			@Override
			public @Nullable World getBukkitWorld() {
				if(sender instanceof Entity entity) {
					return entity.getWorld();
				} else {
					return null;
				}
			}

			@Override
			public @Nullable Location getBukkitLocation() {
				switch(sender) {
				
				}
				if(sender instanceof BlockCommandSender s) {
					return s.getBlock().getLocation();
				} else if(sender instanceof Entity entity) {
					return entity.getLocation();
				} else {
					return null;
				}
				
				if (sender instanceof Player) {
					return ((CraftPlayer) sender).getHandle().getCommandListener();
				} else if (sender instanceof BlockCommandSender) {
					return ((CraftBlockCommandSender) sender).getWrapper();
				} else if (sender instanceof CommandMinecart) {
					return ((CraftMinecartCommand) sender).getHandle().getCommandBlock().getWrapper();
				} else if (sender instanceof RemoteConsoleCommandSender) {
					return ((DedicatedServer) MinecraftServer.getServer()).remoteControlCommandListener.getWrapper();
				} else if (sender instanceof ConsoleCommandSender) {
					return ((CraftServer) sender.getServer()).getServer().getServerCommandListener();
				} else if (sender instanceof ProxiedCommandSender) {
					return ((ProxiedNativeCommandSender) sender).getHandle();
				} else {
					throw new IllegalArgumentException("Cannot make " + sender + " a vanilla command listener");
				}
			}

			@Override
			public CommandSender getBukkitSender() {
				return sender;
			}
		};
	}

	/**
	 * Returns the Brigadier CommandDispatcher from the NMS CommandDispatcher
	 * 
	 * @return A Brigadier CommandDispatcher
	 */
	CommandDispatcher<BukkitBrigadierCommandSource> getBrigadierDispatcher();

	/**
	 * Checks if a Command is an instance of the OBC VanillaCommandWrapper
	 * 
	 * @param command The Command to check
	 * @return true if Command is an instance of VanillaCommandWrapper
	 */
	boolean isVanillaCommandWrapper(Command command);

	/**
	 * Reloads the datapacks by using the updated the commandDispatcher tree
	 * 
	 * @throws SecurityException        reflection exception
	 * @throws NoSuchFieldException     reflection exception
	 * @throws IllegalAccessException   reflection exception
	 * @throws IllegalArgumentException reflection exception
	 */
	default void reloadDataPacks()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
	};

	/* Argument implementations with CommandSyntaxExceptions */
	Advancement getAdvancement(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key) throws CommandSyntaxException;

	Predicate<Block> getBlockPredicate(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key)
			throws CommandSyntaxException;
	
	Component getAdventureChat(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key) throws CommandSyntaxException;

	BaseComponent[] getChat(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key) throws CommandSyntaxException;

	Environment getDimension(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key) throws CommandSyntaxException;

	ItemStack getItemStack(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key) throws CommandSyntaxException;

	Object getEntitySelector(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key, EntitySelector selector)
			throws CommandSyntaxException;

	EntityType getEntityType(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key) throws CommandSyntaxException;

	FunctionWrapper[] getFunction(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key)
			throws CommandSyntaxException;

	Predicate<ItemStack> getItemStackPredicate(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key)
			throws CommandSyntaxException;

	String getKeyedAsString(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key) throws CommandSyntaxException;

	Location getLocation(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key, LocationType locationType)
			throws CommandSyntaxException;

	Location2D getLocation2D(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key, LocationType locationType2d)
			throws CommandSyntaxException;

	String getObjective(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key)
			throws IllegalArgumentException, CommandSyntaxException;

	Player getPlayer(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key) throws CommandSyntaxException;

	PotionEffectType getPotionEffect(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key)
			throws CommandSyntaxException;

	Recipe getRecipe(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key) throws CommandSyntaxException;

	Collection<String> getScoreHolderMultiple(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key)
			throws CommandSyntaxException;

	String getScoreHolderSingle(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key)
			throws CommandSyntaxException;

	String getTeam(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key) throws CommandSyntaxException;

	MathOperation getMathOperation(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key)
			throws CommandSyntaxException;

	/* Argument implementations without CommandSyntaxExceptions */
	float getAngle(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key);

	EnumSet<Axis> getAxis(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key);

	Biome getBiome(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key);

	BlockData getBlockState(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key);

	ChatColor getChatColor(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key);
	
	Component getAdventureChatComponent(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key);

	BaseComponent[] getChatComponent(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key);

	Enchantment getEnchantment(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key);

	FloatRange getFloatRange(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key);

	IntegerRange getIntRange(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key);

	LootTable getLootTable(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key);

	NBTContainer getNBTCompound(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key);

	String getObjectiveCriteria(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key);

	Particle getParticle(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key);

	Rotation getRotation(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key);

	ScoreboardSlot getScoreboardSlot(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key);

	Sound getSound(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key);

	int getTime(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key);

	UUID getUUID(CommandContext<BukkitBrigadierCommandSource> cmdCtx, String key);

	/* Argument types */
	ArgumentType<?> _ArgumentAngle();

	ArgumentType<?> _ArgumentAxis();

	ArgumentType<?> _ArgumentBlockPredicate();

	ArgumentType<?> _ArgumentBlockState();

	ArgumentType<?> _ArgumentChat();

	ArgumentType<?> _ArgumentChatFormat();

	ArgumentType<?> _ArgumentChatComponent();

	ArgumentType<?> _ArgumentDimension();

	ArgumentType<?> _ArgumentEntity(EntitySelector selector);

	ArgumentType<?> _ArgumentEntitySummon();

	ArgumentType<?> _ArgumentEnchantment();

	ArgumentType<?> _ArgumentFloatRange();

	ArgumentType<?> _ArgumentIntRange();

	ArgumentType<?> _ArgumentItemPredicate();

	ArgumentType<?> _ArgumentItemStack();

	ArgumentType<?> _ArgumentMinecraftKeyRegistered();

	ArgumentType<?> _ArgumentMathOperation();

	ArgumentType<?> _ArgumentMobEffect();

	ArgumentType<?> _ArgumentNBTCompound();

	ArgumentType<?> _ArgumentProfile();

	ArgumentType<?> _ArgumentParticle();

	ArgumentType<?> _ArgumentPosition();

	ArgumentType<?> _ArgumentPosition2D();

	ArgumentType<?> _ArgumentRotation();

	ArgumentType<?> _ArgumentScoreboardCriteria();

	ArgumentType<?> _ArgumentScoreboardObjective();

	ArgumentType<?> _ArgumentScoreboardSlot();

	ArgumentType<?> _ArgumentScoreboardTeam();

	ArgumentType<?> _ArgumentScoreholder(boolean single);

	ArgumentType<?> _ArgumentTag();

	ArgumentType<?> _ArgumentTime();

	ArgumentType<?> _ArgumentUUID();

	ArgumentType<?> _ArgumentVec2();

	ArgumentType<?> _ArgumentVec3();

	String convert(ItemStack is);

	String convert(Particle particle);

	String convert(PotionEffectType potion);

	String convert(Sound sound);

	SimpleFunctionWrapper[] getTag(NamespacedKey key);

	SimpleFunctionWrapper getFunction(NamespacedKey key);

	Set<NamespacedKey> getFunctions();

	Set<NamespacedKey> getTags();

}

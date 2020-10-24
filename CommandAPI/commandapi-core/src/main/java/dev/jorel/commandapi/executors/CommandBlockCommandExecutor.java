package dev.jorel.commandapi.executors;

import org.bukkit.command.BlockCommandSender;

import dev.jorel.commandapi.Arguments;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

/**
 * A normal command executor for a BlockCommandSender
 */
@FunctionalInterface
public interface CommandBlockCommandExecutor extends IExecutorNormal<BlockCommandSender> {

	/**
	 * The code to run when this command is performed
	 * 
	 * @param sender
	 *            The sender of this command (a player, the console etc.)
	 */
	void run(BlockCommandSender sender, Arguments args) throws WrapperCommandSyntaxException;

	@Override
	default ExecutorType getType() {
		return ExecutorType.BLOCK;
	}
}

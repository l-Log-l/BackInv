package org.log;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.nbt.*;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class BackInv implements ModInitializer {
	public static final String MOD_ID = "backinv";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final Config config = new Config();

	public static final String BACKINV_COMMAND = "backinv";

	@Override
	public void onInitialize() {
		// Инициализация конфигурации
		Config.init();

		ServerPlayerEvents.ALLOW_DEATH.register((player, damageSource, damageAmount) -> {
			savePlayerInventory(player);
			return true;
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(literal(BACKINV_COMMAND)
					.requires(source -> source.hasPermissionLevel(2))
					.then(literal("list")
							.then(argument("player", StringArgumentType.word())
									.executes(this::listInventories)))
					.then(literal("load")
							.then(argument("player", StringArgumentType.word())
									.then(argument("save", StringArgumentType.word())
											.executes(this::loadInventory)))));
		});
	}
	private void savePlayerInventory(ServerPlayerEntity player) {
		String playerName = player.getName().getString();
		try {
			// Create an NBT compound to store inventory data
			NbtCompound nbt = new NbtCompound();

			// Serialize the player's inventory to NBT

			player.writeNbt(nbt);
			// Create a timestamp
			String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH.mm.ss dd-MM-yyyy"));
			nbt.putString("Health", "20");
			nbt.putString("abilities", "{}");
			nbt.putString("Motion", "[0.0d,0.2d,0.0d]");
			// Write the NBT compound to a file
			Path savePath = getBackupPath(playerName);
			Files.createDirectories(savePath);
			Files.writeString(savePath.resolve(timestamp + ".nbt"), nbt.toString());

			LOGGER.debug("Saved inventory for player {} to file {}", playerName, timestamp);

		} catch (Exception e) {
			LOGGER.error("Error saving inventory", e);
		}
	}


	private Path getBackupPath(String playerName) {
		return FabricLoader.getInstance().getConfigDir()
				.resolve(MOD_ID)
				.resolve("backups")
				.resolve(playerName);
	}

	private void loadPlayerInventory(ServerPlayerEntity player, String timestamp) {
		try {
			Path savePath = getBackupPath(player.getName().getString()).resolve(timestamp + ".nbt");
			if (!Files.exists(savePath)) {
				throw new IOException("Save file not found");
			}

			String nbtData = Files.readString(savePath);
			NbtCompound loadedNbt = StringNbtReader.parse(nbtData);

			player.sendMessage(Text.literal(config.prefix + " " + config.loadPlayerMessage.replace("{player}", player.getName().toString()).replace("{file}", timestamp)));

			player.readNbt(loadedNbt);
			player.networkHandler.disconnect(Text.literal(config.reJoinMessage));
			LOGGER.info("Successfully restored inventory for player {}", player.getName());
		} catch (IOException | CommandSyntaxException e) {
			LOGGER.error("Error loading inventory", e);
			player.sendMessage(Text.literal(config.prefix + " " + config.errorLoadPlayerMessage.replace("{player}", player.getName().toString()).replace("{file}", timestamp).replace("{e}", e.toString())));
		}
	}



	private int listInventories(CommandContext<ServerCommandSource> context) {
		try {
			String playerName = StringArgumentType.getString(context, "player");
			List<String> saves = getPlayerSaves(playerName);

			if (saves.isEmpty()) {
				context.getSource().sendFeedback(() -> Text.literal(config.prefix + " " + config.noSavesFound.replace("{player}", playerName)), false);
				return 0;
			}

			context.getSource().sendFeedback(
					() -> Text.literal(config.prefix + " " + playerName + config.suffix),
					false
			);

			for (int i = 0; i < saves.size(); i++) {
				String save = saves.get(i);
				String formattedDate = save; // Используем сохраненный формат даты
				final int index = i + 1;
				context.getSource().sendFeedback(
						() -> Text.literal(config.itemPrefix + index + ". " + config.itemSuffix + formattedDate),
						false
				);
			}

			return 1;
		} catch (Exception e) {
			LOGGER.error("Error listing inventories", e);
			context.getSource().sendError(Text.literal(config.errorOccurred.replace("{action}", "listing inventories")));
			return 0;
		}
	}



	private int loadInventory(CommandContext<ServerCommandSource> context) {
		try {
			String playerName = StringArgumentType.getString(context, "player");
			String saveIndex = StringArgumentType.getString(context, "save");

			ServerPlayerEntity targetPlayer = context.getSource().getServer()
					.getPlayerManager().getPlayer(playerName);

			if (targetPlayer == null) {
				context.getSource().sendError(Text.literal(config.saveNotFound.replace("{player}", playerName)));
				return 0;
			}

			List<String> saves = getPlayerSaves(playerName);
			if (saves.isEmpty()) {
				context.getSource().sendError(Text.literal(config.noSavesFound.replace("{player}", playerName)));
				return 0;
			}

			String selectedSave;
			try {
				int index = Integer.parseInt(saveIndex) - 1;
				if (index >= 0 && index < saves.size()) {
					selectedSave = saves.get(index);
				} else {
					context.getSource().sendError(Text.literal(config.invalidIndex.replace("{index}", saveIndex)));
					return 0;
				}
			} catch (NumberFormatException e) {
				if (saves.contains(saveIndex)) {
					selectedSave = saveIndex;
				} else {
					context.getSource().sendError(Text.literal(config.saveNotFound.replace("{save}", saveIndex)));
					return 0;
				}
			}

			loadPlayerInventory(targetPlayer, selectedSave);
			return 1;
		} catch (Exception e) {
			LOGGER.error("Error loading inventory", e);
			context.getSource().sendError(Text.literal(config.errorOccurred.replace("{action}", "loading inventory")));
			return 0;
		}
	}




	private List<String> getPlayerSaves(String playerName) {
		try {
			Path savePath = getBackupPath(playerName);
			if (!Files.exists(savePath)) {
				return new ArrayList<>();
			}

			List<String> saves = new ArrayList<>();
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(savePath, "*.nbt")) {
				for (Path path : stream) {
					String filename = path.getFileName().toString();
					saves.add(filename.substring(0, filename.length() - 4));
				}
			}
			saves.sort(Comparator.reverseOrder());
			return saves;
		} catch (IOException e) {
			LOGGER.error("Error getting player saves", e);
			return new ArrayList<>();
		}
	}
}

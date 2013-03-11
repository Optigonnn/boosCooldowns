package cz.boosik.boosCooldown;

import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import util.boosChat;

public class boosCoolDownListener<a> implements Listener {
	private final boosCoolDown plugin;
	private boolean blocked = false;
	public static ConcurrentHashMap<Player, Location> playerloc = new ConcurrentHashMap<Player, Location>();
	public static ConcurrentHashMap<Player, String> playerworld = new ConcurrentHashMap<Player, String>();

	public static void clearLocWorld(Player player) {
		boosCoolDownListener.playerloc.remove(player);
		boosCoolDownListener.playerworld.remove(player);
	}

	public boosCoolDownListener(boosCoolDown instance) {
		plugin = instance;
	}

	private boolean blocked(Player player, String pre, String msg) {
		int limit = -1;
		int uses = boosCoolDownManager.getUses(player, pre, msg);
		if (player.hasPermission("booscooldowns.nolimit")
				|| player.hasPermission("booscooldowns.nolimit." + pre)) {
		} else {
			if (player.hasPermission("booscooldowns.limit2")) {
				limit = boosConfigManager.getLimit2(pre);
				if (limit == -1) {
					return false;
				} else if (limit <= uses) {
					return true;
				}
			} else if (player.hasPermission("booscooldowns.limit3")) {
				limit = boosConfigManager.getLimit3(pre);
				if (limit == -1) {
					return false;
				} else if (limit <= uses) {
					return true;
				}
			} else if (player.hasPermission("booscooldowns.limit4")) {
				limit = boosConfigManager.getLimit4(pre);
				if (limit == -1) {
					return false;
				} else if (limit <= uses) {
					return true;
				}
			} else if (player.hasPermission("booscooldowns.limit5")) {
				limit = boosConfigManager.getLimit5(pre);
				if (limit == -1) {
					return false;
				} else if (limit <= uses) {
					return true;
				}
			} else {
				limit = boosConfigManager.getLimit(pre);
				if (limit == -1) {
					return false;
				} else if (limit <= uses) {
					return true;
				}
			}
		}
		return false;
	}

	// Returns true if the command is on cooldown, false otherwise
	private void checkCooldown(PlayerCommandPreprocessEvent event,
			Player player, String pre, String message, int warmUpSeconds,
			int price) {
		if (!blocked) {
			if (warmUpSeconds > 0) {
				if (!player.hasPermission("booscooldowns.nowarmup")
						&& !player.hasPermission("booscooldowns.nowarmup."
								+ pre)) {
					start(event, player, pre, message, warmUpSeconds);
				}
			} else {
				if (boosCoolDownManager.coolDown(player, pre)) {
					event.setCancelled(true);
				}
			}
			if (!event.isCancelled()) {
				payForCommand(event, player, pre, price);
			}
		} else {
			event.setCancelled(true);
			String msg = String.format(boosConfigManager
					.getCommandBlockedMessage());
			boosChat.sendMessageToPlayer(player, msg);
		}
		if (!event.isCancelled()) {
			boosCoolDownManager.setUses(player, pre, message);
			if (boosConfigManager.getCommandLogging()) {
				boosCoolDown.commandLogger(player.getName(), pre + message);
			}
		}
	}

	private boolean isPluginOnForPlayer(Player player) {
		boolean on;
		if (player.isOp()) {
			on = false;
		}
		if (player.hasPermission("booscooldowns.exception")) {
			on = false;
		} else if (player.isOp()) {
			on = false;
		} else {
			on = true;
		}
		return on;
	}

	@EventHandler(priority = EventPriority.LOW)
	private void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (event.isCancelled()) {
			return;
		}
		ConfigurationSection aliases = boosConfigManager.getAliases();
		String message = event.getMessage();
		try {
			if (aliases.contains(message)) {
				message = boosConfigManager.getAlias(message);
				event.setMessage(message);
			}
		} catch (NullPointerException e) {
			boosCoolDown
					.getLog()
					.warning(
							"Aliases section in config.yml is missing! Please delete your config.yml, restart server and set it again!");
		}

		message = message.trim().replaceAll(" +", " ");
		Player player = event.getPlayer();
		boolean on = true;
		on = isPluginOnForPlayer(player);

		if (on) {
			boolean used = false;
			String messageCommand = "";
			String preSub = "";
			String preSub2 = "";
			String preSub3 = "";
			String messageSub = "";
			String messageSub2 = "";
			String messageSub3 = "";
			int preSubCheck = -1;
			int preSubCheck2 = -1;
			int preSubCheck3 = -1;
			int price = 0;
			int limit = 0;
			int cd = 0;
			playerloc.put(player, player.getLocation());
			playerworld.put(player, player.getWorld().getName());
			String[] splitCommand;
			splitCommand = message.split(" ");
			String preCommand = splitCommand[0];
			if (splitCommand.length > 1) {
				for (int i = 1; i < splitCommand.length; i++) {
					messageCommand = messageCommand + " " + splitCommand[i];
				}
			}
			if (splitCommand.length > 1) {
				preSub = splitCommand[0] + " " + splitCommand[1];
				for (int i = 2; i < splitCommand.length; i++) {
					messageSub = messageSub + " " + splitCommand[i];
				}
			}
			if (splitCommand.length > 2) {
				preSub2 = splitCommand[0] + " " + splitCommand[1] + " "
						+ splitCommand[2];
				for (int i = 3; i < splitCommand.length; i++) {
					messageSub2 = messageSub2 + " " + splitCommand[i];
				}
			}
			if (splitCommand.length > 3) {
				preSub3 = splitCommand[0] + " " + splitCommand[1] + " "
						+ splitCommand[2] + " " + splitCommand[3];
				for (int i = 4; i < splitCommand.length; i++) {
					messageSub3 = messageSub3 + " " + splitCommand[i];
				}
			}
			if (preSub3.length() > 0) {
				if (preSub3 != null) {
					preSubCheck3 = preSubCheck(player, preSub3);
					if (preSubCheck3 < 0) {
						price = prePriceCheck(player, preSub3);
						cd = preCDCheck(player, preSub3);
						limit = preLimitCheck(player, preSub3);
						if (cd > 0) {
							preSubCheck3 = 0;
						} else if (price > 0) {
							preSubCheck3 = 0;
						} else if (limit > 0) {
							preSubCheck3 = 0;
						}
					}
				}
			}
			if (preSub2.length() > 0) {
				if (preSub2 != null && preSubCheck3 < 0) {
					preSubCheck2 = preSubCheck(player, preSub2);
					if (preSubCheck2 < 0) {
						price = prePriceCheck(player, preSub2);
						cd = preCDCheck(player, preSub2);
						limit = preLimitCheck(player, preSub2);
						if (cd > 0) {
							preSubCheck2 = 0;
						} else if (price > 0) {
							preSubCheck2 = 0;
						} else if (limit > 0) {
							preSubCheck2 = 0;
						}
					}
				}
			}
			if (preSub.length() > 0) {
				if (preSub.length() < 1 || preSub != null && preSubCheck2 < 0) {
					preSubCheck = preSubCheck(player, preSub);
					if (preSubCheck < 0) {
						price = prePriceCheck(player, preSub);
						cd = preCDCheck(player, preSub);
						limit = preLimitCheck(player, preSub);
						if (cd > 0) {
							preSubCheck = 0;
						} else if (price > 0) {
							preSubCheck = 0;
						} else if (limit > 0) {
							preSubCheck = 0;
						}
					}
				}
			}
			if (preSubCheck3 >= 0) {
				blocked = blocked(player, preSub3, messageSub3);
				this.checkCooldown(event, player, preSub3, messageSub3,
						preSubCheck3, price);
				used = true;
			} else if (preSubCheck2 >= 0) {
				blocked = blocked(player, preSub2, messageSub2);
				this.checkCooldown(event, player, preSub2, messageSub2,
						preSubCheck2, price);
				used = true;
			} else if (preSubCheck >= 0) {
				blocked = blocked(player, preSub, messageSub);
				this.checkCooldown(event, player, preSub, messageSub,
						preSubCheck, price);
				used = true;
			} else {
				blocked = blocked(player, preCommand, messageCommand);
				int preCmdCheck = preSubCheck(player, preCommand);
				price = prePriceCheck(player, preCommand);
				this.checkCooldown(event, player, preCommand, messageCommand,
						preCmdCheck, price);
				used = true;
			}

			if (!used) {
				blocked = blocked(player, preCommand, messageCommand);
				int preCmdCheck = preSubCheck(player, preCommand);
				price = prePriceCheck(player, preCommand);
				this.checkCooldown(event, player, preCommand, messageCommand,
						preCmdCheck, price);
				used = false;
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	private void onPlayerChat(AsyncPlayerChatEvent event) {
		String chatMessage = event.getMessage();
		String temp = "globalchat";
		int price = 0;
		Player player = event.getPlayer();
		if (chatMessage.startsWith("!")) {
			if (!boosCoolDownManager.checkCoolDownOK(player, temp, chatMessage)) {
				event.setCancelled(true);
				return;
			} else {
				if (boosCoolDownManager.coolDown(player, temp)) {
					event.setCancelled(true);
					return;
				}
			}
			price = prePriceCheck(player, temp);
			payForCommand2(event, player, temp, price);
		}
	}

	private void payForCommand(PlayerCommandPreprocessEvent event,
			Player player, String pre, int price) {
		String name = player.getName();
		if (price > 0) {
			if (!player.hasPermission("booscooldowns.noprice")
					&& !player.hasPermission("booscooldowns.noprice." + pre)) {
				if (boosPriceManager.payForCommand(player, pre, price, name)) {
					return;
				} else {
					boosCoolDownManager.cancelCooldown(player, pre);
					event.setCancelled(true);
					return;
				}
			}
		}
	}

	private void payForCommand2(AsyncPlayerChatEvent event, Player player,
			String pre, int price) {
		String name = player.getName();
		if (price > 0) {
			if (!player.hasPermission("booscooldowns.noprice")
					&& !player.hasPermission("booscooldowns.noprice." + pre)) {
				if (boosPriceManager.payForCommand(player, pre, price, name)) {
					return;
				} else {
					boosCoolDownManager.cancelCooldown(player, pre);
					event.setCancelled(true);
					return;
				}
			}
		}
	}

	private int preCDCheck(Player player, String preSub) {
		if (player.hasPermission("booscooldowns.cooldown2")) {
			return boosConfigManager.getCoolDown2(preSub);
		} else if (player.hasPermission("booscooldowns.cooldown3")) {
			return boosConfigManager.getCoolDown3(preSub);
		} else if (player.hasPermission("booscooldowns.cooldown4")) {
			return boosConfigManager.getCoolDown4(preSub);
		} else if (player.hasPermission("booscooldowns.cooldown5")) {
			return boosConfigManager.getCoolDown5(preSub);
		} else {
			return boosConfigManager.getCoolDown(preSub);
		}
	}

	private int preLimitCheck(Player player, String preSub) {
		if (player.hasPermission("booscooldowns.limit2")) {
			return boosConfigManager.getLimit2(preSub);
		} else if (player.hasPermission("booscooldowns.limit3")) {
			return boosConfigManager.getLimit3(preSub);
		} else if (player.hasPermission("booscooldowns.limit4")) {
			return boosConfigManager.getLimit4(preSub);
		} else if (player.hasPermission("booscooldowns.limit5")) {
			return boosConfigManager.getLimit5(preSub);
		} else {
			return boosConfigManager.getLimit(preSub);
		}
	}

	private int prePriceCheck(Player player, String preSub) {
		if (player.hasPermission("booscooldowns.price2")) {
			return boosConfigManager.getPrice2(preSub);
		} else if (player.hasPermission("booscooldowns.price3")) {
			return boosConfigManager.getPrice3(preSub);
		} else if (player.hasPermission("booscooldowns.price4")) {
			return boosConfigManager.getPrice4(preSub);
		} else if (player.hasPermission("booscooldowns.price5")) {
			return boosConfigManager.getPrice5(preSub);
		} else {
			return boosConfigManager.getPrice(preSub);
		}
	}

	private int preSubCheck(Player player, String preSub) {

		if (player.hasPermission("booscooldowns.warmup2")) {
			return boosConfigManager.getWarmUp2(preSub);
		} else if (player.hasPermission("booscooldowns.warmup3")) {
			return boosConfigManager.getWarmUp3(preSub);
		} else if (player.hasPermission("booscooldowns.warmup4")) {
			return boosConfigManager.getWarmUp4(preSub);
		} else if (player.hasPermission("booscooldowns.warmup5")) {
			return boosConfigManager.getWarmUp5(preSub);
		} else {
			return boosConfigManager.getWarmUp(preSub);
		}

	}

	private void start(PlayerCommandPreprocessEvent event, Player player,
			String pre, String message, int warmUpSeconds) {
		if (!boosCoolDownManager.checkWarmUpOK(player, pre, message)) {
			if (boosCoolDownManager.checkCoolDownOK(player, pre, message)) {
				boosWarmUpManager.startWarmUp(this.plugin, player, pre,
						message, warmUpSeconds);
				event.setCancelled(true);
				return;
			} else {
				event.setCancelled(true);
				return;
			}
		} else {
			if (boosCoolDownManager.coolDown(player, pre)) {
				event.setCancelled(true);
				return;
			} else {
				boosCoolDownManager.removeWarmUpOK(player, pre, message);
				return;
			}
		}
	}
}
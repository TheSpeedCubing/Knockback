package cubing;

import net.minecraft.server.v1_8_R3.PacketPlayOutEntityVelocity;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.Arrays;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

    double[] value;
    boolean kbtoggle = true;

    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginCommand("kb").setExecutor(this);
        value = new double[]{Main.getPlugin(Main.class).getConfig().getDouble("kb.hor"),
                Main.getPlugin(Main.class).getConfig().getDouble("kb.ver"),
                Main.getPlugin(Main.class).getConfig().getDouble("kb.airhor"),
                Main.getPlugin(Main.class).getConfig().getDouble("kb.airver")};
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }
        if (sender.isOp()) {
            if (args.length == 2) {
                double x;
                try {
                    x = Double.parseDouble(args[1]);
                } catch (Exception exception) {
                    exception.printStackTrace();
                    sender.sendMessage("int/float/double required.");
                    return true;
                }
                switch (args[0].toLowerCase()) {
                    case "hor":
                        Main.getPlugin(Main.class).getConfig().set("kb.hor", x);
                        break;
                    case "ver":
                        Main.getPlugin(Main.class).getConfig().set("kb.ver", x);
                        break;
                    case "airhor":
                        Main.getPlugin(Main.class).getConfig().set("kb.airhor", x);
                        break;
                    case "airver":
                        Main.getPlugin(Main.class).getConfig().set("kb.airver", x);
                        break;
                    default:
                        sender.sendMessage("invalid kb type (hor/ver/airhor/airver)");
                        break;
                }
                Main.getPlugin(Main.class).saveConfig();
                Main.getPlugin(Main.class).reloadConfig();
                value = new double[]{Main.getPlugin(Main.class).getConfig().getDouble("kb.hor"),
                        Main.getPlugin(Main.class).getConfig().getDouble("kb.ver"),
                        Main.getPlugin(Main.class).getConfig().getDouble("kb.airhor"),
                        Main.getPlugin(Main.class).getConfig().getDouble("kb.airver")};
                sender.sendMessage(Arrays.toString(value));
            } else if (args.length == 1) {
                if (args[0].equalsIgnoreCase("reload")) {
                    Main.getPlugin(Main.class).reloadConfig();
                    value = new double[]{Main.getPlugin(Main.class).getConfig().getDouble("kb.hor"),
                            Main.getPlugin(Main.class).getConfig().getDouble("kb.ver"),
                            Main.getPlugin(Main.class).getConfig().getDouble("kb.airhor"),
                            Main.getPlugin(Main.class).getConfig().getDouble("kb.airver")};
                    sender.sendMessage(Arrays.toString(value) + " (config reloaded)");
                    sender.sendMessage("");
                } else if (args[0].equalsIgnoreCase("toggle")) {
                    if (kbtoggle)
                        sender.sendMessage("disabled");
                    else sender.sendMessage("enabled");
                    kbtoggle = !kbtoggle;
                }
            } else if (args.length == 0) {
                sender.sendMessage(Arrays.toString(value));
            } else return false;
        }
        return true;
    }

    @EventHandler
    public void onKnockback(PlayerVelocityEvent event) {
        if (kbtoggle) {
            Player player = event.getPlayer();
            EntityDamageEvent lastDamageCause = player.getLastDamageCause();
            if (!(lastDamageCause instanceof EntityDamageByEntityEvent))
                return;
            if (((EntityDamageByEntityEvent) lastDamageCause).getDamager() instanceof Player)
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            if (e.getCause() == EntityDamageEvent.DamageCause.FALL) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (kbtoggle) {
            if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player))
                return;
            Player damager = (Player) event.getDamager();
            Player entity = (Player) event.getEntity();
            if (entity.getNoDamageTicks() > entity.getMaximumNoDamageTicks() / 2)
                return;
            double horizontalMultiplier = (entity.getLocation().getY() % 1D == 0D) ? value[0] : value[2];
            double verticalMultiplier = (entity.getLocation().getY() % 1D == 0D) ? value[1] : value[3];

            double xDiff = damager.getLocation().getX() - entity.getLocation().getX();
            double zDiff = damager.getLocation().getZ() - entity.getLocation().getZ();
            float horizontalDistance = (float) Math.sqrt(xDiff * xDiff + zDiff * zDiff);
            Vector velocity = entity.getVelocity();
            velocity.setX(velocity.getX() / 2D);
            velocity.setY(velocity.getY() / 2D);
            velocity.setZ(velocity.getZ() / 2D);
            velocity.setX(velocity.getX() - xDiff / horizontalDistance * 0.4F);
            velocity.setY(velocity.getY() + 0.4F);
            velocity.setZ(velocity.getZ() - zDiff / horizontalDistance * 0.4F);
            if (velocity.getY() > 0.4000000059604645D)
                velocity.setY(0.4000000059604645D);
            int i = (damager.isSprinting() ? 1 : 0) + ((damager.getItemInHand() == null) ? 0 : damager.getItemInHand().getEnchantmentLevel(Enchantment.KNOCKBACK));
            if (i > 0) {
                velocity.setX(velocity.getX() + -Math.sin((damager.getLocation().getYaw() * Math.PI / 180F)) * i * 0.5D);
                velocity.setY(velocity.getY() + 0.1D);
                velocity.setZ(velocity.getZ() + Math.cos((damager.getLocation().getYaw() * Math.PI / 180F)) * i * 0.5D);
            }
            entity.setVelocity(velocity);
            Vector vector = entity.getVelocity();
            ((CraftPlayer) entity).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityVelocity(entity.getEntityId(), vector.getX() * horizontalMultiplier, vector.getY() * verticalMultiplier, vector.getZ() * horizontalMultiplier));
        }
    }
}

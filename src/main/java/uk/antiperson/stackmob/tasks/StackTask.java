package uk.antiperson.stackmob.tasks;

import com.intellectualcrafters.plot.object.Plot;
import com.wasteofplastic.askyblock.Island;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.scheduler.BukkitRunnable;
import uk.antiperson.stackmob.StackMob;
import static uk.antiperson.stackmob.StackMob.plotApi;
import static uk.antiperson.stackmob.StackMob.sbAPI;
import uk.antiperson.stackmob.tools.extras.GlobalValues;

/**
 * Created by nathat on 27/07/17.
 */
public class StackTask extends BukkitRunnable {

    private StackMob sm;

    public StackTask(StackMob sm) {
        this.sm = sm;
    }

    // TODO: Neaten this stuff up, but I cba.
    @Override
    public void run() {
        double xLoc = sm.config.getCustomConfig().getDouble("check-area.x");
        double yLoc = sm.config.getCustomConfig().getDouble("check-area.y");
        double zLoc = sm.config.getCustomConfig().getDouble("check-area.z");

        int maxSize;
        // Worlds loop, and check that the world isn't blacklisted
        for (World world : Bukkit.getWorlds()) {
            if (sm.config.getCustomConfig().getStringList("no-stack-worlds")
                    .contains(world.getName())) {
                continue;
            }
            // Loop all entities in the current world
            for (Entity first : world.getLivingEntities()) {
                // Checks on first entity
                if (first instanceof ArmorStand) {
                    continue;
                }

                if (sm.tools.notTaskSuitable(first)) {
                    continue;
                }
                if (first.hasMetadata(GlobalValues.NOT_ENOUGH_NEAR)
                        && first.getMetadata(GlobalValues.NOT_ENOUGH_NEAR).get(0).asBoolean()) {
                    sm.tools.notEnoughNearby(first);
                }
                // don't need a check because of the entity might have Not-enough-near

                // Find nearby entities
                for (Entity nearby : first.getNearbyEntities(xLoc, yLoc, zLoc)) {

                    //Checks on nearby
                    if (first.getType() != nearby.getType()) {
                        continue;
                    }
                    maxSize = sm.config.getCustomConfig().getInt("stack-max");
                    if (sm.config.getCustomConfig().isInt("custom." + first.getType() + ".stack-max")) {
                        maxSize = sm.config.getCustomConfig().getInt("custom." + first.getType() + ".stack-max");
                    }

                    if (!nearby.hasMetadata(GlobalValues.METATAG) || nearby.getMetadata(GlobalValues.METATAG).size() == 0
                            || nearby.getMetadata(GlobalValues.METATAG).get(0).asInt() == maxSize) {
                        continue;
                    }

                    if (sm.config.getCustomConfig().getBoolean("stack-limit-enabled")) {
                        int limite = 100;
                        if (plotApi != null) {
                            Plot plot = StackMob.plotApi.getPlot(first.getLocation());
                            if (plot != null) {
                                UUID uuid = plot.guessOwner();
                                String tipo = first.getType().toString();
                                if (Bukkit.getPlayer(uuid) == null) {
                                    continue;
                                }
                                for (PermissionAttachmentInfo perms : Bukkit.getPlayer(uuid).getEffectivePermissions()) {
                                    if (perms.getPermission().contains("stacklimit")) {
                                        String splitar = perms.getPermission().toString();

                                        if (splitar.split(";")[1].equalsIgnoreCase(tipo)) {
                                            int numeroPerm = Integer.parseInt(splitar.split(";")[2]);
                                            if (limite < numeroPerm) {
                                                limite = numeroPerm;
                                            }
                                        }
                                    }
                                }
                                if (nearby.getMetadata(GlobalValues.METATAG).get(0).asInt() == limite) {
                                    continue;
                                }
                            }
                        }
                        if (sbAPI != null) {
                            Island ilha = sbAPI.getIslandAt(first.getLocation());
                            if (ilha != null) {
                                UUID uuid = ilha.getOwner();
                                String tipo = first.getType().toString();
                                if (Bukkit.getPlayer(uuid) == null) {
                                    continue;
                                }
                                for (PermissionAttachmentInfo perms : Bukkit.getPlayer(uuid).getEffectivePermissions()) {
                                    if (perms.getPermission().contains("stacklimit")) {
                                        String splitar = perms.getPermission().toString();

                                        if (splitar.split(";")[1].equalsIgnoreCase(tipo)) {
                                            int numeroPerm = Integer.parseInt(splitar.split(";")[2]);
                                            if (limite < numeroPerm) {
                                                limite = numeroPerm;
                                            }
                                        }
                                    }
                                }
                                if (nearby.getMetadata(GlobalValues.METATAG).get(0).asInt() == limite) {
                                    continue;
                                }
                            }
                        }
                    }

                    if (sm.tools.notTaskSuitable(nearby)) {
                        continue;
                    }

                    // Check attributes of both
                    if (sm.tools.notMatching(first, nearby)) {
                        continue;
                    }

                    int nearbySize = nearby.getMetadata(GlobalValues.METATAG).get(0).asInt();
                    int firstSize;
                    if (first.hasMetadata(GlobalValues.METATAG)) {
                        firstSize = first.getMetadata(GlobalValues.METATAG).get(0).asInt();
                    } else {
                        firstSize = 1;
                    }

                    // Nearby would normally get removed, but we're swapping it.
                    if (nearbySize > firstSize && sm.config.getCustomConfig().getBoolean("big-priority")) {
                        Entity holder = nearby;
                        nearby = first;
                        first = holder;
                    }

                    // Continue to stack together
                    int amountTotal = nearbySize + firstSize;
                    if (amountTotal > maxSize) {
                        first.setMetadata(GlobalValues.METATAG, new FixedMetadataValue(sm, maxSize));
                        nearby.setMetadata(GlobalValues.METATAG, new FixedMetadataValue(sm, amountTotal - maxSize));
                    } else {
                        first.setMetadata(GlobalValues.METATAG, new FixedMetadataValue(sm, amountTotal));
                        nearby.remove();
                    }
                    break;

                }
            }
        }
    }
}

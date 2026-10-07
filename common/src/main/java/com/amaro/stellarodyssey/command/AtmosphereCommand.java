package com.amaro.stellarodyssey.command;

import com.amaro.stellarodyssey.api.celestial.ICelestialBody;
import com.amaro.stellarodyssey.item.OxygenTankItem;
import com.amaro.stellarodyssey.lifesupport.AtmosphereHelper;
import com.amaro.stellarodyssey.world.CelestialBodyRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Diagnostic command (/stellarodyssey atmosphere or /atmosphere) for querying local atmospheric pressure,
 * room sealing status, spacesuit hermetic integrity, and carried oxygen reserves.
 */
public final class AtmosphereCommand {

    private AtmosphereCommand() {
    }

    /**
     * Registers the command hierarchy into Architectury's command event dispatcher.
     */
    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, selection) -> registerCommands(dispatcher));
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("stellarodyssey")
                        .then(Commands.literal("atmosphere")
                                .executes(AtmosphereCommand::executeDiagnostic))
                        .then(Commands.literal("diag")
                                .executes(AtmosphereCommand::executeDiagnostic))
        );

        // Alias shortcut for quick player diagnostics
        dispatcher.register(
                Commands.literal("atmosphere")
                        .executes(AtmosphereCommand::executeDiagnostic)
        );
    }

    private static int executeDiagnostic(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be executed by a player in-game."));
            return 0;
        }

        // Gather atmospheric parameters
        boolean isSealed = AtmosphereHelper.isRoomSealed(player.level(), player.blockPosition());
        boolean isVacuum = AtmosphereHelper.isVacuumEnvironment(player);
        boolean isToxic = AtmosphereHelper.isUnbreathableAtmosphere(player);
        Optional<ICelestialBody> body = CelestialBodyRegistry.getInstance().getBody(player.level().dimension());

        // Gather spacesuit parameters
        int suitCount = AtmosphereHelper.getEquippedSpacesuitPieceCount(player);
        boolean hasHelmet = AtmosphereHelper.hasPressurizedHelmet(player);
        boolean hasManifold = AtmosphereHelper.hasOxygenManifold(player);

        // Gather oxygen inventory parameters
        Inventory inv = player.getInventory();
        int totalOxygen = 0;
        int maxCapacity = 0;
        int tankCount = 0;

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() instanceof OxygenTankItem) {
                totalOxygen += OxygenTankItem.getOxygen(stack);
                maxCapacity += OxygenTankItem.CAPACITY;
                tankCount++;
            }
        }

        // Format and send diagnostic output
        player.sendSystemMessage(Component.literal("=== 🚀 STELLAR ODYSSEY ATMOSPHERIC DIAGNOSTICS ===")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));

        // Location & Body
        String bodyName = body.map(ICelestialBody::name).orElse("Uncharted Sector");
        player.sendSystemMessage(Component.literal(" Location: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(bodyName).withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" (" + player.level().dimension().identifier() + ")").withStyle(ChatFormatting.DARK_GRAY)));

        // Environment Status
        Component envStatus;
        if (isSealed) {
            envStatus = Component.literal("HERMETICALLY SEALED ROOM (Pressurized Artificial Air)")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
        } else if (isVacuum) {
            envStatus = Component.literal("HARD VACUUM (< 0.05 atm - Lethal Decompression Risk)")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        } else if (isToxic) {
            envStatus = Component.literal("UNBREATHABLE/TOXIC ATMOSPHERE (Life Support Required)")
                    .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
        } else {
            envStatus = Component.literal("NOMINAL BREATHABLE ATMOSPHERE")
                    .withStyle(ChatFormatting.GREEN);
        }
        player.sendSystemMessage(Component.literal(" Environment: ")
                .withStyle(ChatFormatting.GRAY)
                .append(envStatus));

        // Spacesuit Integrity
        ChatFormatting suitColor = suitCount == 4 ? ChatFormatting.GREEN : (suitCount > 0 ? ChatFormatting.YELLOW : ChatFormatting.RED);
        String suitDesc = suitCount == 4 ? "4/4 (Fully Hermetic Seal Active)" : suitCount + "/4 (Breached / Incomplete Seal)";
        player.sendSystemMessage(Component.literal(" Spacesuit Seal: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(suitDesc).withStyle(suitColor)));

        player.sendSystemMessage(Component.literal("   - Visor Helmet: ").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.literal(hasHelmet ? "CONNECTED" : "MISSING").withStyle(hasHelmet ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED))
                .append(Component.literal(" | Manifold Chest: ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(hasManifold ? "CONNECTED" : "MISSING").withStyle(hasManifold ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED)));

        // Oxygen Tanks
        String tankText = tankCount > 0 ? totalOxygen + " / " + maxCapacity + " O2 units (" + tankCount + " tank" + (tankCount > 1 ? "s" : "") + ")" : "NO TANKS CARRIED";
        ChatFormatting o2Color = totalOxygen > 500 ? ChatFormatting.GREEN : (totalOxygen > 0 ? ChatFormatting.YELLOW : ChatFormatting.RED);
        player.sendSystemMessage(Component.literal(" Oxygen Reserve: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(tankText).withStyle(o2Color)));

        player.sendSystemMessage(Component.literal("================================================")
                .withStyle(ChatFormatting.AQUA));

        return 1;
    }
}

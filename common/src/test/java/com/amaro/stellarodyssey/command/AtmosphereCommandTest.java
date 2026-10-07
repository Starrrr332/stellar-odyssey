package com.amaro.stellarodyssey.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AtmosphereCommandTest {

    @Test
    @DisplayName("AtmosphereCommand registers literal nodes into Brigadier dispatcher without throwing")
    void testCommandRegistration() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();

        assertDoesNotThrow(() -> AtmosphereCommand.registerCommands(dispatcher));

        assertNotNull(dispatcher.getRoot().getChild("stellarodyssey"), "Root literal 'stellarodyssey' should be registered");
        assertNotNull(dispatcher.getRoot().getChild("stellarodyssey").getChild("atmosphere"), "Subcommand 'atmosphere' should be registered");
        assertNotNull(dispatcher.getRoot().getChild("stellarodyssey").getChild("diag"), "Subcommand 'diag' should be registered");
        assertNotNull(dispatcher.getRoot().getChild("atmosphere"), "Alias 'atmosphere' should be registered");
    }
}

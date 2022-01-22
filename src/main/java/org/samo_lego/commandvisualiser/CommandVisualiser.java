package org.samo_lego.commandvisualiser;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import org.samo_lego.commandvisualiser.gui.CommandGUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class CommandVisualiser {
    public static final String MOD_ID = "commandvisualiser";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static CommandVisualiser INSTANCE;
    private final GUIConfig config;

    public static void main(String ... args) {
        if (INSTANCE == null) {
            INSTANCE = new CommandVisualiser();
        }
    }

    public CommandVisualiser() {
        LOGGER.info("Loading CommandVisualiser ...");
        this.config = GUIConfig.loadConfigFile(new File(FabricLoader.getInstance().getConfigDir() + "/command_visualiser.json"));
        INSTANCE = this;

        CommandRegistrationCallback.EVENT.register(this::registerCommands);
    }

    public static CommandVisualiser getInstance() {
        return INSTANCE;
    }

    public GUIConfig getConfig() {
        return this.config;
    }

    private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, boolean dedicated) {
        for (String cmdNode : this.config.commands) {
            CommandNode<CommandSourceStack> child = dispatcher.getRoot().getChild(cmdNode);
            if (child instanceof LiteralCommandNode<CommandSourceStack> lcn) {
                LiteralArgumentBuilder<CommandSourceStack> build = lcn.createBuilder().executes(c -> CommandGUI.openGUI(c, child, cmdNode));
                dispatcher.register(build);
            }
        }
    }
}

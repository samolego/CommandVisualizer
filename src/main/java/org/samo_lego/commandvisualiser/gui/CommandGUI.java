package org.samo_lego.commandvisualiser.gui;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.gui.AnvilInputGui;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import org.samo_lego.commandvisualiser.CommandVisualiser;
import org.samo_lego.commandvisualiser.mixin.AMappedRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static net.minecraft.core.Registry.ITEM;

public class CommandGUI {

    private static final ItemStack YES_BUTTON = new ItemStack(Items.GREEN_STAINED_GLASS_PANE);
    private static final int REGISTRY_ITEMS_SIZE = ((AMappedRegistry) ITEM).getById().size();

    @SuppressWarnings("unchecked")
    public static SimpleGui createCommandGui(ServerPlayer player, @Nullable SimpleGui previousScreen, CommandNode<CommandSourceStack> parentNode, List<String> currentCommandPath, boolean givenInput) {
        // If node is not an argument, we skip to first child node that is an argument or has more than 1 child node
        while (parentNode.getChildren().size() == 1 && !(parentNode instanceof ArgumentCommandNode<?, ?>)) {
            CommandNode<CommandSourceStack> childNode = (CommandNode<CommandSourceStack>) parentNode.getChildren().toArray()[0];
            if (childNode instanceof ArgumentCommandNode) {
                givenInput = false;
            } else if (childNode.getChildren().size() > 0) {
                currentCommandPath.add(parentNode.getName());
            } else {
                break;
            }
            parentNode = childNode;

        }

        Collection<CommandNode<CommandSourceStack>> childNodes = parentNode.getChildren();
        boolean argumentNode = parentNode instanceof ArgumentCommandNode<?, ?>;

        SimpleGui constructedGui;
        if(argumentNode && !givenInput) {
            constructedGui = new AnvilInputGui(player, true);

            final CommandNode<CommandSourceStack> finalParentNode = parentNode;
            GuiElement confirmButton = new GuiElement(YES_BUTTON, (index, clickType, slotActionType) -> {
                String arg = ((AnvilInputGui) constructedGui).getInput();
                // We "set" the argument to overwrite parent node (arg name)
                currentCommandPath.add(arg);

                CommandNode<CommandSourceStack> newNode = finalParentNode;
                if (childNodes.size() == 1)
                    newNode = (CommandNode<CommandSourceStack>) childNodes.toArray()[0];

                proccessClick(clickType, newNode, constructedGui, currentCommandPath, player, childNodes.size() != 1);
            });

            // Pre-written  text
            MutableComponent argTitle = new TextComponent(parentNode.getName()).withStyle(ChatFormatting.YELLOW);
            ItemStack nameStack = new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE);  // invisible (kinda)
            nameStack.setHoverName(argTitle);

            // Buttons
            constructedGui.setSlot(2, confirmButton);
            constructedGui.setSlot(0, nameStack);

            // Default input value
            String[] examples = parentNode.getExamples().toArray(new String[0]);
            if (examples.length > 0)
                nameStack.setHoverName(new TextComponent(examples[0]));

            for (int i = 1; i < examples.length; ++i) {
                ItemStack exampleStack = new ItemStack(Items.PAPER);
                String example = examples[i];
                exampleStack.setHoverName(new TextComponent(example));

                // 2 being the last slot index in anvil inventory
                constructedGui.setSlot(i * 2 + 1, new GuiElement(exampleStack, (index, type, action) -> {
                    String input = ((AnvilInputGui) constructedGui).getInput();
                    ((AnvilInputGui) constructedGui).setDefaultInputValue(exampleStack.getHoverName().getString());
                    exampleStack.setHoverName(new TextComponent(input));
                }));
            }
        } else {
            // Creates the biggest possible container
            constructedGui = new SimpleGui(MenuType.GENERIC_9x6, player, true);

            // Close screen button
            ItemStack close = new ItemStack(Items.STRUCTURE_VOID);
            close.setHoverName(new TranslatableComponent("spectatorMenu.close"));
            close.enchant(null, 0);

            GuiElement closeScreenButton = new GuiElement(close, (i, clickType, slotActionType) -> player.closeContainer());
            constructedGui.setSlot(8, closeScreenButton);

            // Integer to track item positions
            AtomicInteger i = new AtomicInteger(10);

            // Looping through command node, 8 * 9 being the available inventory space
            int addedSpace = (childNodes.size() < 8 * 9 / 3) ? (3) : (8 * 9 / childNodes.size());
            for (CommandNode<CommandSourceStack> node : childNodes) {
                // Tracking current command "path"
                // after each menu is opened, we add a node to queue
                ArrayList<String> parents = new ArrayList<>(currentCommandPath);
                String nodeName = node.getName();
                if(!(node instanceof ArgumentCommandNode<?, ?>))
                    parents.add(nodeName);

                // Set stack "icon"
                ItemStack stack = new ItemStack(CommandVisualiser.getInstance().getConfig().node2item.getOrDefault(nodeName, getFromName(nodeName)));
                stack.setHoverName(new TextComponent(nodeName));

                // Recursively adding the command nodes
                constructedGui.setSlot(i.getAndAdd(addedSpace), new GuiElement(stack, (index, clickType, slotActionType) -> {
                    // Different action happens on right or left click
                    proccessClick(clickType, node, constructedGui, parents, player, false);
                }));
            }
        }

        // Back button
        ItemStack back = new ItemStack(Items.MAGENTA_GLAZED_TERRACOTTA);
        back.setHoverName(new TranslatableComponent("gui.back"));
        back.enchant(null, 0);

        GuiElement backScreenButton = new GuiElement(back, (i, clickType, slotActionType) -> {
            if (previousScreen == null) {
                player.closeContainer();
            } else {
                constructedGui.close();
                currentCommandPath.remove(currentCommandPath.size() - 1);
                previousScreen.open();
            }
        });
        constructedGui.setSlot(argumentNode && !givenInput ? 1 : 0, backScreenButton);

        // GUI Title - each node adds to it
        StringBuilder title = new StringBuilder();
        for (String s : currentCommandPath) {
            title.append(s).append(" ");
        }
        TextComponent textTitle = new TextComponent(title.toString());

        constructedGui.setTitle(textTitle.withStyle(ChatFormatting.YELLOW));
        constructedGui.setAutoUpdate(true);

        return constructedGui;
    }

    private static void proccessClick(ClickType clickType, CommandNode<CommandSourceStack> node, SimpleGui gui, List<String> currentCommandPath, ServerPlayer player, boolean givenInput) {
        StringBuilder builder = new StringBuilder();
        // Builds the command from parents
        currentCommandPath.forEach(s -> builder.append(s).append(" "));
        // Delete last space
        builder.deleteCharAt(builder.length() - 1);

        if (CommandVisualiser.getInstance().getConfig().prefersExecution.contains(builder.toString())) {
            // Swaps the click type
           clickType = clickType == ClickType.MOUSE_LEFT ? ClickType.MOUSE_RIGHT : ClickType.MOUSE_LEFT;
        }

        if (clickType == ClickType.MOUSE_LEFT && node.getChildren().size() > 0 || (node instanceof ArgumentCommandNode<?, ?> && !givenInput)) {
            createCommandGui(player, gui, node, currentCommandPath, givenInput).open();
        } else {
            execute(player, currentCommandPath);
        }
        gui.close();
    }


    /**
     * Executes the command
     * @param player player to execute command as.
     * @param commandTree command tree to execute.
     */
    private static void execute(ServerPlayer player, List<String> commandTree) {
        try {
            // Execute
            // we "fake" the command
            StringBuilder builder = new StringBuilder();

            // Builds the command from commandTree
            commandTree.forEach(nd -> builder.append(nd).append(" "));
            // Delete last space
            builder.deleteCharAt(builder.length() - 1);

            player.closeContainer();

            player.getServer().getCommands().performCommand(player.createCommandSourceStack(), builder.toString());
        } catch (IllegalArgumentException e) {
            player.sendMessage(new TextComponent(e.getMessage()), player.getUUID());
        }
    }


    /**
     * Gets an item from registry by string hash.
     * @param name string to convert into item
     * @return item, converted from string hash. If air would be returned, it is switched top stone instead.
     */
    public static Item getFromName(String name) {
        int i = Math.abs(name.hashCode());
        Item item = Item.byId(i % REGISTRY_ITEMS_SIZE);
        if (item.equals(Items.AIR))
            item = Items.STONE;

        return item;
    }

    public static int openGUI(CommandContext<CommandSourceStack> context, CommandNode<CommandSourceStack> node, String cmdNode) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        SimpleGui editorGUI = createCommandGui(player, null, node, new ArrayList<>(List.of(cmdNode)), false);
        editorGUI.open();
        return 1;

    }

    static {
        YES_BUTTON.setHoverName(new TranslatableComponent("gui.done"));
    }
}

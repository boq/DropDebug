package openmods.dropdebug;

import java.util.List;
import java.util.Locale;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentTranslation;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class CommandControl implements ICommand {

    private static final String SUBCOMMAND_UNINSTALL = "uninstall";
    private static final String SUBCOMMAND_INSTALL = "install";
    private static final String SUBCOMMAND_STATE = "state";

    private static final List<String> SUBCOMMANDS = ImmutableList.of(SUBCOMMAND_INSTALL, SUBCOMMAND_UNINSTALL, SUBCOMMAND_STATE);

    @Override
    public int compareTo(Object o) {
        return getCommandName().compareTo(((ICommand)o).getCommandName());
    }

    @Override
    public String getCommandName() {
        return "debug_drops";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return Joiner.on(", ").join(SUBCOMMANDS);
    }

    @Override
    public List<?> getCommandAliases() {
        return null;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1)
            throw new CommandException("dropdebug.command.invalid_subcommand");

        if (args[0].equalsIgnoreCase(SUBCOMMAND_STATE)) {
            sender.addChatMessage(new ChatComponentTranslation(DropDebug.instance.isInstalled() ? "dropdebug.command.installed" : "dropdebug.command.uninstalled"));
        } else if (args[0].equalsIgnoreCase(SUBCOMMAND_INSTALL)) {
            DropDebug.instance.install();
            sender.addChatMessage(new ChatComponentTranslation("dropdebug.command.installed"));
        } else if (args[0].equalsIgnoreCase(SUBCOMMAND_UNINSTALL)) {
            DropDebug.instance.uninstall();
            sender.addChatMessage(new ChatComponentTranslation("dropdebug.command.uninstalled"));
        }
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return sender.canCommandSenderUseCommand(2, getCommandName());
    }

    @Override
    public List<?> addTabCompletionOptions(ICommandSender sender, String[] args) {
        return (args.length == 1) ? filterPrefixes(args[0], SUBCOMMANDS) : null;
    }

    public static List<String> filterPrefixes(String prefix, Iterable<String> proposals) {
        prefix = prefix.toLowerCase(Locale.ENGLISH);

        List<String> result = Lists.newArrayList();
        for (String s : proposals)
            if (s.toLowerCase(Locale.ENGLISH).startsWith(prefix))
                result.add(s);

        return result;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

}

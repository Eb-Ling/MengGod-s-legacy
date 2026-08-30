package scripts.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

/** Adds Crimson Spike for campaign and refit testing. */
public final class Meng_OldEmpireAddAxialWeapons implements BaseCommand {
    private static final String[] WEAPONS = {
        "Meng_OldEmpire_axis_crimson_spike"
    };

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage("This command can only be used in the campaign.");
            return CommandResult.WRONG_CONTEXT;
        }
        int amount = 1;
        String trimmed = args == null ? "" : args.trim();
        if (!trimmed.isEmpty()) {
            try {
                amount = Integer.parseInt(trimmed);
            } catch (NumberFormatException ex) {
                return CommandResult.BAD_SYNTAX;
            }
        }
        if (amount < 1 || amount > 99) {
            Console.showMessage("Amount must be between 1 and 99.");
            return CommandResult.BAD_SYNTAX;
        }
        CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
        for (String id : WEAPONS) {
            cargo.addWeapons(id, amount);
        }
        Console.showMessage("Added " + amount + " Crimson Spike weapon(s).");
        return CommandResult.SUCCESS;
    }
}

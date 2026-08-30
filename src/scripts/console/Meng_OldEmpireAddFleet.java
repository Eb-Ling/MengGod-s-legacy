package scripts.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import java.util.Random;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

public final class Meng_OldEmpireAddFleet implements BaseCommand {
    private static final String[] VARIANTS = {
        "Meng_OldEmpire_001_Test",
        "Meng_OldEmpire_002_Test",
        "Meng_OldEmpire_003_Test",
        "Meng_OldEmpire_004_Test",
        "Meng_OldEmpire_005_Test"
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
        if (amount < 1 || amount > 20) {
            Console.showMessage("Amount must be between 1 and 20.");
            return CommandResult.BAD_SYNTAX;
        }

        FleetDataAPI fleet = Global.getSector().getPlayerFleet().getFleetData();
        Random random = new Random();
        try {
            for (String variantId : VARIANTS) {
                for (int i = 0; i < amount; i++) {
                    FleetMemberAPI member = Global.getFactory()
                            .createFleetMember(FleetMemberType.SHIP, variantId);
                    FleetEncounterContext.prepareShipForRecovery(member,
                            true, true, true, 1f, 1f, random);
                    fleet.addFleetMember(member);
                }
            }
        } catch (Throwable ex) {
            Console.showException("Failed to add the OldEmpire test fleet.", ex);
            return CommandResult.ERROR;
        }

        Console.showMessage("Added " + amount + " of each OldEmpire hull ("
                + (amount * VARIANTS.length) + " ships total).");
        return CommandResult.SUCCESS;
    }
}

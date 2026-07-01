package data.scripts.campaign;

import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;

public class Meng_recoverplugin extends FleetInteractionDialogPluginImpl {

    public Meng_recoverplugin() {
        this(null);
    }

    public Meng_recoverplugin(FIDConfig params) {
        super(params);
        context = new Meng_fleetRecover();
    }
}

package data.scripts.campaign.bar;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventCreator;

public class Meng_protect_bar_eventcreator_2 extends BaseBarEventCreator {
    @Override
    public PortsideBarEvent createBarEvent() {
        Global.getLogger(this.getClass()).info("Event stage 3 created");
        return new Meng_protect_bar_event2();
    }

    @Override
    public float getBarEventAcceptedTimeoutDuration() {
        return 10000000000f; // will reset when intel ends... or not, if keeping this one-time-only
    }


    @Override
    public boolean isPriority() {
        return true;
    }
}


package data.scripts.campaign.bar;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventCreator;

public class MouMeng_bar_eventcreator_1 extends BaseBarEventCreator {
    @Override
    public PortsideBarEvent createBarEvent() {
        Global.getLogger(this.getClass()).info("Event stage 1 created");
        return new MouMeng_bar_event1();
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


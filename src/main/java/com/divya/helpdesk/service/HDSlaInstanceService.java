package com.divya.helpdesk.service;

import com.divya.helpdesk.entity.HDSlaInstance;
import com.divya.helpdesk.entity.HDSlaPolicy;
import com.divya.helpdesk.entity.HDTicket;

import java.util.Optional;

public interface HDSlaInstanceService {

    HDSlaInstance createSlaInstance(HDTicket ticket, HDSlaPolicy policy);

    void resolveSlaInstance(HDTicket ticket);

    HDSlaInstance reopenSlaInstance(HDTicket ticket, HDSlaPolicy policy);

    void checkSlaInstances();

    boolean isBreached(HDTicket ticket);
}

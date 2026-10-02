package com.divya.helpdesk.service;

import com.divya.helpdesk.entity.HDSlaInstanceEntity;
import com.divya.helpdesk.entity.HDSlaPolicyEntity;
import com.divya.helpdesk.entity.HDTicketEntity;

public interface SlaInstanceService {

    HDSlaInstanceEntity createSlaInstance(HDTicketEntity ticket, HDSlaPolicyEntity policy);

    void resolveSlaInstance(HDTicketEntity ticket);

    void pauseSlaInstance(HDTicketEntity ticket);

    void resumeSlaInstance(HDTicketEntity ticket);

    HDSlaInstanceEntity reopenSlaInstance(HDTicketEntity ticket, HDSlaPolicyEntity policy);

    void checkSlaInstances();
}

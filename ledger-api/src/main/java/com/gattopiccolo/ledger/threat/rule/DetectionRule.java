package com.gattopiccolo.ledger.threat.rule;

import com.gattopiccolo.ledger.threat.EventWindow;
import com.gattopiccolo.ledger.threat.SecurityEvent;

import java.util.Optional;

/**
 * A detection use case. Rules are evaluated for every incoming event, after the
 * event has been added to the window. They must be cheap and side-effect free.
 */
public interface DetectionRule {

    String id();

    Optional<Detection> evaluate(SecurityEvent latest, EventWindow window);
}

package org.camunda.app.service;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

import static java.lang.String.format;

@Slf4j
@NoArgsConstructor
public class NotifyCreditorService implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        log.info(format("\n\n  ... Now notifying creditor %s\n\n", execution.getVariable("creditor")));
    }
}

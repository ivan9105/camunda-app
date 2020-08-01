package org.camunda.app.service;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.variable.value.FileValue;

import static java.lang.String.format;

@Slf4j
@NoArgsConstructor
public class ArchiveInvoiceService implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        Boolean shouldFail = (Boolean) execution.getVariable("shouldFail");
        FileValue invoiceDocument = execution.getVariableTyped("invoiceDocument");
        if (shouldFail != null && shouldFail) {
            throw new ProcessEngineException("Could not archive invoice...");
        } else {
            Object invoiceNumber = execution.getVariable("invoiceNumber");
            String filename = invoiceDocument.getFilename();
            log.info(format("\n\n  ... Now archiving invoice %s, filename: %s \n\n", invoiceNumber, filename));
        }
    }
}

package org.camunda.app.process;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.app.generator.SampleDataGenerator;
import org.camunda.bpm.application.PostDeploy;
import org.camunda.bpm.application.ProcessApplication;
import org.camunda.bpm.application.impl.ServletProcessApplication;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.metrics.reporter.DbMetricsReporter;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinitionQuery;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.value.FileValue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.function.Consumer;

import static java.lang.String.format;
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Collections.singletonList;
import static org.apache.commons.codec.Resources.getInputStream;
import static org.camunda.bpm.engine.impl.util.ClockUtil.reset;
import static org.camunda.bpm.engine.impl.util.ClockUtil.setCurrentTime;

@ProcessApplication
@NoArgsConstructor
@Slf4j
public class InvoiceProcessApplication extends ServletProcessApplication {

    private final SampleDataGenerator dataGenerator = new SampleDataGenerator();

    private static final String INVOICE_PDF = "invoice.pdf";

    @PostDeploy
    public void startFirstProcess(ProcessEngine processEngine) {
        dataGenerator.generate(processEngine);

        ProcessEngineConfigurationImpl engineConfiguration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();
        engineConfiguration.setDbMetricsReporterActivate(true);
        engineConfiguration.getDbMetricsReporter().setReporterId("REPORTER");
        startProcessInstances(processEngine, "invoice", 1);
        startProcessInstances(processEngine, "invoice", null);
        engineConfiguration.setDbMetricsReporterActivate(false);
    }

    private void startProcessInstances(ProcessEngine processEngine, String processDefinitionKey, Integer version) {
        ProcessEngineConfigurationImpl engineConfiguration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();
        ProcessDefinition processDefinition = getProcessDefinition(processEngine, processDefinitionKey, version);
        RuntimeService runtimeService = processEngine.getRuntimeService();
        TaskService taskService = processEngine.getTaskService();
        DbMetricsReporter dbMetricsReporter = engineConfiguration.getDbMetricsReporter();

        long numberOfRunningProcessInstances = runtimeService.createProcessInstanceQuery()
                .processDefinitionId(processDefinition.getId())
                .count();

        if (numberOfRunningProcessInstances != 0) {
            log.info(format(
                    "No new instances of %s version %d started, there are %d instances running",
                    processDefinition.getName(), processDefinition.getVersion(), numberOfRunningProcessInstances
            ));
            return;
        }

        log.info(format(
                "Start 3 instances of %s, version %d",
                processDefinition.getName(), processDefinition.getVersion()
        ));

        startInvoiceProcess((inputStream) -> {
            startInvoiceProcess(
                    processDefinition,
                    runtimeService,
                    inputStream,
                    "Great Pizza for Everyone Inc.",
                    30.0D,
                    "Travel Expenses",
                    "GPFE-23232323"
            );

            dbMetricsReporter.reportNow();
        });

        startInvoiceProcess((inputStream) -> {
            Calendar calendar = Calendar.getInstance();
            addDays(calendar, -14);

            ProcessInstance instance = startInvoiceProcess(
                    processDefinition,
                    runtimeService,
                    inputStream,
                    "Bobby's Office Supplies",
                    900.0D,
                    "Misc",
                    "BOS-43934"
            );

            dbMetricsReporter.reportNow();

            addDays(calendar, 14);
            processEngine.getIdentityService().setAuthentication("demo", singletonList("camunda-admin"));

            Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();
            taskService.claim(task.getId(), "demo");
            taskService.complete(task.getId(), Variables.createVariables().putValue("approved", true));
            dbMetricsReporter.reportNow();
            reset();
            processEngine.getIdentityService().clearAuthentication();
        });

        startInvoiceProcess((inputStream) -> {
            Calendar calendar = Calendar.getInstance();
            addDays(calendar, -5);

            ProcessInstance instance = startInvoiceProcess(
                    processDefinition,
                    runtimeService,
                    inputStream,
                    "Papa Steve's all you can eat",
                    10.99D,
                    "Travel Expenses",
                    "PSACE-5342"
            );

            dbMetricsReporter.reportNow();

            addDays(calendar, 5);
            processEngine.getIdentityService().setAuthentication("demo", singletonList("camunda-admin"));

            Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();
            taskService.createComment(null, instance.getId(), "I cannot approve this invoice: the amount is missing.\n\n Could you please provide the amount?");
            taskService.complete(task.getId(), Variables.createVariables().putValue("approved", false));
            dbMetricsReporter.reportNow();
            reset();
            processEngine.getIdentityService().clearAuthentication();
        });
    }

    private static ProcessInstance startInvoiceProcess(ProcessDefinition processDefinition, RuntimeService runtimeService, InputStream inputStream, String creditor, double amount, String category, String number) {
        return runtimeService.startProcessInstanceById(
                processDefinition.getId(),
                Variables.createVariables()
                        .putValue("creditor", creditor)
                        .putValue("amount", amount)
                        .putValue("invoiceCategory", category)
                        .putValue("invoiceNumber", number)
                        .putValue("invoiceDocument", toInvoiceFileValue(inputStream))
        );
    }

    private static void addDays(Calendar calendar, int days) {
        calendar.add(DAY_OF_MONTH, days);
        setCurrentTime(calendar.getTime());
    }

    private static FileValue toInvoiceFileValue(InputStream inputStream) {
        return Variables.fileValue("invoice.pdf")
                .file(inputStream)
                .mimeType("application/pdf")
                .create();
    }

    private void startInvoiceProcess(Consumer<InputStream> consumer) {
        try (InputStream invoiceInputStream = getInputStream(INVOICE_PDF)) {
            consumer.accept(invoiceInputStream);
        } catch (IOException e) {
            throw new IllegalArgumentException(format("Couldn't read %s file", INVOICE_PDF));
        }
    }

    private ProcessDefinition getProcessDefinition(ProcessEngine processEngine, String processDefinitionKey, Integer version) {
        ProcessDefinitionQuery processDefinitionQuery = processEngine.getRepositoryService().createProcessDefinitionQuery().processDefinitionKey(processDefinitionKey);
        if (version != null) {
            processDefinitionQuery.processDefinitionVersion(version);
        } else {
            processDefinitionQuery.latestVersion();
        }
        return processDefinitionQuery.singleResult();
    }
}

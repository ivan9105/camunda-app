package org.camunda.app;

import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.camunda.app.process.InvoiceProcessApplication;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Resource;
import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.camunda.bpm.spring.boot.starter.event.PostDeployEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.EventListener;

import javax.annotation.PostConstruct;
import java.util.List;

@SpringBootApplication
@EnableProcessApplication
public class CamundaApplication {

    @Autowired
    protected ProcessEngine processEngine;

    //TODO make as component
    protected InvoiceProcessApplication invoiceProcess = new InvoiceProcessApplication();


    public static void main(String[] args) {
        // Avoid resetting URL stream handler factory
        TomcatURLStreamHandlerFactory.disable();
        SpringApplication.run(CamundaApplication.class, args).registerShutdownHook();
    }

    @PostConstruct
    public void deployInvoice() {
        //TODO check exists deployments
        RepositoryService repositoryService = processEngine.getRepositoryService();

//   TODO rename     org.camunda.bpm.example.invoice.service ->

//        if(processEngine.getIdentityService().createUserQuery().list().isEmpty()) {
//            ClassLoader classLoader = getClass().getClassLoader();
//            repositoryService
//                    .createDeployment()
//                    .addInputStream("invoice.v1.bpmn", classLoader.getResourceAsStream("invoice.v1.bpmn"))
//                    .addInputStream("reviewInvoice.bpmn", classLoader.getResourceAsStream("reviewInvoice.bpmn"))
//                    .deploy();
//        }
    }

    @EventListener
    public void onPostDeploy(PostDeployEvent event) {
        invoiceProcess.startFirstProcess(event.getProcessEngine());
    }
}

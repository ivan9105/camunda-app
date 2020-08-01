package org.camunda.app.generator;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.FilterService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.authorization.*;
import org.camunda.bpm.engine.filter.Filter;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;
import org.camunda.bpm.engine.impl.IdentityServiceImpl;
import org.camunda.bpm.engine.task.TaskQuery;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.asLifoQueue;
import static java.util.Collections.singletonList;
import static org.camunda.bpm.engine.authorization.Authorization.AUTH_TYPE_GLOBAL;
import static org.camunda.bpm.engine.authorization.Authorization.AUTH_TYPE_GRANT;
import static org.camunda.bpm.engine.authorization.Permissions.*;
import static org.camunda.bpm.engine.authorization.Resources.*;

@Slf4j
@NoArgsConstructor
public class SampleDataGenerator {

    private static final String CAMUNDA_ADMIN_GROUP_ID = "camunda-admin";
    private static final String SALES_GROUP_ID = "sales";
    private static final String ACCOUNTING_GROUP_ID = "accounting";
    private static final String MANAGEMENT_GROUP_ID = "management";

    private static final String WORKFLOW_GROUP_TYPE = "WORKFLOW";
    private static final String SYSTEM_GROUP_TYPE = "SYSTEM";

    private static final String ANY_RESOURCE_ID = "*";
    private static final String TASK_LIST_RESOURCE_ID = "tasklist";
    private static final String INVOICE_RESOURCE_ID = "invoice";

    private static final String DEMO_USER_ID = "demo";
    private static final String JOHN_USER_ID = "john";
    private static final String PETER_USER_ID = "peter";
    private static final String MARY_USER_ID = "mary";

    public void generate(ProcessEngine engine) {
        IdentityServiceImpl identityService = (IdentityServiceImpl) engine.getIdentityService();
        if (identityService.isReadOnly()) {
            log.info("Identity service provider is Read Only, not creating any demo users.");
            return;
        }

        User demoUser = identityService.createUserQuery().userId(DEMO_USER_ID).singleResult();
        if (demoUser != null) {
            log.info("Data was already init");
            return;
        }

        AuthorizationService authorizationService = engine.getAuthorizationService();
        FilterService filterService = engine.getFilterService();
        TaskService taskService = engine.getTaskService();

        createUsers(identityService);
        createGroups(identityService);
        createMemberShips(identityService);
        createAuthorizations(authorizationService);
        createFilters(authorizationService, filterService, taskService);
    }

    private void createFilters(AuthorizationService authorizationService, FilterService filterService, TaskService taskService) {
        Filter tasksFilter = createFilter(
                filterService,
                taskService.createTaskQuery().taskAssigneeExpression("${currentUser()}"),
                "Tasks assigned to me",
                -10,
                "My Tasks"
        );

        createAuthorization(authorizationService, null, null, singletonList(READ), tasksFilter.getId(), FILTER, AUTH_TYPE_GLOBAL);

        Filter groupTasksFilter = createFilter(
                filterService,
                taskService.createTaskQuery().taskCandidateGroupInExpression("${currentUserGroups()}").taskUnassigned(),
                "Tasks assigned to my Groups",
                -5,
                "My Group Tasks"
        );

        createAuthorization(authorizationService, null, null, singletonList(READ), groupTasksFilter.getId(), FILTER, AUTH_TYPE_GLOBAL);

        Filter accountGroupFilter = createFilter(
                filterService,
                taskService.createTaskQuery().taskCandidateGroupIn(singletonList(ACCOUNTING_GROUP_ID)).taskUnassigned(),
                "Tasks for Group Accounting",
                -3,
                "Accounting"
        );

        createAuthorization(authorizationService, ACCOUNTING_GROUP_ID, null, singletonList(READ), accountGroupFilter.getId(), FILTER, AUTH_TYPE_GRANT);

        createFilter(
                filterService,
                taskService.createTaskQuery().taskAssignee(JOHN_USER_ID),
                "Tasks assigned to John",
                -1,
                "John's Tasks"
        );

        createFilter(
                filterService,
                taskService.createTaskQuery().taskAssignee(MARY_USER_ID),
                "Tasks assigned to Mary",
                -1,
                "Mary's Tasks"
        );

        createFilter(
                filterService,
                taskService.createTaskQuery().taskAssignee(PETER_USER_ID),
                "Tasks assigned to Peter",
                -1,
                "Peter's Tasks"
        );

        createFilter(
                filterService,
                taskService.createTaskQuery(),
                "All Tasks - Not recommended to be used in production :)",
                10,
                "All Tasks"
        );
    }

    private void createMemberShips(IdentityServiceImpl identityService) {
        createMembership(identityService, DEMO_USER_ID, SALES_GROUP_ID, ACCOUNTING_GROUP_ID, MANAGEMENT_GROUP_ID, CAMUNDA_ADMIN_GROUP_ID);
        createMembership(identityService, JOHN_USER_ID, SALES_GROUP_ID);
        createMembership(identityService, MARY_USER_ID, ACCOUNTING_GROUP_ID);
        createMembership(identityService, PETER_USER_ID, MANAGEMENT_GROUP_ID);
    }

    private void createAuthorizations(AuthorizationService authorizationService) {
        Arrays.stream(Resources.values())
                .filter(resource -> isResourceForbidden(authorizationService, resource))
                .forEach(resource -> createAdminAuthorization(authorizationService, resource));
        createGroupAuthorization(authorizationService, SALES_GROUP_ID, ACCESS, TASK_LIST_RESOURCE_ID, APPLICATION);
        createAuthorization(authorizationService, SALES_GROUP_ID, null, asList(READ, READ_HISTORY), INVOICE_RESOURCE_ID, PROCESS_DEFINITION, AUTH_TYPE_GRANT);
        createGroupAuthorization(authorizationService, ACCOUNTING_GROUP_ID, ACCESS, TASK_LIST_RESOURCE_ID, APPLICATION);
        createAuthorization(authorizationService, ACCOUNTING_GROUP_ID, null, asList(READ, READ_HISTORY), INVOICE_RESOURCE_ID, PROCESS_DEFINITION, AUTH_TYPE_GRANT);
        createGroupAuthorization(authorizationService, MANAGEMENT_GROUP_ID, ACCESS, TASK_LIST_RESOURCE_ID, APPLICATION);
        createAuthorization(authorizationService, MANAGEMENT_GROUP_ID, null, asList(READ, READ_HISTORY), INVOICE_RESOURCE_ID, PROCESS_DEFINITION, AUTH_TYPE_GRANT);

        createGroupAuthorization(authorizationService, SALES_GROUP_ID, READ, DEMO_USER_ID, USER);
        createGroupAuthorization(authorizationService, SALES_GROUP_ID, READ, JOHN_USER_ID, USER);
        createGroupAuthorization(authorizationService, MANAGEMENT_GROUP_ID, READ, DEMO_USER_ID, USER);
        createGroupAuthorization(authorizationService, MANAGEMENT_GROUP_ID, READ, PETER_USER_ID, USER);
        createGroupAuthorization(authorizationService, ACCOUNTING_GROUP_ID, READ, DEMO_USER_ID, USER);
        createGroupAuthorization(authorizationService, ACCOUNTING_GROUP_ID, READ, MARY_USER_ID, USER);
        createUserAuthorization(authorizationService, MARY_USER_ID, TASK, ANY_RESOURCE_ID, asList(READ, UPDATE));
    }

    private void createGroups(IdentityServiceImpl identityService) {
        createGroup(identityService, SALES_GROUP_ID, "Sales", WORKFLOW_GROUP_TYPE);
        createGroup(identityService, ACCOUNTING_GROUP_ID, "Accounting", WORKFLOW_GROUP_TYPE);
        createGroup(identityService, MANAGEMENT_GROUP_ID, "Management", WORKFLOW_GROUP_TYPE);
        if (!isExistsGroup(identityService, CAMUNDA_ADMIN_GROUP_ID)) {
            createGroup(identityService, CAMUNDA_ADMIN_GROUP_ID, "camunda BPM Administrators", SYSTEM_GROUP_TYPE);
        }
    }

    private void createUsers(IdentityServiceImpl identityService) {
        createUser(identityService, DEMO_USER_ID, "Demo", "Demo", "demo", "demo@camunda.org");
        createUser(identityService, JOHN_USER_ID, "John", "Doe", "john", "john@camunda.org");
        createUser(identityService, MARY_USER_ID, "Mary", "Anne", "mary", "mary@camunda.org");
        createUser(identityService, PETER_USER_ID, "Peter", "Meter", "peter", "peter@camunda.org");
    }

    private static Filter createFilter(
            FilterService filterService,
            TaskQuery query,
            String description,
            int priority,
            String name
    ) {
        Filter taskFilter = filterService.newTaskFilter()
                .setName(name)
                .setProperties(createFilterProperties(description, priority))
                .setOwner(DEMO_USER_ID)
                .setQuery(query);
        return filterService.saveFilter(taskFilter);
    }

    private static Map<String, Object> createFilterProperties(String description, int priority) {
        Map<String, Object> filterProperties = new HashMap<>();
        filterProperties.put("description", description);
        filterProperties.put("priority", priority);
        filterProperties.put("variables", createFilterVariables());
        return filterProperties;
    }

    private static List<Map<String, String>> createFilterVariables() {
        List<Map<String, String>> variables = new ArrayList<>();
        addFilterVariable(variables, "amount", "Invoice Amount");
        addFilterVariable(variables, "invoiceNumber", "Invoice Number");
        addFilterVariable(variables, "creditor", "Creditor");
        addFilterVariable(variables, "approver", "Approver");
        return variables;
    }

    protected static void addFilterVariable(List<Map<String, String>> variables, String name, String label) {
        Map<String, String> variable = new HashMap<>();
        variable.put("name", name);
        variable.put("label", label);
        variables.add(variable);
    }

    private static void createUserAuthorization(
            AuthorizationService authorizationService,
            String userId,
            Resource resource,
            String resourceId,
            List<Permission> permissions
    ) {
        createAuthorization(authorizationService, null, userId, permissions, resourceId, resource, AUTH_TYPE_GRANT);
    }

    private static void createMembership(IdentityServiceImpl identityService, String userId, String... groupIds) {
        for (String groupId : groupIds) {
            identityService.createMembership(userId, groupId);
        }
    }

    private static void createAdminAuthorization(AuthorizationService authorizationService, Resource resource) {
        createGroupAuthorization(authorizationService, CAMUNDA_ADMIN_GROUP_ID, ALL, ANY_RESOURCE_ID, resource);
    }

    private static void createGroupAuthorization(
            AuthorizationService authorizationService,
            String groupId,
            Permission permission,
            String resourceId,
            Resource resource
    ) {
        createAuthorization(authorizationService, groupId, null, singletonList(permission), resourceId, resource, AUTH_TYPE_GRANT);
    }

    private static void createAuthorization(
            AuthorizationService authorizationService,
            String groupId,
            String userId,
            List<Permission> permissions,
            String resourceId,
            Resource resource,
            int type
    ) {
        Authorization authorization = authorizationService.createNewAuthorization(type);

        if (groupId != null) {
            authorization.setGroupId(groupId);
        }

        if (userId != null) {
            authorization.setUserId(userId);
        }

        authorization.setResourceId(resourceId);
        authorization.setResource(resource);
        permissions.forEach(authorization::addPermission);
        authorizationService.saveAuthorization(authorization);
    }

    private static boolean isResourceForbidden(AuthorizationService authorizationService, Resource resource) {
        return authorizationService.createAuthorizationQuery()
                .groupIdIn(CAMUNDA_ADMIN_GROUP_ID)
                .resourceType(resource)
                .resourceId(ANY_RESOURCE_ID)
                .count() == 0L;
    }

    private static boolean isExistsGroup(IdentityServiceImpl identityService, String id) {
        return identityService.createGroupQuery().groupId(id).count() > 0;
    }

    private static void createGroup(
            IdentityServiceImpl identityService,
            String id,
            String name,
            String type
    ) {
        Group group = identityService.newGroup(id);
        group.setName(name);
        group.setType(type);
        identityService.saveGroup(group);
    }

    private static void createUser(
            IdentityServiceImpl identityService,
            String id,
            String firstName,
            String lastName,
            String password,
            String email
    ) {
        User user = identityService.newUser(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPassword(password);
        user.setEmail(email);
        identityService.saveUser(user, true);
    }
}

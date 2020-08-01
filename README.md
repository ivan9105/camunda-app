You must add settings.xml for mvn to ~/.m2

<?xml version="1.0" encoding="UTF-8"?>
<settings>
   <servers>
      <server>
         <id>camunda-bpm-nexus-ee</id>
         <username>$USER_NAME</username>
         <password>$PASSWORD</password>
      </server>
   </servers>
</settings>

go to https://camunda.com/download/enterprise/
fill form and get "download trial license"
after that you will receive license key and credentials for a private repository https://app.camunda.com/nexus/content/repositories/camunda-bpm-ee

for editing *.bpmn, *.dmn files you need download modeler https://camunda.com/download/modeler/
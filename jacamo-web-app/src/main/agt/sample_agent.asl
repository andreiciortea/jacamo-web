// Agent sample_agent in project hellojacamo

/* Initial beliefs and rules */

/* Initial goals */

!start.

/* Plans */

+!start : true <- 
  .print("hello world.");
  createWorkspace("test");
  joinWorkspace("test", WkspId);
  .print("hello from test workspace!").

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }

// uncomment the include below to have an agent compliant with its organisation
//{ include("$jacamoJar/templates/org-obedient.asl") }

// Agent sample_agent in project hellojacamo

/* Initial beliefs and rules */

remote_wksp("http://localhost:8080/workspaces/local1").

/* Initial goals */

!start.

/* Plans */

+!start : remote_wksp(RemoteWorkspaceIRI) <- 
  .print("hello world.");
  joinWorkspace(RemoteWorkspaceIRI, RemoteWkspId);
  .print("hello from remote workspace!");
  .print("Focusing on clock artifact!");
  focusWhenAvailable("clock");
  .print("Focused on clock artifact!");
  .wait(5000);
  .print("Performing stop");
  stop[artifact_name("clock")].

//  .print("Done waiting for 5s, performing lookup...");
//  lookupArtifact("clock", ClockArtId);
//  .print("Found artifact id, stopping focus...");
//  stopFocus(ClockArtId).

+tick : true <- .print("Tick!").

+counter(V) : true <- .print("New value: ", V).


{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }

// uncomment the include below to have an agent compliant with its organisation
//{ include("$jacamoJar/templates/org-obedient.asl") }

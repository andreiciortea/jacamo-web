// Agent sample_agent in project hellojacamo

//{namespace(node,"http://localhost:8081/workspaces/")}

/* Initial beliefs and rules */

remote_wksp("http://localhost:8081/workspaces/remote1").
local_wksp("local1").

/* Initial goals */

!start.

/* Plans */

+!start : local_wksp(LocalWorkspaceName) <- 
  .print("hello world.");
  createWorkspace(LocalWorkspaceName);
  joinWorkspace(LocalWorkspaceName, LocalWkspId);
  .print("hello from local workspace!");
  makeArtifact("clock", "net.sf.ex.ClockArtifact", [], _);
  focusWhenAvailable("clock");
  .print("Created clock artifact!");
  start;
  .print("Clock started!").
//  .print("Joining remote workspace!");
//  joinWorkspace("http://localhost:8081/workspaces/remote1", RemoteWkspId);
//  .print("Hello from remote workspace!").

+tick : true <- .print("Tick!").

+counter(V) : true <- .print("New value: ", V).


{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }

// uncomment the include below to have an agent compliant with its organisation
//{ include("$jacamoJar/templates/org-obedient.asl") }

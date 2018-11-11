package net.sf.jacamo.web;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.rapidoid.http.Req;
import org.rapidoid.setup.On;

import com.google.gson.Gson;

import cartago.AgentBody;
import cartago.AgentIdCredential;
import cartago.ArtifactId;
import cartago.CartagoEvent;
import cartago.CartagoService;
import cartago.CartagoWorkspace;
import cartago.ICartagoCallback;
import cartago.ICartagoContext;
import cartago.ICartagoSession;
import cartago.Op;
import cartago.events.ActionFailedEvent;
import cartago.events.ActionSucceededEvent;
import cartago.events.ArtifactObsEvent;
import cartago.events.CartagoActionEvent;
import cartago.events.ConsultManualSucceededEvent;
import cartago.events.FocusSucceededEvent;
import cartago.events.JoinWSPSucceededEvent;
import cartago.events.QuitWSPSucceededEvent;
import cartago.events.StopFocusSucceededEvent;
import cartago.infrastructure.web.AgentCallbackRegistry;
import cartago.infrastructure.web.AgentIRIListener;
import io.vertx.core.json.JsonObject;
import jacamo.platform.Platform;
import net.sf.jacamo.web.models.Workspace;

public class HttpAPI implements Platform {
  
  private final static Logger LOGGER = Logger.getLogger(HttpAPI.class.getCanonicalName());
  
  Map<String, ICartagoSession> agentSessions;
  
  @Override
  public void init(String[] args) {
    agentSessions = new Hashtable<String, ICartagoSession>();
    
    On.setup().port(Integer.parseInt(args[0]));
    
    On.get("/").html("Hello world from rapidoid!");
    
    On.get("/workspaces").json( 
        (Req r) ->
            CartagoService.getWorkspaces().stream()
              .map(wksp -> new Workspace(constructRequestIRI(r), wksp))
              .collect(Collectors.toList())
      );
    
    On.post("/workspaces/{workspaceName}/agents/").json( 
        (String workspaceName, Req r) -> {
          JsonObject payload = new JsonObject(new String(r.body()));
          
          String agentName = payload.getString("agentName");
          String agentCallbackIRI = payload.getString("agentCallbackIRI");
          
          AgentIdCredential agentCred = new AgentIdCredential(agentName);
          AgentIRIListener agentCallback = new AgentIRIListener(agentCallbackIRI);
          
          if (!agentSessions.containsKey(agentName)) {
            ICartagoSession session = CartagoService.startSession(workspaceName, agentCred, agentCallback);
            agentSessions.put(agentName, session);
          }
          
          ICartagoSession agentSession = agentSessions.get(agentName);
          CartagoWorkspace workspace = CartagoService.getWorkspace(workspaceName);
          
          workspace.join(agentCred, (ICartagoCallback) agentSession);
          
          return new Workspace("", workspace);
        }
      );
    
    // TODO: generalize for all operations
//    On.put("/workspaces/{workspaceName}/artifacts/{artifactName}/observers/{agentName}").json(
//        (String workspaceName, String artifactName, String agentName) -> {
//          ICartagoSession agentSession = agentSessions.get(agentName);
//          
//          Optional<ArtifactId> wspArtId = Arrays.asList(CartagoService.getWorkspace(workspaceName).getController().getCurrentArtifacts())
//                                    .stream().filter(artId -> artId.getName().equalsIgnoreCase("workspace")).findFirst();
//          
//          if (wspArtId.isPresent()) {
//            // TODO: not using any alignment tests, but it seems they are not used in the JaCa bridge either
//            long actionId = agentSession.doAction(wspArtId.get(), new Op("focusWhenAvailable", artifactName), null, Long.MAX_VALUE);
//            
//            return "{ 'result' : " + actionId + " }";
//          }
//          
//          return "";
//        }
//      );
    
    On.post("/workspaces/{workspaceName}/operations").json(
        (String workspaceName, Req r) -> {
          JsonObject payload = new JsonObject(new String(r.body()));
          
          String agentName = payload.getString("agentName");
          String artifactName = payload.getString("artifactName");
          
          ICartagoSession agentSession = agentSessions.get(agentName);
          
          Optional<ArtifactId> wspArtId = Arrays.asList(CartagoService.getWorkspace(workspaceName).getController().getCurrentArtifacts())
                                    .stream().filter(artId -> artId.getName().equalsIgnoreCase(artifactName)).findFirst();
          
          if (wspArtId.isPresent()) {
            JsonObject operation = payload.getJsonObject("operation");
            
            String operationName = operation.getString("name");
            Object[] params = operation.getJsonArray("params").getList().toArray();
            
            LOGGER.info("Received operation: " + operationName + ", params: " + params);
            
            // TODO: not using any alignment tests, but it seems they are not used in the JaCa bridge either
            long actionId = agentSession.doAction(wspArtId.get(),new Op(operationName, params), null, Long.MAX_VALUE);
            
            return "" + actionId;
          }
          
          return null;
        }
      );
    
    On.post("/notifications/{workspaceName}/{agentName}").json(
        (String workspaceName, String agentName, Req r) -> {
          
          String agentIRI = "http://localhost:8081/notifications/" + workspaceName + "/" + agentName;
          Set<ICartagoContext> agentBodies = AgentCallbackRegistry.getInstance().getCartagoCallbacks(agentIRI);
          
          JsonObject notJson = new JsonObject(new String(r.body()));
          
          String eventClassName = notJson.getString("className");
          String eventStr = notJson.getString("event");
          
          /** CArtAgO Action Succeeded Events **/
          if (eventClassName.equalsIgnoreCase(FocusSucceededEvent.class.getCanonicalName())) {
            FocusSucceededEvent event = (new Gson()).fromJson(eventStr, FocusSucceededEvent.class);
            notifyCartagoEvent(agentBodies, updateActionId(agentIRI, event));
          }
          else if (eventClassName.equalsIgnoreCase(StopFocusSucceededEvent.class.getCanonicalName())) {
            StopFocusSucceededEvent event = (new Gson()).fromJson(eventStr, StopFocusSucceededEvent.class);
            notifyCartagoEvent(agentBodies, updateActionId(agentIRI, event));
          }
          else if (eventClassName.equalsIgnoreCase(JoinWSPSucceededEvent.class.getCanonicalName())) {
            JoinWSPSucceededEvent event = (new Gson()).fromJson(eventStr, JoinWSPSucceededEvent.class);
            notifyCartagoEvent(agentBodies, updateActionId(agentIRI, event));
          }
          else if (eventClassName.equalsIgnoreCase(QuitWSPSucceededEvent.class.getCanonicalName())) {
            QuitWSPSucceededEvent event = (new Gson()).fromJson(eventStr, QuitWSPSucceededEvent.class);
            notifyCartagoEvent(agentBodies, updateActionId(agentIRI, event));
          }
          else if (eventClassName.equalsIgnoreCase(ConsultManualSucceededEvent.class.getCanonicalName())) {
            ConsultManualSucceededEvent event = (new Gson()).fromJson(eventStr, ConsultManualSucceededEvent.class);
            notifyCartagoEvent(agentBodies, updateActionId(agentIRI, event));
          }
          else if (eventClassName.equalsIgnoreCase(ActionSucceededEvent.class.getCanonicalName())) {
            ActionSucceededEvent event = (new Gson()).fromJson(eventStr, ActionSucceededEvent.class);
            notifyCartagoEvent(agentBodies, updateActionId(agentIRI, event));
          }
          /** CArtAgO Action Failed Event **/
          else if (eventClassName.equalsIgnoreCase(ActionFailedEvent.class.getCanonicalName())) {
            ActionFailedEvent event = (new Gson()).fromJson(eventStr, ActionFailedEvent.class);
            notifyCartagoEvent(agentBodies, updateActionId(agentIRI, event));
          }
          /** Artifact Observable Event **/
          else if (eventClassName.equalsIgnoreCase(ArtifactObsEvent.class.getCanonicalName())) {
            ArtifactObsEvent event = (new Gson()).fromJson(eventStr, ArtifactObsEvent.class);
            notifyCartagoEvent(agentBodies, event);
          }
          else {
            return null;
          }
          
          return "Notification received";
        }
      );
    
  }
  
  private CartagoActionEvent updateActionId(String agentIRI, CartagoActionEvent event) {
    long localActionId = AgentCallbackRegistry.getInstance().getLocalActionId(agentIRI, event.getActionId());
    event.setActionId(localActionId);
    
    return event;
  }
  
  private void notifyCartagoEvent(Set<ICartagoContext> agentBodies, CartagoEvent event) {
    for (ICartagoContext body : agentBodies) {
      ((AgentBody) body).getCallback().notifyCartagoEvent(event);
    }
  }
  
  private String constructRequestIRI(Req request) {
    return "http://" + request.host() + request.path();
  }
  
}

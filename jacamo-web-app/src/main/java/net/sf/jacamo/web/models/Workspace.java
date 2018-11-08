package net.sf.jacamo.web.models;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import cartago.CartagoException;
import cartago.CartagoWorkspace;

public class Workspace {
  
  @NotNull
  public String iri;
  
  @NotNull
  public String name;
  
  public Collection<String> agents;
  public Collection<String> artifacts;
  
  public Workspace(String baseIri, CartagoWorkspace wksp) {
    name = wksp.getId().getName();
    
    if (!baseIri.endsWith("/")) {
      baseIri = baseIri.concat("/");
    }
    
    iri = baseIri + name;
    
    try {
      
      agents = Arrays.asList(wksp.getController().getCurrentAgents()).stream()
                .map(aId -> aId.getAgentName())
                .collect(Collectors.toList());
      
      artifacts = Arrays.asList(wksp.getController().getCurrentArtifacts()).stream()
                  .map(aId -> aId.getName())
                  .collect(Collectors.toList());
      
    } catch (CartagoException e) {
      e.printStackTrace();
    }
  }
  
}

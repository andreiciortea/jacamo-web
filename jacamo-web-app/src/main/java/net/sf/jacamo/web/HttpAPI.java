package net.sf.jacamo.web;

import java.util.stream.Collectors;

import org.rapidoid.http.Req;
import org.rapidoid.setup.On;

import cartago.CartagoService;
import jacamo.platform.Platform;
import net.sf.jacamo.web.models.Workspace;

public class HttpAPI implements Platform {
  
  @Override
  public void init(String[] args) {
    On.get("/").html("Hello world from rapidoid!");
    
    On.get("/workspaces").json( 
        (Req r) ->
            CartagoService.getWorkspaces().stream()
              .map(wksp -> new Workspace(constructRequestIRI(r), wksp))
              .collect(Collectors.toList())
      );
  }
  
  private String constructRequestIRI(Req request) {
    return "http://" + request.host() + request.path();
  }
  
}

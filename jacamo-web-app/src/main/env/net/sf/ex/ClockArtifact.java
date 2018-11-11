package net.sf.ex;
// CArtAgO artifact code for project jacamo-web-app

import cartago.*;

public class ClockArtifact extends Artifact {
  boolean counting;
  final static long TICK_TIME = 1000;

  void init(){
    counting = false;
    
    defineObsProperty("counter", 0);
  }

  @OPERATION void start(){
    if (!counting){
      counting = true;
      execInternalOp("count");
    } else {
      failed("already_counting");
    }
  }

  @OPERATION void stop(){
    counting = false;
  }

  @INTERNAL_OPERATION void count(){
    while (counting){
      ObsProperty prop = getObsProperty("counter");
      prop.updateValue(prop.intValue() + 1);
      
      signal("tick");
      
      await_time(TICK_TIME);
    }
  }
}


/**
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 */

package jdocs.tutorial_5;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.HashMap;
import java.util.Map;

public class DeviceManager extends AbstractActor {
  private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  public static Props props() {
    return Props.create(DeviceManager.class);
  }

  final static class RequestTrackDevice {
    public final String groupId;
    public final String deviceId;

    public RequestTrackDevice(String groupId, String deviceId) {
      this.groupId = groupId;
      this.deviceId = deviceId;
    }
  }

  final static class DeviceRegistered {
  }

  Map<String, ActorRef> groupIdToActor = new HashMap<>();
  Map<ActorRef, String> actorToGroupId = new HashMap<>();

  @Override
  public void preStart() {
    log.info("DeviceManager started");
  }

  @Override
  public void postStop() {
    log.info("DeviceManager stopped");
  }

  public Receive createReceive() {
    return receiveBuilder()
            .match(RequestTrackDevice.class, trackMsg -> {
              String groupId = trackMsg.groupId;
              ActorRef ref = groupIdToActor.get(groupId);
              if (ref != null) {
                ref.forward(trackMsg, getContext());
              } else {
                log.info("Creating device group actor for {}", groupId);
                ActorRef groupActor = getContext().actorOf(DeviceGroup.props(groupId), "group-" + groupId);
                getContext().watch(groupActor);
                groupActor.forward(trackMsg, getContext());
                groupIdToActor.put(groupId, groupActor);
                actorToGroupId.put(groupActor, groupId);
              }
            })
            .match(Terminated.class, t -> {
              ActorRef groupActor = t.getActor();
              String groupId = actorToGroupId.get(groupActor);
              log.info("Device group actor for {} has been terminated", groupId);
              actorToGroupId.remove(groupActor);
              groupIdToActor.remove(groupId);
            })
            .build();
  }

}
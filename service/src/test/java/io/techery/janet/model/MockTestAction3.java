package io.techery.janet.model;

import io.techery.janet.proxy.LabeledAction;

@MockServiceAction
public class MockTestAction3 implements LabeledAction {
  @Override public String getLabel() {
    return "service3";
  }
}

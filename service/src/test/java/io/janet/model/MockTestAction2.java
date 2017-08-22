package io.janet.model;

@MockServiceAction
public class MockTestAction2 implements LabeledAction {
  @Override public String label() {
    return "service2";
  }
}

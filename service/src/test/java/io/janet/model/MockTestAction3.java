package io.janet.model;

@MockServiceAction
public class MockTestAction3 implements LabeledAction {
  @Override public String label() {
    return "service3";
  }
}

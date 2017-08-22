package io.janet.model;

@OtherServiceAction
public class OtherTestAction implements LabeledAction {
  @Override public String label() {
    return "some_service";
  }
}

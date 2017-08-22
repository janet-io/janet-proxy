package io.janet;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.janet.model.LabeledAction;
import io.janet.model.MockServiceAction;
import io.janet.model.MockTestAction1;
import io.janet.model.MockTestAction2;
import io.janet.model.MockTestAction3;
import io.janet.model.OtherServiceAction;
import io.janet.proxy.ServiceMappingRule;
import io.reactivex.observers.TestObserver;
import io.reactivex.subscribers.TestSubscriber;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProxyTest {

  protected Janet janet;
  protected ActionService service1;
  protected ActionService service2;
  protected ActionPipe<MockTestAction1> actionPipe1;
  protected ActionPipe<MockTestAction2> actionPipe2;

  @Before public void setup() throws JanetException {
    service1 = provideService();
    service2 = provideService();
    janet = new Janet.Builder()
        .addService(new ProxyService.Builder(MockServiceAction.class)
            .add(service1, (ServiceMappingRule<LabeledAction>) action -> action.label().equals("service1"))
            .add(service2, (ServiceMappingRule<LabeledAction>) action -> action.label().equals("service2"))
            .build()
        ).build();
    actionPipe1 = janet.createPipe(MockTestAction1.class);
    actionPipe2 = janet.createPipe(MockTestAction2.class);
  }

  protected ActionService provideService() throws JanetException {
    ActionService service = Mockito.spy(ActionService.class);
    when(service.getSupportedAnnotationType()).thenReturn(MockServiceAction.class);
    doAnswer(new AssertUtil.SuccessAnswer(service)).when(service).sendInternal(any(ActionHolder.class));
    return service;
  }

  @Test public void serviceRoutingForObservables() {
    TestSubscriber<ActionState<MockTestAction1>> subscriber1 =
        actionPipe1.createObservable(new MockTestAction1()).test();
    AssertUtil.SuccessAnswer.assertAllStatuses(subscriber1);

    TestSubscriber<ActionState<MockTestAction2>> subscriber2 =
        actionPipe2.createObservable(new MockTestAction2()).test();
    AssertUtil.SuccessAnswer.assertAllStatuses(subscriber2);

    verify(service1, Mockito.times(1)).send(any(ActionHolder.class));
    verify(service2, Mockito.times(1)).send(any(ActionHolder.class));
  }

  @Test public void serviceRoutingForSendObserve() {
    TestSubscriber<ActionState<MockTestAction1>> subscriber1 = actionPipe1.observe().test();
    TestSubscriber<ActionState<MockTestAction2>> subscriber2 = actionPipe2.observe().test();

    actionPipe1.send(new MockTestAction1());
    //
    AssertUtil.SuccessAnswer.assertAllStatuses(subscriber1);
    verify(service1, Mockito.times(1)).send(any(ActionHolder.class));
    verify(service2, Mockito.never()).send(any(ActionHolder.class));

    actionPipe2.send(new MockTestAction2());
    //
    AssertUtil.SuccessAnswer.assertAllStatuses(subscriber2);
    verify(service1, Mockito.times(1)).send(any(ActionHolder.class));
    verify(service2, Mockito.times(1)).send(any(ActionHolder.class));
  }


  @Test public void serviceCreationChecks() {
    // check service is created of non-annotation class
    Assertions.assertThatThrownBy(() ->
        new ProxyService.Builder(AssertUtil.class).build()).isInstanceOf(IllegalArgumentException.class);

    // check service is created with no sub-services
    Assertions.assertThatThrownBy(() ->
        new ProxyService.Builder(MockServiceAction.class).build()).isInstanceOf(IllegalStateException.class);


    // check service is created with unsupported sub-service
    Assertions.assertThatThrownBy(() -> {
      ActionService service = mock(ActionService.class);
      when(service.getSupportedAnnotationType()).thenReturn(OtherServiceAction.class);
      new ProxyService.Builder(MockServiceAction.class)
          .add(service, action -> true)
          .build();
    }).isInstanceOf(IllegalArgumentException.class);
  }

  @Test public void checkWrongActionException() {
    TestObserver<MockTestAction3> observer = janet.createPipe(MockTestAction3.class)
        .createObservableResult(new MockTestAction3())
        .test();
    observer.assertError(t -> t instanceof JanetInternalException
        && t.getCause() instanceof IllegalArgumentException
        && t.getLocalizedMessage().contains("Cant find proper service")
    );
  }

  @Test public void checkServiceExceptionThrownGracefully() throws JanetException {
    ActionService service = Mockito.spy(ActionService.class);
    when(service.getSupportedAnnotationType()).thenReturn(MockServiceAction.class);
    doAnswer(invocation -> {
      throw new JanetException(new RuntimeException("Some service exception"));
    }).when(service).sendInternal(any(ActionHolder.class));
    Janet janet = new Janet.Builder()
        .addService(new ProxyService.Builder(MockServiceAction.class).add(service, action -> true).build())
        .build();
    //
    TestObserver<MockTestAction1> subscriber = janet.createPipe(MockTestAction1.class)
        .createObservableResult(new MockTestAction1())
        .test();
    Assertions.assertThat(subscriber.errors().get(0).getCause())
        .hasCauseExactlyInstanceOf(RuntimeException.class)
        .hasMessageContaining("Some service exception");
  }

}

package io.janet;

import java.util.LinkedList;
import java.util.List;

import io.janet.proxy.ServiceMappingRule;

final public class ProxyService extends ActionService {

  private Class supportedAnnotation;
  private List<ServiceRuleTuple> entries;

  ProxyService(Class supportedAnnotation, List<ServiceRuleTuple> entries) {
    this.supportedAnnotation = supportedAnnotation;
    this.entries = entries;
  }

  @Override protected Class getSupportedAnnotationType() {
    return supportedAnnotation;
  }

  @Override void setCallback(Callback callback) {
    super.setCallback(callback);
    for (ServiceRuleTuple tuple : entries) {
      tuple.service.setCallback(callback);
    }
  }

  @SuppressWarnings("unchecked")
  @Override protected <A> void sendInternal(ActionHolder<A> holder) throws JanetException {
    findService(holder).sendInternal(holder);
  }

  @Override protected <A> void cancel(ActionHolder<A> holder) {
    findService(holder).cancel(holder);
  }

  private ActionService findService(ActionHolder holder) throws JanetInternalException {
    for (ServiceRuleTuple entry : entries) {
      if (entry.rule.matches(holder.action())) return entry.service;
    }
    throw new JanetInternalException(
        new IllegalArgumentException("Cant find proper service for " + holder.action().getClass().getName())
    );
  }

  public static class Builder {
    private Class supportedAnnotation;
    private List<ServiceRuleTuple> entries;

    public Builder(Class supportedAnnotation) {
      if (!supportedAnnotation.isAnnotation()) {
        throw new IllegalArgumentException(supportedAnnotation.getCanonicalName() + " is not an annotation");
      }
      this.supportedAnnotation = supportedAnnotation;
      this.entries = new LinkedList<ServiceRuleTuple>();
    }

    public Builder add(ActionService service, ServiceMappingRule rule) {
      if (supportedAnnotation != service.getSupportedAnnotationType()) {
        String message = String.format("Service with unsupported annotation: expected - %s but service has %s",
            supportedAnnotation.getCanonicalName(), service.getSupportedAnnotationType().getCanonicalName());
        throw new IllegalArgumentException(message);
      }
      entries.add(new ServiceRuleTuple(service, rule));
      return this;
    }

    public ProxyService build() {
      if (entries.isEmpty()) throw new IllegalStateException("No service added, can't operate");
      return new ProxyService(supportedAnnotation, entries);
    }
  }

  static class ServiceRuleTuple {
    public final ActionService service;
    public final ServiceMappingRule rule;

    ServiceRuleTuple(ActionService service, ServiceMappingRule rule) {
      this.service = service;
      this.rule = rule;
    }
  }

}

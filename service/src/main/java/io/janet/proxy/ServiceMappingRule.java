package io.janet.proxy;

public interface ServiceMappingRule<T> {
  boolean matches(T action);
}

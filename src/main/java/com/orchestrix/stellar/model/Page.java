package com.orchestrix.stellar.model;

public record Page(int limit, int offset) {
  public static Page ofLimit(int limit) { return new Page(limit, 0); }
  public static Page of(int limit, int offset) { return new Page(limit, offset); }
}

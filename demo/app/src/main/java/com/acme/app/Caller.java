package com.acme.app;

// Flagged: InternalHelper is declared in the "core" module's internal
// package, but this file lives in the "app" module.
import com.acme.internal.InternalHelper;

public class Caller {
    private final InternalHelper helper = new InternalHelper();
}

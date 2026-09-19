package io.roleos.experience.importer;

/** One deterministic, structured item accepted by the fake adapter. */
public record ResumeImportFixture(String candidateType, String payload, String sourceLocation) {}

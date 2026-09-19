package io.roleos.experience.importer;

/** External parsing boundary. Returned candidates are proposals, never confirmed career facts. */
@FunctionalInterface
public interface ResumeImportPort {
  ResumeImportResult extract(ResumeImportRequest request);
}

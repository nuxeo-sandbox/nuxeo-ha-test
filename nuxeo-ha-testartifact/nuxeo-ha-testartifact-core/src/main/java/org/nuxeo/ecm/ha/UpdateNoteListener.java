package org.nuxeo.ecm.ha;

import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkManager.Scheduling;
import org.nuxeo.runtime.api.Framework;

public class UpdateNoteListener implements EventListener {

  protected final List<String> handled = Arrays.asList("documentCreated");

  @Override
  public void handleEvent(Event event) {
    EventContext ctx = event.getContext();
    if (!(ctx instanceof DocumentEventContext)) {
      return;
    }

    DocumentEventContext docCtx = (DocumentEventContext) ctx;
    DocumentModel doc = docCtx.getSourceDocument();
    if ("Note".equals(doc.getType())) {
      Work work = new DocUpdaterWork(doc.getRepositoryName(), doc.getId());
      Framework.getService(WorkManager.class).schedule(work, Scheduling.IF_NOT_RUNNING_OR_SCHEDULED);
    }
  }
}

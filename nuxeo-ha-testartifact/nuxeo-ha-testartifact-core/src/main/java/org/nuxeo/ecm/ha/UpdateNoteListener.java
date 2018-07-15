package org.nuxeo.ecm.ha;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkManager.Scheduling;
import org.nuxeo.runtime.api.Framework;

public class UpdateNoteListener implements EventListener, PostCommitEventListener {

    private static final Log log = LogFactory.getLog(DocUpdaterWork.class);

    private final Object mutex = new Object();

    private WorkManager workManager = null;

    private String updateType = "Note";

    @Override
    public void handleEvent(EventBundle events) {
        for (Event event : events) {
            handleEvent(event);
        }
    }

    @Override
    public void handleEvent(Event event) {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }

        synchronized (mutex) {
            if (workManager == null) {
                workManager = Framework.getService(WorkManager.class);
                updateType = Framework.getProperty("nuxeo.ha.listener.updateType", "Note");
            }
        }

        DocumentEventContext docCtx = (DocumentEventContext) ctx;
        DocumentModel doc = docCtx.getSourceDocument();
        if (updateType.equals(doc.getType())) {
            if (log.isDebugEnabled()) {
                log.debug("Performing update: " + doc.getId() + "@" + doc.getVersionLabel() + ", " + event.getName());
            }
            Work work = new DocUpdaterWork(doc.getRepositoryName(), doc.getId());
            this.workManager.schedule(work, Scheduling.ENQUEUE, true);
        }
    }
}

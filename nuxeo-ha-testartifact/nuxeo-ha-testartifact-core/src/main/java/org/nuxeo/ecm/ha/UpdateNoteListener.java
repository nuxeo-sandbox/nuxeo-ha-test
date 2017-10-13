package org.nuxeo.ecm.ha;

import java.util.Arrays;
import java.util.List;

import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

public class UpdateNoteListener implements PostCommitFilteringEventListener {

    protected final List<String> handled = Arrays.asList("documentCreated");

    @Override
    public void handleEvent(EventBundle events) {
        for (Event event : events) {
            if (acceptEvent(event)) {
                handleEvent(event);
            }
        }
    }

    @Override
    public boolean acceptEvent(Event event) {
        return handled.contains(event.getName());
    }

    public void handleEvent(Event event) {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }

        DocumentEventContext docCtx = (DocumentEventContext) ctx;
        DocumentModel doc = docCtx.getSourceDocument();
        if ("Note".equals(doc.getType())) {
            try {
                Thread.sleep(Integer.parseInt(Framework.getProperty("nuxeo.ha.listener.duration", "2000")));
                doc.setPropertyValue("dc:description", "updated");
                doc.getCoreSession().saveDocument(doc);
                doc.getCoreSession().save();
            } catch (InterruptedException e) {
                ExceptionUtils.checkInterrupt(e);
            }
        }
    }
}

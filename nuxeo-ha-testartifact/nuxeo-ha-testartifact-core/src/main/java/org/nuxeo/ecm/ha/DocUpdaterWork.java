/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.ha;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.runtime.api.Framework;

/**
 *
 */
public class DocUpdaterWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DocUpdaterWork.class);

    public DocUpdaterWork(String repositoryName, String id) {
        super(id);
        setDocument(repositoryName, id);
    }

    @Override
    public String getCategory() {
        return "hatest";
    }

    @Override
    public String getTitle() {
        return "ha-test-updater";
    }

    @Override
    public void work() {
        log.debug("Running update worker for doc: " + docId);
        // Check parameters (period * rounds) for document existence
        long checkPeriod = Math.abs(Long.parseLong(Framework.getProperty("nuxeo.ha.listener.checkPeriod", "500")));
        int checkRounds = Math.abs(Integer.parseInt(Framework.getProperty("nuxeo.ha.listener.checkRounds", "100")));

        // Open session
        openSystemSession();

        // Check document reference
        IdRef docRef = new IdRef(docId);
        while (checkRounds-- > 0) {
            // Open session
            log.debug("Checking for existence of doc, round: " + checkRounds);
            if (!session.exists(docRef)) {
                if (checkRounds == 0) {
                    // doc does not exist
                    log.error("Document with ID not found: " + docId);
                    return;
                } else {
                    // Close the session to be opened later
                    // closeSession();

                    // doc has not appeared, wait
                    try {
                        log.debug("No doc yet @ round " + checkRounds);
                        commitOrRollbackTransaction();
                        Thread.sleep(checkPeriod);
                        startTransaction();
                    } catch (InterruptedException iex) {
                        log.error("Interrupted during sleep");
                    }
                }
            } else {
                // Found!
                log.debug("Document found!");
                break;
            }
        }

        // Resolve document
        DocumentModel doc = null;
        if (session.exists(docRef)) {
            doc = session.getDocument(docRef);
            log.debug("Found document: " + docRef);
        } else {
            log.warn("No such document: " + docId);
            return;
        }

        // Check for configured sleep value
        Property prop = doc.getProperty("dc:source");
        String val = prop.getValue(String.class);
        if (val == null) {
            val = Framework.getProperty("nuxeo.ha.listener.duration", "1500");
        }

        // Work delay
        try {
            log.debug("Sleeping for ms: " + val);
            Long delay = Long.parseLong(val);
            Thread.sleep(delay);
        } catch (Exception ex) {
        }

        // Update doc
        log.debug("Updating document...");
        doc.setPropertyValue("dc:description", "updated");
        session.saveDocument(doc);
        session.save();

        closeSession();
    }

}

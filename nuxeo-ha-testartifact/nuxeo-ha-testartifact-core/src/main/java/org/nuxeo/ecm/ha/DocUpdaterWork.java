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

import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.runtime.api.Framework;

/**
 *
 */
public class DocUpdaterWork extends AbstractWork {


    private static final long serialVersionUID = 1L;



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
        openSystemSession();

        IdRef docRef = new IdRef(docId);
        if (!session.exists(docRef)) {
            // doc is gone
            return;
        }
        DocumentModel doc = session.getDocument(docRef);
        try {
            Thread.sleep(Integer.parseInt(Framework.getProperty("nuxeo.ha.listener.duration", "2000")));
            doc.setPropertyValue("dc:description", "updated");
            session.saveDocument(doc);
        } catch (InterruptedException e) {
            ExceptionUtils.checkInterrupt(e);
        }
    }



}

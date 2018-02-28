package org.nuxeo.ecm.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Empty Unit Testing class.
 * <p/>
 *
 * @see <a href="https://doc.nuxeo.com/corg/unit-testing/">Unit Testing</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.ha.nuxeo-ha-testartifact")
public class LongAsynchronousUpdaterTest {

  @Inject
  protected CoreSession session;

  @Inject
  WorkManager workManager;

  @Test
  public void propertyIsUpdateAsynchronouslyAndAfterALongTime() throws Exception {
    DocumentModel doc = session.createDocumentModel("/", "sample", "Note");
    doc = session.createDocument(doc);
    assertNull(doc.getProperty("dc:description").getValue(String.class));

    waitForWorkers();

    doc = session.getDocument(new PathRef("/sample"));
    assertEquals("updated", doc.getProperty("dc:description").getValue(String.class));

  }

  protected void waitForWorkers() throws InterruptedException {
    TransactionHelper.commitOrRollbackTransaction();
    TransactionHelper.startTransaction();
    
    final boolean allCompleted = workManager.awaitCompletion(10, TimeUnit.SECONDS);
    assertTrue(allCompleted);
  }
}

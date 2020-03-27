/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.packager;

import java.io.File;
import java.util.EventListener;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.maven.plugin.MojoExecutionException;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.component.internal.multi.ComponentManagerManager;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.internal.DefaultWikiComponentManagerEventListener;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.eventstream.store.internal.DocumentEventListener;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.platform.wiki.creationjob.WikiCreationRequest;

import com.xpn.xwiki.web.Utils;

/**
 * Helper to manipulate APIs needed for the Wiki Mojo.
 *
 * @version $Id: 367c58283ae8ed2b8426ca64744850e9210dd03d $
 * @since 9.5RC1
 */
@Component(roles = WikiHelper.class)
@Singleton
public class WikiHelper implements AutoCloseable
{
    @Inject
    private Execution execution;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private DocumentReferenceResolver<String> stringDocumentReferenceResolver;

    @Inject
    private JobExecutor jobExecutor;

    @Inject
    private ExecutionContextManager ecim;

    @Inject
    private ComponentManagerManager componentManagerManager;

    private File hibernateConfig;

    private boolean disposeComponentManager;

    /**
     * Public for technical reason, {@link #create(File)} should be used instead.
     */
    public WikiHelper()
    {
    }

    /**
     * @param hibernateConfig the hibernate configuration to use
     * @return an initialized instance of {@link WikiHelper}
     * @throws MojoExecutionException when failing to initialize {@link WikiHelper} instance
     */
    public static WikiHelper create(File hibernateConfig) throws MojoExecutionException
    {
        // Create and initialize a Component Manager
        EmbeddableComponentManager embeddableComponentManager =
                (EmbeddableComponentManager) org.xwiki.environment.System.initialize();

        // Initialize Execution Context
        try {
            ExecutionContextManager ecim = embeddableComponentManager.getInstance(ExecutionContextManager.class);
            ecim.initialize(new ExecutionContext());
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to initialize Execution Context Manager.", e);
        }

        WikiHelper wikiHelper = create(embeddableComponentManager, hibernateConfig);

        wikiHelper.disposeComponentManager = true;

        return wikiHelper;
    }

    /**
     * @param componentManager the component manager
     * @param hibernateConfig the hibernate configuration to use
     * @return a {@link WikiHelper}
     * @throws MojoExecutionException when failing to get the {@link WikiHelper} instance
     */
    public static WikiHelper create(ComponentManager componentManager, File hibernateConfig)
            throws MojoExecutionException
    {
        WikiHelper wikiHelper;
        try {
            wikiHelper = componentManager.getInstance(WikiHelper.class);
            wikiHelper.initialize(hibernateConfig);
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to get WikiHelper component", e);
        }

        return wikiHelper;
    }

    private void initialize(File hibernateConfig) throws Exception
    {
        this.hibernateConfig = hibernateConfig;
    }

    @Override
    public void close() throws Exception
    {
        Utils.setComponentManager(null);

        if (this.disposeComponentManager) {
            this.execution.removeContext();

            org.xwiki.environment.System.dispose(this.componentManager);
        }
    }

    /**
     * Uses the string {@link DocumentReferenceResolver} to resolve a document full name into a DocumentReference.
     * @param documentReference the name of the document
     * @return the resolved reference
     */
    public DocumentReference resolveStringDocumentReference(String documentReference)
    {
        return stringDocumentReferenceResolver.resolve(documentReference);
    }

    /**
     * Create a new wiki.
     *
     * @param request the request describing the wiki
     * @return the job related to the request
     * @throws JobException if the job could not be started
     */
    public Job createWiki(WikiCreationRequest request) throws JobException
    {
        return jobExecutor.execute("wikicreationjob", request);
    }

    /**
     * Unregister some listeners triggering the evaluation of velocity code upon extension installation.
     * The evaluation of velocity code implies a servlet context for the Velocity engine, which we don't have.
     */
    public void nukeListeners()
    {
        componentManager.unregisterComponent(DocumentEventListener.class, "EventStreamStoreListener");
        componentManager.unregisterComponent(EventListener.class,
                DefaultWikiComponentManagerEventListener.EVENT_LISTENER_NAME);
    }

    /**
     * Remove a leftover property in the context, see XWIKI-17159.
     */
    public void cleanupContext()
    {
        execution.getContext().removeProperty("extension.xar.packageconfiguration");
    }
}

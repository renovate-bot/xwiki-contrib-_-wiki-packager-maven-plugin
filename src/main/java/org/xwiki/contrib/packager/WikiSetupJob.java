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

import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.plugin.MojoExecutionException;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.extension.internal.validator.AbstractExtensionValidator;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.script.ScriptExtensionRewriter;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.DefaultJobStatus;
import org.xwiki.platform.wiki.creationjob.WikiCreationRequest;
import org.xwiki.tool.extension.util.ExtensionMojoHelper;

/**
 * Setup job for creating a new wiki and installing default extensions.
 *
 * @version $Id$
 * @since 1.1
 */
@Component
@Named(WikiSetupJob.WIKI_SETUP_JOB_TYPE)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class WikiSetupJob extends AbstractJob<WikiSetupJobRequest, DefaultJobStatus<WikiSetupJobRequest>>
{
    /**
     * The type of the job.
     */
    public static final String WIKI_SETUP_JOB_TYPE = "wikiSetup";

    @Inject
    private WikiHelper wikiHelper;

    @Inject
    private ExtensionMojoHelper extensionHelper;

    @Override
    public String getType()
    {
        return WIKI_SETUP_JOB_TYPE;
    }

    @Override
    protected void runInternal() throws Exception
    {
        InstallRequest installRequest = new InstallRequest();

        if (getRequest().getWiki().getId().equals("xwiki")) {
            // Allow modifying root namespace
            installRequest.setRootModificationsAllowed(true);

            // Make sure jars are installed on root
            // TODO: use a less script oriented class
            ScriptExtensionRewriter rewriter = new ScriptExtensionRewriter();
            rewriter.installExtensionTypeOnRootNamespace("jar");
            rewriter.installExtensionTypeOnRootNamespace("webjar");
            installRequest.setRewriter(rewriter);
        } else {
            createSubWiki(getRequest().getWiki());
        }

        installRequest.setProperty(AbstractExtensionValidator.PROPERTY_USERREFERENCE,
            wikiHelper.resolveStringDocumentReference(getRequest().getWiki().getOwner()));
        installRequest.setVerbose(true);

        logger.info("Installing extensions on wiki [{}]", getRequest().getWiki().getId());
        this.extensionHelper.install(getRequest().getWiki().getExtensions(), installRequest,
            String.format("wiki:%s", getRequest().getWiki().getId()), null);
        wikiHelper.cleanupContext();
        logger.info("Installation done");
    }

    /**
     * Create a subwiki.
     *
     * @param wiki the wiki parameter
     * @throws MojoExecutionException if an error happens
     */
    private void createSubWiki(Wiki wiki) throws MojoExecutionException
    {
        // In a case of a wiki different from the main wiki, we'll  need to create this wiki first.
        WikiCreationRequest creationRequest = new WikiCreationRequest();
        creationRequest.setId(wiki.getId());
        creationRequest.setWikiId(wiki.getId());
        creationRequest.setPrettyName(wiki.getPrettyName());
        creationRequest.setOwnerId(wiki.getOwner());
        creationRequest.setMembershipType(wiki.getMembership());
        creationRequest.setUserScope(wiki.getUserScope());
        creationRequest.setTemplate(wiki.isTemplate());

        creationRequest.setMembers(Collections.EMPTY_LIST);
        creationRequest.setAlias(wiki.getId());
        creationRequest.setFailOnExist(false);

        try {
            logger.info("Creating wiki {}", wiki.getId());
            wikiHelper.getJobExecutor().execute("wikicreationjob", creationRequest).join();
            logger.info("Successfully created wiki [{}]", wiki.getId());
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }
}

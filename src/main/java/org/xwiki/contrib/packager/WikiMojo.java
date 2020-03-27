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
import java.util.List;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.xwiki.extension.internal.validator.AbstractExtensionValidator;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.script.ScriptExtensionRewriter;
import org.xwiki.platform.wiki.creationjob.WikiCreationRequest;
import org.xwiki.tool.utils.AbstractOldCoreMojo;

/**
 * Maven 2 plugin based on the DataMojo to generate an XWiki Database with multiple subwikis.
 * Note that this mojo works with a configuration slightly different from the one used
 * by the data mojo, especially for defining wikis.
 * *
 * @version $Id: 93d51d60aae264b72294b04fa2c360cdd34119cb $
 */
@Mojo(name = "wiki", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresProject = true)
public class WikiMojo extends AbstractOldCoreMojo
{
    @Parameter(property = "wikis")
    protected List<Wiki> wikis;

    protected WikiHelper wikiHelper;

    @Override
    protected void before() throws MojoExecutionException
    {
        super.before();

        this.wikiHelper =
                WikiHelper.create(this.extensionHelper.getComponentManager(), hibernateConfig);
    }

    @Override
    public void executeInternal() throws MojoExecutionException
    {
        wikiHelper.nukeListeners();

        for (Wiki wiki : wikis) {
            InstallRequest installRequest = new InstallRequest();

            if (wiki.getId().equals("xwiki")) {
                // Allow modifying root namespace
                installRequest.setRootModificationsAllowed(true);

                // Make sure jars are installed on root
                // TODO: use a less script oriented class
                ScriptExtensionRewriter rewriter = new ScriptExtensionRewriter();
                rewriter.installExtensionTypeOnRootNamespace("jar");
                rewriter.installExtensionTypeOnRootNamespace("webjar");
                installRequest.setRewriter(rewriter);
            } else {
                createWiki(wiki);
            }

            installRequest.setProperty(AbstractExtensionValidator.PROPERTY_USERREFERENCE,
                    wikiHelper.resolveStringDocumentReference(wiki.getOwner()));
            installRequest.setVerbose(true);

            getLog().info(String.format("Installing extensions on wiki [%s]", wiki.getId()));
            this.extensionHelper.install(wiki.getExtensions(), installRequest,
                    String.format("wiki:%s", wiki.getId()), null);
            wikiHelper.cleanupContext();
            getLog().info("Installation done");
        }
    }

    /**
     * Create a subwiki.
     * @param wiki the wiki parameter
     * @throws MojoExecutionException if an error happens
     */
    private void createWiki(Wiki wiki) throws MojoExecutionException
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
            getLog().info(String.format("Creating wiki [%s]", wiki.getId()));
            wikiHelper.createWiki(creationRequest).join();
            getLog().info(String.format("Successfully created wiki [%s]", wiki.getId()));
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }
}

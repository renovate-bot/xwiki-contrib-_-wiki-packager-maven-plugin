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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.event.status.JobStatus;
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
    private static final int WAIT_TIME = 5000;

    private static final int TOTAL_WAIT_TIME = 12 * WAIT_TIME;

    @Parameter(property = "wikis")
    protected List<Wiki> wikis;

    @Parameter(property = "parallel", defaultValue = "false")
    protected boolean parallel;

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
        List<Job> ongoingJobs = new ArrayList<>();

        try {
            startWikiSetupJobs(wikis, ongoingJobs);

            int totalWaitTime = 0;
            while (!ongoingJobs.isEmpty() && totalWaitTime <= TOTAL_WAIT_TIME) {
                getLog().info("Waiting for jobs to finish ...");

                List<Job> finishedJobs = new ArrayList<>();
                for (Job job : ongoingJobs) {
                    if (job.getStatus().getState().equals(JobStatus.State.FINISHED)) {
                        getLog().info(String.format("Job for wiki [%s] is now finished",
                            ((WikiSetupJobRequest) job.getRequest()).getWiki().getId()));
                        finishedJobs.add(job);
                    }
                }

                for (Job job : finishedJobs) {
                    ongoingJobs.remove(job);
                }

                Thread.sleep(WAIT_TIME);

                totalWaitTime += WAIT_TIME;
            }
        } catch (InterruptedException e) {
            getLog().error("Got interrupted while waiting for the completion of the wiki creation jobs", e);
        }

        if (ongoingJobs.size() > 0) {
            throw new MojoExecutionException("Failed to install every wiki.");
        } else {
            getLog().info("Successfully installed every wiki.");
        }
    }

    private void startWikiSetupJobs(List<Wiki> wikis, List<Job> ongoingJobs) throws InterruptedException
    {
        for (Wiki wiki : wikis) {
            try {
                if (wiki.getId().equals("xwiki") || !parallel) {
                    setupWikiAsync(wiki).join();
                } else {
                    ongoingJobs.add(setupWikiAsync(wiki));
                }
            } catch (JobException e) {
                getLog().error(String.format("Failed to set up wiki [%s]", wiki.getId()), e);
            }
        }
    }

    private Job setupWikiAsync(Wiki wiki) throws JobException
    {
        WikiSetupJobRequest jobRequest = new WikiSetupJobRequest();
        jobRequest.setWiki(wiki);

        return  wikiHelper.getJobExecutor().execute(WikiSetupJob.WIKI_SETUP_JOB_TYPE, jobRequest);
    }
}

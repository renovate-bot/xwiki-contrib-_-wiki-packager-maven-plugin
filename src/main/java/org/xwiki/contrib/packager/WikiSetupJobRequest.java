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

import org.xwiki.job.AbstractRequest;

/**
 * Simple job request for {@link WikiSetupJob}.
 *
 * @since 1.1
 * @version $Id$
 */
public class WikiSetupJobRequest extends AbstractRequest
{
    /**
     * The property id of the wiki to create.
     */
    public static final String WIKI_PROPERTY = "wiki";

    /**
     * Set the wiki to create.
     *
     * @param wiki the wiki description
     */
    public void setWiki(Wiki wiki) {
        this.setProperty(WIKI_PROPERTY, wiki);
    }

    /**
     * @return the wiki to create
     */
    public Wiki getWiki() {
        return this.getProperty(WIKI_PROPERTY);
    }
}

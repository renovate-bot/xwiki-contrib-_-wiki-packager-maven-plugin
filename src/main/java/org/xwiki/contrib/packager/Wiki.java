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

import org.xwiki.tool.extension.util.ExtensionArtifact;
import org.xwiki.wiki.user.MembershipType;
import org.xwiki.wiki.user.UserScope;

/**
 * Custom object for describing the properties of a wiki.
 *
 * @version $Id$
 */
public class Wiki
{
    private String id;

    private String prettyName;

    private String owner;

    private MembershipType membership;

    private UserScope userScope;

    private boolean template;

    private List<ExtensionArtifact> extensions;

    /**
     * Builds a {@link Wiki} with some default parameters.
     */
    public Wiki()
    {
        // Set default values
        this.id = "xwiki";
        this.prettyName = "XWiki";
        this.owner = "xwiki:XWiki.superadmin";
        this.membership = MembershipType.OPEN;
        this.userScope = UserScope.GLOBAL_ONLY;
        this.template = false;
        this.extensions = Collections.emptyList();
    }

    /**
     * @return the wiki id
     */
    public String getId()
    {
        return id;
    }

    /**
     * @param id the wiki id
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * @return the wiki pretty name
     */
    public String getPrettyName()
    {
        return prettyName;
    }

    /**
     * @param prettyName the wiki pretty name
     */
    public void setPrettyName(String prettyName)
    {
        this.prettyName = prettyName;
    }

    /**
     * @return the wiki owner
     */
    public String getOwner()
    {
        return owner;
    }

    /**
     * @param owner the wiki owner
     */
    public void setOwner(String owner)
    {
        this.owner = owner;
    }

    /**
     * @return the wiki membership type
     */
    public MembershipType getMembership()
    {
        return membership;
    }

    /**
     * @param membership the wiki membership type
     */
    public void setMembership(MembershipType membership)
    {
        this.membership = membership;
    }

    /**
     * @return the wiki user scope
     */
    public UserScope getUserScope()
    {
        return userScope;
    }

    /**
     * @param userScope the wiki user scope
     */
    public void setUserScope(UserScope userScope)
    {
        this.userScope = userScope;
    }

    /**
     * @return true if the wiki should be a template
     */
    public boolean isTemplate()
    {
        return template;
    }

    /**
     * @param template true if the wiki should be a template
     */
    public void setTemplate(boolean template)
    {
        this.template = template;
    }

    /**
     * @return the wiki extensions
     */
    public List<ExtensionArtifact> getExtensions()
    {
        return extensions;
    }

    /**
     * @param extensions the wiki extensions
     */
    public void setExtensions(List<ExtensionArtifact> extensions)
    {
        this.extensions = extensions;
    }
}

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
package org.xwiki.extension.script.internal;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.Extension;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.handler.ExtensionValidator;
import org.xwiki.job.Request;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Default right needed to install/uninstall an extension in XWiki.
 * 
 * @version $Id$
 * @since 4.2M2
 */
// The rationale for being in this module is that checking right is useless if you don't also provide public script
// service but if there is other things to put in a new xwiki-platform-extension-xwiki we might want to move it.
@Component
@Singleton
public class XWikiExtensionValidator implements ExtensionValidator
{
    /**
     * Property name used to indicate the reference of the user applying the action.
     */
    private static final String PROPERTY_USERREFERENCE = "user.reference";

    /**
     * Property name used to indicate the reference of the script author calling the action.
     */
    private static final String PROPERTY_CALLERREFERENCE = "caller.reference";

    /**
     * Property name used to indicate if the right of the user and caller should be checked in the action plan.
     */
    private static final String PROPERTY_CHECKRIGHTS = "checkrights";

    /**
     * The full name (space.page) of the XWikiPreference page.
     */
    private static final String XWIKIPREFERENCES_FULLNAME = "XWiki.XWikiPreferences";

    /**
     * The identifier of the programming right.
     */
    private static final String RIGHTS_PROGRAMMING = "programming";

    /**
     * Needed for checking programming rights.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Used to access the current {@link XWikiContext}.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Used to serialize users references.
     */
    @Inject
    private EntityReferenceSerializer<String> serializer;

    /**
     * @param right the right to check
     * @param document the document to check the right on
     * @param request the request of the action
     * @return true of the action is allowed, false otherwise
     * @throws XWikiException failed to check rights
     */
    private boolean hasAccessLevel(String right, String document, Request request) throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        boolean hasAccess = true;

        String caller = getRequestUserString(PROPERTY_CALLERREFERENCE, request);
        if (caller != null) {
            hasAccess =
                xcontext.getWiki().getRightService().hasAccessLevel(right, caller, XWIKIPREFERENCES_FULLNAME, xcontext);
        }

        if (hasAccess) {
            String user = getRequestUserString(PROPERTY_USERREFERENCE, request);
            if (user != null) {
                hasAccess =
                    xcontext.getWiki().getRightService()
                        .hasAccessLevel(right, user, XWIKIPREFERENCES_FULLNAME, xcontext);
            }
        }

        return hasAccess;
    }

    /**
     * @param property the property containing a user reference
     * @param request the request containing the property
     * @return the user reference
     */
    private DocumentReference getRequestUserReference(String property, Request request)
    {
        Object obj = request.getProperty(property);

        if (obj instanceof DocumentReference) {
            return (DocumentReference) obj;
        }

        return null;
    }

    /**
     * @param property the property containing a user reference
     * @param request the request containing the property
     * @return the user reference as String
     */
    private String getRequestUserString(String property, Request request)
    {
        String str = null;

        if (request.containsProperty(property)) {
            DocumentReference reference = getRequestUserReference(property, request);

            if (reference != null) {
                str = this.serializer.serialize(reference);
            } else {
                str = XWikiRightService.GUEST_USER_FULLNAME;
            }
        }

        return str;
    }

    @Override
    public void checkInstall(Extension extension, String namespace, Request request) throws InstallException
    {
        if (request.getProperty(PROPERTY_CHECKRIGHTS) == Boolean.TRUE) {
            try {
                if (!hasAccessLevel(RIGHTS_PROGRAMMING, namespace + XWIKIPREFERENCES_FULLNAME, request)) {
                    if (namespace == null) {
                        throw new InstallException(String.format(
                            "Programming right is required to install extension [%s]", extension.getId()));
                    } else {
                        throw new InstallException(String.format(
                            "Programming right is required to install extension [%s] on namespace [%s]",
                            extension.getId(), namespace));
                    }
                }
            } catch (XWikiException e) {
                throw new InstallException("Failed to check rights to install extension", e);
            }
        }
    }

    @Override
    public void checkUninstall(InstalledExtension extension, String namespace, Request request)
        throws UninstallException
    {
        if (request.getProperty(PROPERTY_CHECKRIGHTS) == Boolean.TRUE) {
            try {
                if (!hasAccessLevel(RIGHTS_PROGRAMMING, namespace + XWIKIPREFERENCES_FULLNAME, request)) {
                    if (namespace == null) {
                        throw new UninstallException(String.format(
                            "Programming right is required to uninstall extension [%s]", extension.getId()));
                    } else {
                        throw new UninstallException(String.format(
                            "Programming right is required to uninstall extension [%s] from namespace [%s]",
                            extension.getId(), namespace));
                    }
                }
            } catch (XWikiException e) {
                throw new UninstallException("Failed to check rights to uninstall extension", e);
            }
        }
    }
}

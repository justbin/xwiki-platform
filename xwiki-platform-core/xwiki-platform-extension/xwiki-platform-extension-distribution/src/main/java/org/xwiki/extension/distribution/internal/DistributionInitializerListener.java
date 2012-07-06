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
package org.xwiki.extension.distribution.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.distribution.internal.job.DistributionJobStatus;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.job.event.status.JobStatus.State;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.Event;

@Component
@Named("DistributionInitializerListener")
public class DistributionInitializerListener implements EventListener
{
    private static List<Event> EVENTS = Arrays.<Event> asList(new ApplicationStartedEvent());

    /**
     * The repository with core modules provided by the platform.
     */
    @Inject
    private InstalledExtensionRepository installedExtensionRepository;

    @Inject
    private DistributionManager distributionManager;

    @Inject
    private Logger logger;

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public String getName()
    {
        return "DistributionInitializerListener";
    }

    @Override
    public void onEvent(Event arg0, Object arg1, Object arg2)
    {
        CoreExtension distributionExtension = this.distributionManager.getDistributionExtension();

        DistributionJobStatus distributionStatus = this.distributionManager.getPreviousJobStatus();

        // Is install already done (allow to cancel stuff for example)
        if (distributionStatus != null
            && distributionStatus.getDistributionExtension().equals(distributionExtension.getId())
            && distributionStatus.getState() == State.FINISHED) {
            this.logger.info("Up to date");
        }

        
        ExtensionId uiExtensionId = this.distributionManager.getUIExtensionId();

        if (uiExtensionId != null) {
            // Lets check if the UI is already installed
            InstalledExtension installedExtension =
                this.installedExtensionRepository.getInstalledExtension(uiExtensionId.getId(), "wiki:xwiki");
            if (installedExtension == null) {
                // The UI is not installed on main wiki
                this.logger.info("New install: " + uiExtensionId);
            } else {
                int diff =
                    distributionExtension.getId().getVersion().compareTo(installedExtension.getId().getVersion());
                if (diff > 0) {
                    this.logger.info("Upgrade: " + uiExtensionId);
                } else if (diff < 0) {
                    this.logger.info("Downgrade: " + uiExtensionId);
                } else {
                    this.logger.info("Up to date");
                }
            }
        }
    }
}
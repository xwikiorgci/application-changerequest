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
package org.xwiki.contrib.changerequest.internal.descriptors;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.changerequest.notifications.events.ChangeRequestCreatedRecordableEvent;
import org.xwiki.eventstream.RecordableEventDescriptor;

/**
 * Descriptor related to {@link ChangeRequestCreatedRecordableEvent}.
 *
 * @version $Id$
 * @since 0.1
 */
@Component
@Singleton
@Named("changerequest.create")
public class ChangeRequestCreatedEventDescriptor implements RecordableEventDescriptor
{
    @Override
    public String getEventType()
    {
        return ChangeRequestCreatedRecordableEvent.class.getCanonicalName();
    }

    @Override
    public String getApplicationName()
    {
        return "changerequest.application.name";
    }

    @Override
    public String getDescription()
    {
        return "changerequest.event.created.description";
    }

    @Override
    public String getApplicationIcon()
    {
        return "branch";
    }
}

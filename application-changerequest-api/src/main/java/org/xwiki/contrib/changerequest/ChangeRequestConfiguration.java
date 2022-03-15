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
package org.xwiki.contrib.changerequest;

import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.stability.Unstable;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.UserReference;

/**
 * Available configuration for change request application.
 *
 * @version $Id$
 * @since 0.1
 */
@Role
@Unstable
public interface ChangeRequestConfiguration
{
    /**
     * @return the hint of the {@link MergeApprovalStrategy} to use.
     */
    String getMergeApprovalStrategy();

    /**
     * @return the location where to store the change requests.
     */
    SpaceReference getChangeRequestSpaceLocation();

    /**
     * Define the duration after a stale change request notification as been sent
     * (see {@link #getStaleChangeRequestDurationForNotifying()}) to close the change request automatically.
     * Note that {@code 0} means the change request won't be closed automatically.
     * Also if {@link #getStaleChangeRequestDurationForNotifying()} returns {@code 0}, then the duration is considered
     * since the creation or the latest update of the change request (see {@link #useCreationDateForStaleDurations()}).
     *
     * @return a duration in days for automatically closing a change request after a stale notification has been sent.
     * @since 0.10
     */
    default long getStaleChangeRequestDurationForClosing()
    {
        return 0;
    }

    /**
     * Define the duration after a change request has been created or updated
     * (see {@link #useCreationDateForStaleDurations()}) for sending a stale change request notifications: this
     * notification can inform users that the change request is about to be closed automatically.
     * Note that {@code 0} means no notification will be sent.
     *
     * @return a duration in days for triggering a notifications after a change request has been created or updated.
     * @since 0.10
     */
    default long getStaleChangeRequestDurationForNotifying()
    {
        return 0;
    }

    /**
     * Define if the durations given by {@link #getStaleChangeRequestDurationForNotifying()} and
     * {@link #getStaleChangeRequestDurationForClosing()} should concern the update date or the creation date of the
     * change request.
     *
     * @return {@code true} to consider the creation date of the change request, {@code false} to consider the
     *          update date.
     * @since 0.10
     */
    default boolean useCreationDateForStaleDurations()
    {
        return false;
    }

    /**
     * Defines the user to use in context when running the scheduler.
     *
     * @return the reference of the user to be put in context for running the scheduler, or null if not set.
     * @since 0.10
     */
    default UserReference getSchedulerContextUser()
    {
        return null;
    }

    /**
     * Defines the duration unit to be used for all durations given in this interface.
     * This API is mainly provided for testing purpose.
     *
     * @return a temporal unit to be used for all durations provided in this interface.
     */
    default TemporalUnit getDurationUnit()
    {
        return ChronoUnit.DAYS;
    }

    /**
     * Defines the user to use for merging the change request.
     * When no value is defined, it returns the guest user, in which case the current user is actually used to perform
     * merge if the user has edit rights.
     *
     * @return the reference of the user to use for merging a change request.
     * @since 0.10
     */
    default UserReference getMergeUser()
    {
        return GuestUserReference.INSTANCE;
    }
}

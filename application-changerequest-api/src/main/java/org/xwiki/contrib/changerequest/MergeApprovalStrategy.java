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

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Define the strategies to use for merging a change request.
 * A strategy can be, for example, to only allow merging whenever a number of approvals are been made, or when there's
 * at least one approval.
 *
 * @version $Id$
 * @since 0.1
 */
@Unstable
@Role
public interface MergeApprovalStrategy
{
    /**
     * Check if the given change request can be merged, based on the current strategy.
     *
     * @param changeRequest the change request to check for merging.
     * @return {@code true} if the change request can be merged, based on the current strategy.
     */
    boolean canBeMerged(ChangeRequest changeRequest);
}

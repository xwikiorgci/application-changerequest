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

import java.util.Optional;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;
import org.xwiki.store.merge.MergeDocumentResult;
import org.xwiki.user.UserReference;

/**
 * Component responsible to perform business operations on change requests.
 *
 * @version $Id$
 * @since 0.1
 */
@Role
@Unstable
public interface ChangeRequestManager
{
    /**
     * Check if the given file change expose conflicts with the current version of the documents.
     *
     * @param fileChange the change to be checked for conflicts.
     * @return {@code true} if it contains conflicts, {@code false} otherwise.
     * @throws ChangeRequestException in case of problem for detecting conflicts.
     */
    boolean hasConflicts(FileChange fileChange) throws ChangeRequestException;

    /**
     * Check if the given user is authorized to merge the given change request.
     *
     * @param userReference the user for which to check the authorizations
     * @param changeRequest the change request to check
     * @return {@code true} if the user has the appropriate rights to perform the merge.
     */
    boolean isAuthorizedToMerge(UserReference userReference, ChangeRequest changeRequest);

    /**
     * Check if all conditions are met so that a change request can be merged.
     * This method checks in particular if there's no conflict in the change request, if it's status allows is to be
     * merged, and if the approval strategy is met.
     *
     * @param changeRequest the change request to check for merging.
     * @return {@code true} if the change request can be merged.
     * @throws ChangeRequestException in case of problems during one of the check.
     */
    boolean canBeMerged(ChangeRequest changeRequest) throws ChangeRequestException;

    /**
     * Merge a given modified document in the given change request, without saving the result.
     *
     * @param modifiedDocument a document with changes not yet saved.
     * @param previousVersion the version of the document where the modifications have been started.
     * @param changeRequest an existing change request.
     * @return an empty optional if the change request did not contain any changes related to the given document, else
     *          returns an optional containing the result of the merge: this one can be checked for conflicts.
     * @throws ChangeRequestException in case of problem for detecting conflicts.
     * @since 0.3
     */
    default Optional<MergeDocumentResult> mergeDocumentChanges(DocumentModelBridge modifiedDocument,
        String previousVersion, ChangeRequest changeRequest)
        throws ChangeRequestException
    {
        return Optional.empty();
    }
}

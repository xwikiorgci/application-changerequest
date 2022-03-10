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
package org.xwiki.contrib.changerequest.script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.changerequest.ApproversManager;
import org.xwiki.contrib.changerequest.ChangeRequest;
import org.xwiki.contrib.changerequest.ChangeRequestException;
import org.xwiki.contrib.changerequest.ChangeRequestManager;
import org.xwiki.contrib.changerequest.ChangeRequestMergeDocumentResult;
import org.xwiki.contrib.changerequest.ChangeRequestReference;
import org.xwiki.contrib.changerequest.ChangeRequestStatus;
import org.xwiki.contrib.changerequest.ConflictResolutionChoice;
import org.xwiki.contrib.changerequest.FileChange;
import org.xwiki.contrib.changerequest.FileChangeCompatibilityChecker;
import org.xwiki.contrib.changerequest.MergeApprovalStrategy;
import org.xwiki.contrib.changerequest.storage.ChangeRequestStorageManager;
import org.xwiki.diff.Conflict;
import org.xwiki.diff.ConflictDecision;
import org.xwiki.diff.internal.DefaultConflictDecision;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.script.service.ScriptService;
import org.xwiki.script.service.ScriptServiceManager;
import org.xwiki.stability.Unstable;
import org.xwiki.store.merge.MergeDocumentResult;
import org.xwiki.url.ExtendedURL;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

/**
 * Script service for change request.
 *
 * @version $Id$
 * @since 0.1
 */
@Unstable
@Component
@Named("changerequest")
@Singleton
public class ChangeRequestScriptService implements ScriptService
{
    @Inject
    private ChangeRequestManager changeRequestManager;

    @Inject
    private ChangeRequestStorageManager changeRequestStorageManager;

    @Inject
    private UserReferenceResolver<CurrentUserReference> currentUserReferenceResolver;

    @Inject
    private ResourceReferenceSerializer<ChangeRequestReference, ExtendedURL> urlResourceReferenceSerializer;

    @Inject
    private DocumentReferenceResolver<ChangeRequest> documentReferenceResolver;

    @Inject
    private ApproversManager<ChangeRequest> changeRequestApproversManager;

    @Inject
    private ScriptServiceManager scriptServiceManager;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    /**
     * @param <S> the type of the {@link ScriptService}
     * @param serviceName the name of the sub {@link ScriptService}
     * @return the {@link ScriptService} or null of none could be found
     */
    @SuppressWarnings("unchecked")
    public <S extends ScriptService> S get(String serviceName)
    {
        return (S) this.scriptServiceManager.get("changerequest." + serviceName);
    }

    /**
     * Retrieve the change request identified with the given id.
     *
     * @param changeRequestId the identifier of a change request.
     * @return an optional containing the change request instance if it can be found, else an empty optional.
     * @throws ChangeRequestException in case of problem when retrieving the change request.
     */
    public Optional<ChangeRequest> getChangeRequest(String changeRequestId) throws ChangeRequestException
    {
        return this.changeRequestStorageManager.load(changeRequestId);
    }

    /**
     * Check if the current user is authorized to merge the given changed request.
     *
     * @param changeRequest the change request to be checked for merging authorization.
     * @return {@code true} if the current user has proper rights to merge the given change request.
     * @throws ChangeRequestException in case of problem when checking the role of the user.
     * @since 0.3
     */
    @Unstable
    public boolean isAuthorizedToMerge(ChangeRequest changeRequest) throws ChangeRequestException
    {
        UserReference currentUser = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        return this.changeRequestManager.isAuthorizedToMerge(currentUser, changeRequest);
    }

    /**
     * Check if the given change request can be merged.
     * This method checks if the approval strategy is reached and if the change request has conflicts.
     *
     * @param changeRequest the change request to check.
     * @return {@code true} if the given change request can be merged (i.e. the approval strategy
     *          allows it and the change request does not have conflicts).
     * @throws ChangeRequestException in case of problem when loading information.
     */
    public boolean canBeMerged(ChangeRequest changeRequest) throws ChangeRequestException
    {
        return this.changeRequestManager.canBeMerged(changeRequest);
    }

    /**
     * Retrieve the modified document containing in the given change request and identified by the given reference.
     *
     * @param changeRequest the change request where to find the modified document
     * @param documentReference the reference of the modified document to get
     * @return an optional containing the instance of modified document or an empty optional if it cannot be found.
     */
    public Optional<DocumentModelBridge> getModifiedDocument(ChangeRequest changeRequest,
        DocumentReference documentReference)
    {
        Optional<FileChange> latestFileChange = changeRequest.getLatestFileChangeFor(documentReference);
        return latestFileChange.map(FileChange::getModifiedDocument);
    }

    /**
     * Retrieve all change requests that contain a change for the given document.
     *
     * @param documentReference the reference to look for in the change requests.
     * @return the list of all change requests containing a change for the given document.
     * @throws ChangeRequestException in case of problem when loading change request.
     * @since 0.3
     */
    public List<ChangeRequest> getChangeRequestWithChangesFor(DocumentReference documentReference)
        throws ChangeRequestException
    {
        return this.changeRequestStorageManager.findChangeRequestTargeting(documentReference);
    }

    /**
     * Retrieve change requests different from the given change request, that are not closed or merged and which
     * contains a change for one of the document reference modified by the given change request.
     *
     * @param changeRequest the change request from which to take the modified documents.
     * @return a map whose keys are the given document references and values the list of found change requests. If no
     *          change request is found for a given reference, the entry is not added.
     * @throws ChangeRequestException in case of problem for loading other change requests.
     * @since 0.7
     */
    public Map<DocumentReference, List<ChangeRequest>> getOpenChangeRequestsTargetingSame(ChangeRequest changeRequest)
        throws ChangeRequestException
    {
        Map<DocumentReference, List<ChangeRequest>> result = new HashMap<>();

        for (DocumentReference modifiedDocument : changeRequest.getModifiedDocuments()) {
            List<ChangeRequest> changeRequests = getChangeRequestWithChangesFor(modifiedDocument).stream()
                .filter(foundCR ->
                    !foundCR.getId().equals(changeRequest.getId())
                    && foundCR.getStatus() != ChangeRequestStatus.CLOSED
                    && foundCR.getStatus() != ChangeRequestStatus.MERGED
            ).collect(Collectors.toList());
            if (!changeRequests.isEmpty()) {
                result.put(modifiedDocument, changeRequests);
            }
        }

        return result;
    }

    /**
     * Find all change request documents whose title is matching the given title.
     *
     * @param title a partial title for finding the change requests.
     * @return a list of document references corresponding to change request pages.
     * @throws ChangeRequestException in case of problem for loading other change request.
     * @since 0.3
     */
    public List<DocumentReference> findChangeRequestMatchingTitle(String title) throws ChangeRequestException
    {
        return this.changeRequestStorageManager.getChangeRequestMatchingName(title);
    }

    /**
     * Filter the list of change request document references to only keep the ones that are compatible with the given
     * document reference.
     * @param changeRequestReferences the document references of change requests.
     * @param newDocumentChange the new document that should be added to a change request.
     * @return the list containing same entries than {@code changeRequestReferences} without the ones that are not
     *         compatible given the {@link FileChangeCompatibilityChecker} implementations.
     * @throws ComponentLookupException in case of problem to find the {@link FileChangeCompatibilityChecker}.
     * @throws ChangeRequestException in case of problem to load the change request.
     */
    public List<DocumentReference> filterCompatibleChangeRequest(List<DocumentReference> changeRequestReferences,
        DocumentReference newDocumentChange) throws ComponentLookupException, ChangeRequestException
    {
        List<FileChangeCompatibilityChecker> checkerList =
            this.componentManager.getInstanceList(FileChangeCompatibilityChecker.class);

        List<DocumentReference> result = new ArrayList<>();
        for (DocumentReference changeRequestReference : changeRequestReferences) {
            Optional<ChangeRequest> changeRequestOptional =
                this.changeRequestStorageManager.load(changeRequestReference.getLastSpaceReference().getName());
            boolean shouldBeAdded = false;
            if (changeRequestOptional.isPresent()) {
                shouldBeAdded = true;
                for (FileChangeCompatibilityChecker compatibilityChecker : checkerList) {
                    shouldBeAdded = shouldBeAdded && compatibilityChecker
                        .canChangeOnDocumentBeAdded(changeRequestOptional.get(), newDocumentChange);
                }
            }
            if (shouldBeAdded) {
                result.add(changeRequestReference);
            }
        }
        return result;
    }

    /**
     * Retrieve the URL for the given change request action and id.
     * @param action the change request action as defined by
     *          {@link org.xwiki.contrib.changerequest.ChangeRequestReference.ChangeRequestAction}.
     * @param changeRequestId the change request id.
     * @return an URL as a String or an empty string in case of error.
     * @throws SerializeResourceReferenceException in case of problem for serializing the URL
     * @throws UnsupportedResourceReferenceException if the action is not recognized.
     * @since 0.3
     */
    public String getChangeRequestURL(String action, String changeRequestId)
        throws SerializeResourceReferenceException, UnsupportedResourceReferenceException
    {
        ChangeRequestReference.ChangeRequestAction requestAction =
            ChangeRequestReference.ChangeRequestAction.valueOf(action.toUpperCase(Locale.ROOT));
        ChangeRequestReference reference = new ChangeRequestReference(requestAction, changeRequestId);
        ExtendedURL extendedURL = this.urlResourceReferenceSerializer.serialize(reference);
        return extendedURL.serialize();
    }

    /**
     * Compute the reference of the actual document which stores the change request.
     *
     * @param changeRequest the change request for which to get the store document reference.
     * @return a document reference where the change request can be seen.
     * @since 0.4
     */
    public DocumentReference getChangeRequestDocumentReference(ChangeRequest changeRequest)
    {
        return this.documentReferenceResolver.resolve(changeRequest);
    }

    /**
     * Check if the current user can edit the change request.
     *
     * @param changeRequest the change request for which to check the authors.
     * @return {@code true} if the current user is one of the author of the given change request and the change request
     *          is not merged or closed yet.
     * @since 0.7
     */
    public boolean isAuthorizedToEdit(ChangeRequest changeRequest)
    {
        UserReference currentUser = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        return this.changeRequestManager.isAuthorizedToEdit(currentUser, changeRequest);
    }

    /**
     * Check if the current user can comment a change request.
     * @param changeRequest the change request for which to check if it can be commented.
     * @return {@code true} if the current user is authorized to comment the change request.
     * @throws ChangeRequestException in case of problem to check the authorization.
     * @see ChangeRequestManager#isAuthorizedToComment(UserReference, ChangeRequest)
     * @since 0.11
     */
    public boolean isAuthorizedToComment(ChangeRequest changeRequest) throws ChangeRequestException
    {
        UserReference currentUser = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        return this.changeRequestManager.isAuthorizedToComment(currentUser, changeRequest);
    }

    /**
     * Check if the current user can edit the change request.
     *
     * @param changeRequest the change request for which to check the authors.
     * @return {@code true} if the current user is one of the author of the given change request and the change request
     *          is not merged yet.
     * @since 0.9
     */
    public boolean isAuthorizedToOpen(ChangeRequest changeRequest)
    {
        UserReference currentUser = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        return this.changeRequestManager.isAuthorizedToOpen(currentUser, changeRequest);
    }

    /**
     * Mark the given change request as ready for review.
     *
     * @param changeRequest the change request for which to change the status.
     * @throws ChangeRequestException in case of problem for saving the change request.
     * @since 0.4
     */
    public void setReadyForReview(ChangeRequest changeRequest) throws ChangeRequestException
    {
        this.changeRequestManager.updateStatus(changeRequest, ChangeRequestStatus.READY_FOR_REVIEW);
    }

    /**
     * Mark the given change request as draft.
     *
     * @param changeRequest the change request for which to change the status.
     * @throws ChangeRequestException in case of problem for saving the change request.
     * @since 0.4
     */
    public void setDraft(ChangeRequest changeRequest) throws ChangeRequestException
    {
        this.changeRequestManager.updateStatus(changeRequest, ChangeRequestStatus.DRAFT);
    }

    /**
     * Mark the given change request as closed.
     *
     * @param changeRequest the change request for which to change the status.
     * @throws ChangeRequestException in case of problem when saving the status.
     *
     * @since 0.6
     */
    public void setClose(ChangeRequest changeRequest) throws ChangeRequestException
    {
        this.changeRequestManager.updateStatus(changeRequest, ChangeRequestStatus.CLOSED);
    }

    /**
     * Perform a merge without saving between the changes of the change request related to the given reference, and the
     * published document with same reference, and returns the merge result.
     *
     * @param changeRequest the change request for which to find changes.
     * @param documentReference the document reference for which to perform a merge.
     * @return a {@link Optional#empty()} if no change for the given reference can be found or if an error occurs
     *         during the merge, else an optional containing the {@link MergeDocumentResult}.
     * @throws ChangeRequestException in case of problem for loading information.
     * @since 0.4
     */
    public Optional<ChangeRequestMergeDocumentResult> getMergeDocumentResult(ChangeRequest changeRequest,
        DocumentReference documentReference) throws ChangeRequestException
    {
        Optional<ChangeRequestMergeDocumentResult> result = Optional.empty();
        Optional<FileChange> optionalFileChange = changeRequest.getLatestFileChangeFor(documentReference);
        if (optionalFileChange.isPresent()) {
            ChangeRequestMergeDocumentResult changeRequestMergeDocumentResult =
                this.changeRequestManager.getMergeDocumentResult(optionalFileChange.get());
            result = Optional.of(changeRequestMergeDocumentResult);
        }
        return result;
    }

    /**
     * Allow to create a {@link ConflictDecision} based on the given parameters.
     *
     * @param mergeDocumentResult the merge result for which to create a conflict decision.
     * @param conflictReference the reference of the conflict for which to create the decision.
     * @param decisionType the decision taken for fixing the conflict.
     * @param customResolution a custom resolution input. Note that if this parameter is given, then the decisionType
     *                         will be set to custom.
     * @return an {@link Optional#empty()} if no conflict matches the given reference in the merge result, else returns
     *          a {@link ConflictDecision} with the appropriate information to be used in
     *          {@link #fixConflicts(ChangeRequest, DocumentReference, ConflictResolutionChoice, List)}.
     * @since 0.4
     */
    public Optional<ConflictDecision<?>> createConflictDecision(MergeDocumentResult mergeDocumentResult,
        String conflictReference, ConflictDecision.DecisionType decisionType, List<Object> customResolution)
    {
        Optional<ConflictDecision<?>> result = Optional.empty();
        Conflict<?> concernedConflict = null;
        for (Conflict<?> conflict : mergeDocumentResult.getConflicts()) {
            if (StringUtils.equals(conflictReference, conflict.getReference())) {
                concernedConflict = conflict;
                break;
            }
        }
        if (concernedConflict != null) {
            ConflictDecision<Object> decision = new DefaultConflictDecision<>(concernedConflict);
            decision.setType(decisionType);
            if (customResolution != null && !customResolution.isEmpty()) {
                decision.setCustom(customResolution);
            }
            result = Optional.of(decision);
        }

        return result;
    }

    /**
     * Fix conflicts related to the given {@link MergeDocumentResult} by applying the given decision.
     *
     * @param changeRequest the change request for which to fix the conflicts.
     * @param documentReference the document reference for which to perform a merge.
     * @param resolutionChoice the global choice to make.
     * @param customDecisions the specific decisions in case the resolution choice was
     *          {@link ConflictResolutionChoice#CUSTOM}.
     * @return {@code true} if the conflicts were properly fixed, {@code false} if any problem occurs preventing to fix
     *          the conflict.
     * @throws ChangeRequestException in case of problem for applying decisions.
     * @since 0.4
     */
    public boolean fixConflicts(ChangeRequest changeRequest, DocumentReference documentReference,
        ConflictResolutionChoice resolutionChoice, List<ConflictDecision<?>> customDecisions)
        throws ChangeRequestException
    {
        boolean result = false;
        if (this.canFixConflict(changeRequest, documentReference)) {
            Optional<FileChange> optionalFileChange = changeRequest.getLatestFileChangeFor(documentReference);
            if (optionalFileChange.isPresent()) {
                result = this.changeRequestManager
                    .mergeWithConflictDecision(optionalFileChange.get(), resolutionChoice, customDecisions);
            }
        }

        return result;
    }

    /**
     * Retrieve the {@link MergeApprovalStrategy} to be used.
     *
     * @return an {@link Optional#empty()} in case of problem to get the strategy, else an optional containing the
     * {@link MergeApprovalStrategy}.
     * @throws ChangeRequestException in case of problem for loading the strategy.
     * @since 0.4
     */
    public MergeApprovalStrategy getMergeApprovalStrategy() throws ChangeRequestException
    {
        return this.changeRequestManager.getMergeApprovalStrategy();
    }

    /**
     * Check if the current user can fix a conflict related to the given document reference in the given change request.
     * @param changeRequest the change request concerned by a conflict.
     * @param documentReference the reference of the document concerned by the conflict.
     * @return {@code true} if the current user is authorized to fix the conflict.
     * @since 0.4
     */
    public boolean canFixConflict(ChangeRequest changeRequest, DocumentReference documentReference)
    {
        UserReference currentUserReference = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        Optional<FileChange> optionalFileChange = changeRequest.getLatestFileChangeFor(documentReference);
        if (optionalFileChange.isPresent()) {
            return this.changeRequestManager.isAuthorizedToFixConflict(currentUserReference, optionalFileChange.get());
        }
        return false;
    }

    /**
     * Check if the given document can be requested for deletion.
     *
     * @param documentReference the document for which to check if it can be requested for deletion.
     * @return {@code true} if the document can be requested for deletion.
     * @throws ChangeRequestException in case of problem for loading rights.
     * @since 0.5
     */
    public boolean canDeletionBeRequested(DocumentReference documentReference) throws ChangeRequestException
    {
        return this.changeRequestManager.canDeletionBeRequested(documentReference);
    }

    /**
     * Retrieve all explicit approvers for the given change request.
     *
     * @param changeRequest the request for which to get approvers.
     * @return the list of approvers.
     * @throws ChangeRequestException in case of problem for loading information.
     * @since 0.5
     */
    public Set<UserReference> getApprovers(ChangeRequest changeRequest) throws ChangeRequestException
    {
        return this.changeRequestApproversManager.getAllApprovers(changeRequest, true);
    }

    /**
     * Retrieve a file change based on its id.
     * @param changeRequest the change request from where to retrieve the file change.
     * @param fileChangeId the id of the file change to find.
     * @return an {@link Optional#empty()} if the file change cannot be retrieved, else the retrieved file change.
     * @since 0.6
     */
    public Optional<FileChange> getFileChange(ChangeRequest changeRequest, String fileChangeId)
    {
        return changeRequest.getAllFileChanges().stream()
            .filter(fileChange -> StringUtils.equals(fileChangeId, fileChange.getId()))
            .findFirst();
    }

    /**
     * Check if the given file change is outdated.
     *
     * @param fileChange the file change to check if it is outdated.
     * @return {@code true} if the published version of the filechange does not match the current version of the
     *          document.
     * @throws ChangeRequestException in case of problems for loading the information.
     * @since 0.9
     */
    public boolean isFileChangeOutdated(FileChange fileChange) throws ChangeRequestException
    {
        return this.changeRequestManager.isFileChangeOutdated(fileChange);
    }

    /**
     * Check if the given template is supported by change request: if not supported, and it won't allow to
     * "save as change request" when using it.
     *
     * @param templateProviderReference the reference of the template.
     * @return {@code true} if it's supported.
     * @throws ChangeRequestException in case of problem when loading information.
     * @since 0.9
     */
    public boolean isTemplateSupported(DocumentReference templateProviderReference)
        throws ChangeRequestException
    {
        return this.changeRequestManager.isTemplateSupported(templateProviderReference);
    }
}

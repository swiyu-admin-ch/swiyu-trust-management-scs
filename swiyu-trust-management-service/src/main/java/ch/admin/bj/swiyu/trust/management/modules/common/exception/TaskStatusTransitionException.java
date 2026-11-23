package ch.admin.bj.swiyu.trust.management.modules.common.exception;

/**
 * A task was asked to move to a state its current state does not allow, for example approving it a second time or
 * rejecting it after it was approved. Extends {@link IllegalStateException} so callers that only care that the
 * transition was refused keep their existing handling.
 */
public class TaskStatusTransitionException extends IllegalStateException {

    public TaskStatusTransitionException(String message) {
        super(message);
    }
}

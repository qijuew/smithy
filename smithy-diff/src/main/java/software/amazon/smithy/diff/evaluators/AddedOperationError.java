/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.diff.ChangedShape;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Emits a warning when an error is added to an operation.
 */
public final class AddedOperationError extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        return differences.changedShapes(OperationShape.class)
                .flatMap(change -> createErrorViolations(change, differences.getNewModel()).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> createErrorViolations(ChangedShape<OperationShape> change, Model newModel) {
        if (change.getOldShape().getErrors().equals(change.getNewShape().getErrors())) {
            return Collections.emptyList();
        }

        List<ValidationEvent> events = new ArrayList<>();
        for (ShapeId error : change.getNewShape().getErrors()) {
            SourceLocation errorSource = newModel.expectShape(error).getSourceLocation();
            if (!change.getOldShape().getErrors().contains(error)) {
                events.add(
                        ValidationEvent.builder()
                                .id(getEventId() + "." + error.getName())
                                .severity(Severity.WARNING)
                                .message(String.format(
                                        "The `%s` error was added to the `%s` operation. This "
                                                + "is backward-compatible if the error is only "
                                                + "encountered as a result of a change in behavior of "
                                                + "the client (for example, the client sends a new "
                                                + "parameter to an operation).",
                                        error,
                                        change.getShapeId()))
                                .shape(change.getNewShape())
                                .sourceLocation(errorSource)
                                .build());
            }
        }

        return events;
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.iam.traits;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import software.amazon.smithy.aws.traits.ArnTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SmithyInternalApi;
import software.amazon.smithy.utils.StringUtils;

/**
 * Ensures that any resource name defined in the {@link IamResourceTrait} is
 * consistent with the resource name used in any {@link ArnTrait} definition
 * applied to the resource.
 */
@SmithyInternalApi
public class IamResourceTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> results = new ArrayList<>();
        for (ResourceShape resource : model.getResourceShapesWithTrait(IamResourceTrait.class)) {
            // If the resource has both the IamResourceTrait and Arn trait,
            // check that the resource name is consistent between the two traits
            if (resource.hasTrait(ArnTrait.ID)) {
                String resourceName = resource.expectTrait(IamResourceTrait.class)
                        .getName()
                        .orElseGet(() -> StringUtils.lowerCase(resource.getId().getName()));
                ArnTrait arnTrait = resource.expectTrait(ArnTrait.class);
                List<String> arnComponents = parseArnComponents(arnTrait.getTemplate());

                // Do not check for a matching resource name when the arn is marked as absolute
                if (!arnComponents.contains(resourceName) && !arnTrait.isAbsolute()) {
                    results.add(danger(resource,
                            String.format(
                                    "The `@aws.iam#iamResource` trait applied to the resource "
                                            + "defines an IAM resource name, `%s`, that does not match the `@arn` template, "
                                            + "`%s`, of the resource.",
                                    resourceName,
                                    arnTrait.getTemplate())));
                }
            }
        }
        return results;
    }

    private List<String> parseArnComponents(String arnTemplate) {
        List<String> components = new ArrayList<>();
        for (String component : arnTemplate.split("/")) {
            components.addAll(Arrays.asList(component.split(":")));
        }
        return components;
    }
}

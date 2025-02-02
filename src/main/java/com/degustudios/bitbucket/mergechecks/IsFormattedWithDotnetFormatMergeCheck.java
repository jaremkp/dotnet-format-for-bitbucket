package com.degustudios.bitbucket.mergechecks;

import com.atlassian.bitbucket.hook.repository.PreRepositoryHookContext;
import com.atlassian.bitbucket.hook.repository.PullRequestMergeHookRequest;
import com.atlassian.bitbucket.hook.repository.RepositoryHookResult;
import com.atlassian.bitbucket.hook.repository.RepositoryMergeCheck;
import com.degustudios.dotnetformat.DotnetFormatCommandResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component("isFormattedWithDotnetFormatMergeCheck")
public class IsFormattedWithDotnetFormatMergeCheck implements RepositoryMergeCheck {
    private static final String rejectedSummaryMessageWhenRun = "Dotnet format has found issues.";
    private static final String rejectedSummaryMessageWhenCouldNotRun = "Dotnet format could not be run.";
    public static final String DOTNET_FORMAT_PARAMS = "dotnetFormatParams";
    private final DotnetFormatRefValidator dotnetFormatRefValidator;
    private final PullRequestCommenter pullRequestCommenter;

    @Autowired
    public IsFormattedWithDotnetFormatMergeCheck(
            @Qualifier("IdempotentlyCachedDotnetFormatRefValidatorWrapper") DotnetFormatRefValidator validator,
            PullRequestCommenter pullRequestCommenter) {
        this.dotnetFormatRefValidator = validator;
        this.pullRequestCommenter = pullRequestCommenter;
    }

    @Nonnull
    @Override
    public RepositoryHookResult preUpdate(@Nonnull PreRepositoryHookContext context,
                                          @Nonnull PullRequestMergeHookRequest request) {
        String dotnetFormatParams = context.getSettings().getString(DOTNET_FORMAT_PARAMS);
        DotnetFormatCommandResult result = dotnetFormatRefValidator.validate(request.getFromRef(), dotnetFormatParams);
        if (result.getExitCode() == 0) {
            return RepositoryHookResult.accepted();
        } else if (!result.hasExecutedCorrectly()) {
            return RepositoryHookResult.rejected(rejectedSummaryMessageWhenCouldNotRun, result.getMessage());
        } else {
            pullRequestCommenter.addComment(
                    request.getPullRequest(),
                    "dotnet-format results:" + System.lineSeparator() + result.getMessage());
            return RepositoryHookResult.rejected(
                    rejectedSummaryMessageWhenRun,
                    "Dotnet format exit code: " + result.getExitCode());
        }
    }
}
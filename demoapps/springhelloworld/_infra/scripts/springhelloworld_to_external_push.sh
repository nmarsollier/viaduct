#!/usr/bin/env bash

# Pushes new commits from airbnb/viaduct springhelloworld to the viaduct-graphql/spring-starter repository.
# Based on the main viaduct OSS copybara push script.
#
# Use --dry-run to test script changes
# Use --verbose to debug copybara behavior

# How to run locally:
# 1. Set up VIADUCT_SPRING_STARTER_GITHUB_ACCESS_TOKEN environment variable
# 2. Optionally set VIADUCT_SPRING_STARTER_REPO_URL (defaults to the public repo)
# 3. Run this script from the airbnb/viaduct repository with copybara installed

readonly ACC_TOK="$VIADUCT_SPRING_STARTER_GITHUB_ACCESS_TOKEN"
readonly DESTINATION_REPO="${VIADUCT_SPRING_STARTER_REPO_URL:-https://github.com/viaduct-graphql/spring-starter.git}"

# Validate required environment variables
if [[ -z "$ACC_TOK" ]]; then
    echo "Error: VIADUCT_SPRING_STARTER_GITHUB_ACCESS_TOKEN environment variable is required"
    exit 1
fi

# netrc changes auth details for curl and git under the hood.
NETRC_ENTRY=$(cat <<EOF
machine github.com
login x-access-token
password $ACC_TOK
EOF
)

echo "# BEGIN TEMP GIT ACCESS" >> ~/.netrc
echo "$NETRC_ENTRY" >> ~/.netrc
echo "# END TEMP GIT ACCESS" >> ~/.netrc
chmod 600 ~/.netrc

# Ensure clean-up
trap 'sed -i.bak "/# BEGIN TEMP GIT ACCESS/,/# END TEMP GIT ACCESS/d" ~/.netrc; rm -f ~/.netrc.bak' EXIT

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
DESTINATION_BRANCH_NAME="main"

# Run copybara with the springhelloworld configuration
copybara \
  --git-committer-email "viaduct-maintainers@airbnb.com" \
  --git-committer-name "ViaductBot" \
  --git-destination-push "$DESTINATION_BRANCH_NAME" \
  --git-destination-url="$DESTINATION_REPO" \
  "$SCRIPT_DIR"/../../copy.bara.sky airbnb-viaduct-to-spring-starter

# Google has an integration test, expecting 4 for NO_OP.
# https://github.com/google/copybara/blob/master/copybara/integration/tool_test.sh#L24
# This also reinforces what we see in CI when there is no diff to merge.

NO_OP_EXIT_CODE=4
exit_code=$?
if [ $exit_code -eq 0 ] || [ $exit_code -eq $NO_OP_EXIT_CODE ]; then
    exit 0
else
    exit $exit_code
fi
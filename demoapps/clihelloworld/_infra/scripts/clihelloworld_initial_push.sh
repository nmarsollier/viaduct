#!/usr/bin/env bash

# Creates the initial commit in the external cli-starter repository.
# This will overwrite any existing content in the destination repository.
#
# Use --dry-run to test script changes
# Use --verbose to debug copybara behavior
# Use --last-rev <commit> to specify starting revision
# Use --init-history to import full history from the beginning

# How to run locally:
# 1. Set up VIADUCT_CLI_STARTER_GITHUB_ACCESS_TOKEN environment variable
# 2. Optionally set VIADUCT_CLI_STARTER_REPO_URL (defaults to the public repo)
# 3. Run this script from the clihelloworld directory

readonly ACC_TOK="$VIADUCT_CLI_STARTER_GITHUB_ACCESS_TOKEN"
readonly DESTINATION_REPO="${VIADUCT_CLI_STARTER_REPO_URL:-https://github.com/viaduct-graphql/cli-starter.git}"

# Validate required environment variables
if [[ -z "$ACC_TOK" ]]; then
    echo "Error: VIADUCT_CLI_STARTER_GITHUB_ACCESS_TOKEN environment variable is required"
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

echo "WARNING: This will create an initial commit that may overwrite existing content in the destination repository."
echo "Destination: $DESTINATION_REPO"
echo "Press Ctrl+C to cancel, or any key to continue..."
read -n 1

# Run copybara with the initial-commit workflow
# Pass through any additional arguments to copybara
yak script tools/copybara:run \
  --git-committer-email "viaduct-maintainers@airbnb.com" \
  --git-committer-name "ViaductBot" \
  --git-destination-push "$DESTINATION_BRANCH_NAME" \
  --git-destination-url="$DESTINATION_REPO" \
  "$@" \
  "$SCRIPT_DIR"/../../copy.bara.sky initial-commit

# Google has an integration test, expecting 4 for NO_OP.
NO_OP_EXIT_CODE=4
exit_code=$?
if [ $exit_code -eq 0 ] || [ $exit_code -eq $NO_OP_EXIT_CODE ]; then
    echo "Initial commit completed successfully!"
    exit 0
else
    echo "Initial commit failed with exit code $exit_code"
    exit $exit_code
fi
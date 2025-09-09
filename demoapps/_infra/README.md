# Viaduct Demo Applications CI/CD Setup

This directory contains the infrastructure configuration for synchronizing Viaduct demo applications from the main `airbnb/viaduct` repository to the external starter repositories in the `viaduct-graphql` GitHub organization.

## Overview

The setup uses Copybara to automatically sync changes from:
- `airbnb/viaduct:demoapps/clihelloworld/` → `viaduct-graphql/cli-starter`
- `airbnb/viaduct:demoapps/springhelloworld/` → `viaduct-graphql/spring-starter`

## Architecture

```
airbnb/viaduct (GitHub)
├── demoapps/clihelloworld/     → viaduct-graphql/cli-starter
├── demoapps/springhelloworld/  → viaduct-graphql/spring-starter
└── .circleci/config.yml        (Contains sync jobs)
```

## How It Works

1. **Trigger**: When changes are pushed to the `main` or `master` branch of `airbnb/viaduct`
2. **Build & Test**: Standard CI pipeline runs (build, test, lint)  
3. **Sync**: After successful tests, Copybara jobs sync changes to starter repos
4. **Result**: Updated starter repositories reflect the latest demo application code

## Configuration

### CircleCI Environment Variables

The following environment variables must be configured in the `airbnb/viaduct` CircleCI project:

1. **CLI_STARTER_GITHUB_TOKEN** - GitHub access token with write permissions to `viaduct-graphql/cli-starter`
2. **SPRING_STARTER_GITHUB_TOKEN** - GitHub access token with write permissions to `viaduct-graphql/spring-starter`

### Copybara Configuration Files

Each demo application has its own Copybara configuration:

- `clihelloworld/copy.bara.sky` - Syncs CLI Hello World to cli-starter repo
- `springhelloworld/copy.bara.sky` - Syncs Spring Hello World to spring-starter repo

### Sync Scripts

Manual sync scripts are available for testing and troubleshooting:

- `clihelloworld/_infra/scripts/clihelloworld_to_external_push.sh`
- `springhelloworld/_infra/scripts/springhelloworld_to_external_push.sh`

## Manual Sync

To manually sync changes (for testing or troubleshooting):

1. Clone the `airbnb/viaduct` repository
2. Install Copybara locally
3. Set the required environment variables:
   ```bash
   export VIADUCT_CLI_STARTER_GITHUB_ACCESS_TOKEN="your_token"
   export VIADUCT_SPRING_STARTER_GITHUB_ACCESS_TOKEN="your_token"
   ```
4. Run the sync script:
   ```bash
   cd demoapps/clihelloworld
   ./_infra/scripts/clihelloworld_to_external_push.sh
   ```

## Troubleshooting

### Common Issues

1. **Authentication Failures**
   - Verify GitHub tokens have correct permissions
   - Check token expiration dates
   - Ensure tokens are properly set in CircleCI environment variables

2. **Copybara Sync Failures**
   - Check for conflicting changes in destination repositories
   - Verify Copybara configuration syntax
   - Review transformation rules for path mapping issues

3. **Build Failures**
   - Ensure all tests pass before sync attempts
   - Check for compilation errors in demo applications
   - Verify dependency versions are compatible

### Logs and Monitoring

- CircleCI build logs contain detailed sync information
- Copybara provides exit codes: 0 (success), 4 (no-op), other (error)
- GitHub webhooks can be configured for additional notifications

## Migration Notes

This setup replaces the previous treehouse-based CI/CD pipeline with a GitHub-native approach using:

- **Source**: `airbnb/viaduct` (GitHub) instead of treehouse
- **CI/CD**: CircleCI instead of treehouse JORB system
- **Copybara**: Direct GitHub-to-GitHub sync instead of file-based workflows

The migration maintains the same end result (synchronized starter repositories) while simplifying the infrastructure and removing dependencies on internal Airbnb systems.
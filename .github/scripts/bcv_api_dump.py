# .github/scripts/generate_api_dump.py
# This script generates API dump files for public API modules.

import sys
from bcv_modules import api_dump_tasks, run_gradle_task


def main(argv: list[str]) -> None:
    # This script forwards any extra CLI args to Gradle (e.g. --no-build-cache, --stacktrace)
    extra_args = argv[1:]

    # Run all API dump tasks in api_modules.py projects
    for task in api_dump_tasks():
        run_gradle_task(task, extra_args)


if __name__ == "__main__":
    main(sys.argv)

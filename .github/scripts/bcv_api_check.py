# .github/scripts/run_api_check.py
# This script runs the API compatibility checks for all relevant modules.

import sys

from bcv_modules import api_check_tasks, run_gradle_task


def main(argv: list[str]) -> None:
	# This script forwards any extra CLI args to Gradle (e.g. --no-build-cache, --stacktrace)
	extra_args = argv[1:]

	for task in api_check_tasks():
		run_gradle_task(task, extra_args)


if __name__ == "__main__":
	main(sys.argv)
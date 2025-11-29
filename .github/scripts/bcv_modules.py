# .github/scripts/api_modules.py
# Binary Compatibility Validation (BCV) modules configuration and common libraries

import os
import subprocess
import sys
from pathlib import Path

# List of Gradle modules that expose public APIs we want to validate with binary compatibility validator.
API_MODULES = [
    ":core:tenant:tenant-api",
    ":core:service:service-api",
]


def api_check_tasks() -> list[str]:
    """Gradle tasks used to validate binary API compatibility."""
    return [f"{m}:apiCheck" for m in API_MODULES]


def api_dump_tasks() -> list[str]:
    """Gradle tasks used to regenerate API dumps (baseline)."""
    return [f"{m}:apiDump" for m in API_MODULES]


def repo_root() -> Path:
    # Return the repository root directory (the directory that contains .github/).
    scripts_dir = Path(__file__).resolve().parent
    return scripts_dir.parent.parent


def run_gradle_task(task: str, extra_args: list[str]) -> None:
    # Run a Gradle task from the repo root, forwarding any extra args.
    root = repo_root()
    cmd = ["./gradlew", task, *extra_args]
    result = subprocess.run(cmd, cwd=root)
    if result.returncode != 0:
        sys.exit(result.returncode)

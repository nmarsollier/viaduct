import unittest
import sys
from pathlib import Path

# Add parent directory to path so we can import the module
sys.path.insert(0, str(Path(__file__).parent.parent))

from generate_changelog import extract_username_from_email, extract_username, format_entry, clean_commit_message


class TestCleanCommitMessage(unittest.TestCase):
    def test_removes_github_change_id(self):
        message = "Fix bug Github-Change-Id: 956283"
        result = clean_commit_message(message)
        self.assertEqual(result, "Fix bug")
        self.assertNotIn("Github-Change-Id", result)

    def test_removes_gitorigin_revid(self):
        message = "Update docs GitOrigin-RevId: 1fcdd8123bc49a717103985430322eca3b5b1fb3"
        result = clean_commit_message(message)
        self.assertEqual(result, "Update docs")
        self.assertNotIn("GitOrigin-RevId", result)

    def test_keeps_closes_issue_reference(self):
        message = "Fix parser Closes #137"
        result = clean_commit_message(message)
        self.assertEqual(result, "Fix parser Closes #137")
        self.assertIn("Closes #137", result)

    def test_keeps_fixes_issue_reference(self):
        message = "Fix bug Fixes #456"
        result = clean_commit_message(message)
        self.assertEqual(result, "Fix bug Fixes #456")
        self.assertIn("Fixes #456", result)

    def test_removes_multiple_metadata_patterns(self):
        message = "Feature Github-Change-Id: 123 GitOrigin-RevId: abc123"
        result = clean_commit_message(message)
        self.assertEqual(result, "Feature")
        self.assertNotIn("Github-Change-Id", result)
        self.assertNotIn("GitOrigin-RevId", result)

    def test_mixed_metadata_and_issue_reference(self):
        message = "Build docs Closes #137 Github-Change-Id: 956283"
        result = clean_commit_message(message)
        self.assertEqual(result, "Build docs Closes #137")
        self.assertIn("Closes #137", result)
        self.assertNotIn("Github-Change-Id", result)

    def test_case_insensitive_matching(self):
        message = "Fix GITHUB-CHANGE-ID: 789"
        result = clean_commit_message(message)
        self.assertNotIn("GITHUB-CHANGE-ID", result)


class TestExtractUsernameFromEmail(unittest.TestCase):
    def test_valid_email(self):
        self.assertEqual(extract_username_from_email("john.doe@example.com"), "@john.doe")

    def test_email_with_plus(self):
        self.assertEqual(extract_username_from_email("john+test@example.com"), "@john+test")

    def test_empty_email(self):
        self.assertEqual(extract_username_from_email(""), "")

    def test_noreply_email(self):
        self.assertEqual(extract_username_from_email("noreply@github.com"), "")

    def test_no_reply_email(self):
        self.assertEqual(extract_username_from_email("no-reply@github.com"), "")

    def test_github_actions_email(self):
        self.assertEqual(extract_username_from_email("github-actions@github.com"), "")

    def test_viaductbot_email(self):
        self.assertEqual(extract_username_from_email("viaductbot@airbnb.com"), "")

    def test_invalid_email_no_at(self):
        self.assertEqual(extract_username_from_email("notanemail"), "")

    def test_email_with_dash(self):
        self.assertEqual(extract_username_from_email("john-doe@example.com"), "@john-doe")

    def test_email_with_underscore(self):
        self.assertEqual(extract_username_from_email("john_doe@example.com"), "@john_doe")


class TestExtractUsername(unittest.TestCase):
    def test_valid_co_author_line(self):
        self.assertEqual(
            extract_username("John Doe <john.doe@example.com>"),
            "@john.doe"
        )

    def test_co_author_with_full_name(self):
        self.assertEqual(
            extract_username("Jane Smith <jane.smith@company.com>"),
            "@jane.smith"
        )

    def test_noreply_co_author(self):
        self.assertEqual(
            extract_username("GitHub <noreply@github.com>"),
            ""
        )

    def test_github_actions_co_author(self):
        self.assertEqual(
            extract_username("Actions Bot <github-actions@github.com>"),
            ""
        )

    def test_viaductbot_co_author(self):
        self.assertEqual(
            extract_username("Viaduct Bot <viaductbot@airbnb.com>"),
            ""
        )

    def test_invalid_format_no_email(self):
        self.assertEqual(extract_username("John Doe"), "")

    def test_email_only(self):
        self.assertEqual(
            extract_username("<alice@example.com>"),
            "@alice"
        )

    def test_co_author_with_plus_in_email(self):
        self.assertEqual(
            extract_username("Bob <bob+test@example.com>"),
            "@bob+test"
        )


class TestFormatEntry(unittest.TestCase):
    def test_single_author_no_coauthors(self):
        entry = "Fix bug in parser by AUTHOR_STARTjohn.doe@example.comAUTHOR_END CO_AUTHORS_STARTCO_AUTHORS_END"
        result = format_entry(entry)
        self.assertEqual(result, "Fix bug in parser by @john.doe")

    def test_author_with_single_coauthor(self):
        entry = "Add new feature by AUTHOR_STARTjohn.doe@example.comAUTHOR_END CO_AUTHORS_STARTJane Smith <jane.smith@example.com>CO_AUTHORS_END"
        result = format_entry(entry)
        self.assertEqual(result, "Add new feature by @john.doe, @jane.smith")

    def test_author_with_multiple_coauthors(self):
        entry = "Refactor code by AUTHOR_STARTalice@example.comAUTHOR_END CO_AUTHORS_STARTBob <bob@example.com>|Charlie <charlie@example.com>CO_AUTHORS_END"
        result = format_entry(entry)
        self.assertEqual(result, "Refactor code by @alice, @bob, @charlie")

    def test_bot_author_filtered_out(self):
        entry = "Update dependencies by AUTHOR_STARTnoreply@github.comAUTHOR_END CO_AUTHORS_STARTCO_AUTHORS_END"
        result = format_entry(entry)
        self.assertEqual(result, "Update dependencies by @anonymous")

    def test_bot_coauthor_filtered_out(self):
        entry = "Merge PR by AUTHOR_STARTjohn@example.comAUTHOR_END CO_AUTHORS_STARTGitHub Actions <github-actions@github.com>CO_AUTHORS_END"
        result = format_entry(entry)
        self.assertEqual(result, "Merge PR by @john")

    def test_all_bots_filtered_out(self):
        entry = "Auto update by AUTHOR_STARTnoreply@github.comAUTHOR_END CO_AUTHORS_STARTBot <viaductbot@airbnb.com>CO_AUTHORS_END"
        result = format_entry(entry)
        self.assertEqual(result, "Auto update by @anonymous")

    def test_empty_coauthors_segment(self):
        entry = "Simple commit by AUTHOR_STARTdev@example.comAUTHOR_END CO_AUTHORS_STARTCO_AUTHORS_END"
        result = format_entry(entry)
        self.assertEqual(result, "Simple commit by @dev")

    def test_commit_message_with_special_chars(self):
        entry = "Fix: handle [special] chars (issue #123) by AUTHOR_STARTuser@example.comAUTHOR_END CO_AUTHORS_STARTCO_AUTHORS_END"
        result = format_entry(entry)
        self.assertEqual(result, "Fix: handle [special] chars (issue #123) by @user")

    def test_mixed_valid_and_invalid_coauthors(self):
        entry = "Team effort by AUTHOR_STARTlead@example.comAUTHOR_END CO_AUTHORS_STARTDev1 <dev1@example.com>|Bot <noreply@github.com>|Dev2 <dev2@example.com>CO_AUTHORS_END"
        result = format_entry(entry)
        self.assertEqual(result, "Team effort by @lead, @dev1, @dev2")

    def test_coauthor_with_empty_entries(self):
        entry = "Update docs by AUTHOR_STARTauthor@example.comAUTHOR_END CO_AUTHORS_START|Helper <helper@example.com>|CO_AUTHORS_END"
        result = format_entry(entry)
        self.assertEqual(result, "Update docs by @author, @helper")

    def test_commit_with_metadata_in_subject(self):
        """
        Test that exposes the issue where commits without proper blank line after subject
        include metadata (Github-Change-Id, Closes, etc.) in the changelog entry.

        This happens because git's %s format joins multi-line subjects with spaces.
        Metadata should be filtered out from the changelog.
        """
        entry = "Build from scratch docs improvement Closes #137 Github-Change-Id: 956283 by AUTHOR_STARTuser@example.comAUTHOR_END CO_AUTHORS_STARTCO_AUTHORS_END"
        result = format_entry(entry)

        # Assert that metadata is NOT in the result
        self.assertNotIn("Github-Change-Id", result)
        self.assertIn("Closes #137", result)
        self.assertIn("Build from scratch docs improvement", result)
        self.assertIn("@user", result)

    def test_commit_with_gitorigin_revid_in_subject(self):
        """
        Test for filtering out GitOrigin-RevId metadata from changelog entries.
        """
        entry = "Fix parser bug GitOrigin-RevId: 1fcdd8123bc49a717103985430322eca3b5b1fb3 by AUTHOR_STARTdev@example.comAUTHOR_END CO_AUTHORS_STARTCO_AUTHORS_END"
        result = format_entry(entry)

        # Assert that GitOrigin-RevId is NOT in the result
        self.assertNotIn("GitOrigin-RevId", result)
        self.assertIn("Fix parser bug", result)
        self.assertIn("@dev", result)


if __name__ == '__main__':
    unittest.main()

package pl.lublin.wsei.plan.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.lublin.wsei.plan.domain.update.AppUpdateStatus
import pl.lublin.wsei.plan.domain.update.VersionComparator

class GitHubReleaseParserTest {

    private val body = """
        [
          {
            "tag_name": "v0.7.5", "name": "Taal 0.7.5", "body": "Poprawki\r\n- a", "draft": false,
            "prerelease": false, "published_at": "2026-10-01T10:00:00Z",
            "html_url": "https://github.com/m1chi0001/Taal/releases/tag/v0.7.5",
            "assets": [
              {"name": "taal-debug.apk", "browser_download_url": "https://github.com/d.apk", "size": 10},
              {"name": "taal-release.apk", "browser_download_url": "https://github.com/r.apk", "size": 20}
            ]
          },
          { "tag_name": "v0.8.0-beta", "name": "Beta", "body": "x", "draft": false, "prerelease": true, "assets": [] },
          { "tag_name": "v0.9.0", "name": "Draft", "body": "x", "draft": true, "prerelease": false, "assets": [] },
          {
            "tag_name": "v0.7.10", "name": null, "body": null, "draft": false, "prerelease": false,
            "published_at": "2026-11-20T10:00:00Z", "html_url": "https://github.com/m1chi0001/Taal/releases/tag/v0.7.10",
            "assets": [{"name": "notes.txt", "browser_download_url": "https://github.com/n.txt", "size": 1}],
            "unknown_field": {"nested": true}
          },
          { "tag_name": "0.7.6", "name": "", "body": "Nowości", "draft": false, "prerelease": false, "assets": [] }
        ]
    """.trimIndent()

    @Test
    fun `drafts and prereleases are skipped, newest first`() {
        val releases = GitHubReleaseParser.parseReleases(body)
        assertEquals(listOf("0.7.10", "0.7.6", "0.7.5"), releases.map { it.version })
    }

    @Test
    fun `fields are mapped`() {
        val releases = GitHubReleaseParser.parseReleases(body)
        val newest = releases[0]
        assertEquals("v0.7.10", newest.tagName)
        assertEquals("v0.7.10", newest.title) // no name → tag
        assertEquals("", newest.notesMarkdown)
        assertNull(newest.apkUrl)

        val r076 = releases[1]
        assertEquals("0.7.6", r076.title) // blank name → tag
        assertEquals("Nowości", r076.notesMarkdown)
        assertEquals("https://github.com/m1chi0001/Taal/releases/tag/0.7.6", r076.htmlUrl)

        val r075 = releases[2]
        assertEquals("Taal 0.7.5", r075.title)
        assertEquals("taal-release.apk", r075.apkName)
        assertEquals("https://github.com/r.apk", r075.apkUrl)
        assertEquals(20L, r075.apkSizeBytes)
        assertEquals("2026-10-01T10:00:00Z", r075.publishedAtIso)
    }

    @Test
    fun `empty list`() {
        assertTrue(GitHubReleaseParser.parseReleases("[]").isEmpty())
    }

    @Test(expected = Exception::class)
    fun `malformed body throws`() {
        GitHubReleaseParser.parseReleases("{\"message\":\"Not Found\"}")
    }

    @Test
    fun `cache round trip`() {
        val releases = GitHubReleaseParser.parseReleases(body)
        assertEquals(releases, GitHubReleaseParser.decodeCache(GitHubReleaseParser.encodeCache(releases)))
        assertTrue(GitHubReleaseParser.decodeCache("not json").isEmpty())
    }

    @Test
    fun `version comparison`() {
        assertTrue(VersionComparator.compare("0.7.10", "0.7.9") > 0)
        assertTrue(VersionComparator.compare("v0.8", "0.7.6") > 0)
        assertEquals(0, VersionComparator.compare("v0.7.6", "0.7.6"))
        assertEquals(0, VersionComparator.compare("0.7", "0.7.0"))
        assertTrue(VersionComparator.compare("2026112212345678901", "0.7.6") > 0) // no overflow crash
        assertTrue(UpdateManager.isNewer("0.5.1", "0.5"))
        assertFalse(UpdateManager.isNewer("latest", "0.5"))
        assertFalse(UpdateManager.isNewer("0.7.6", "0.7.6"))
    }

    @Test
    fun `prompt only for a newer, not dismissed release`() {
        val latest = GitHubReleaseParser.parseReleases(body).first()
        val status = AppUpdateStatus(currentVersion = "0.7.6", latest = latest)
        assertTrue(status.isUpdateAvailable)
        assertTrue(status.shouldPrompt)
        assertFalse(status.copy(dismissedVersion = "0.7.10").shouldPrompt)
        assertFalse(status.copy(currentVersion = "0.7.10").isUpdateAvailable)
    }
}

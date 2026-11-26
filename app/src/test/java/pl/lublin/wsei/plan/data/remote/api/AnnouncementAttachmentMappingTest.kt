package pl.lublin.wsei.plan.data.remote.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.lublin.wsei.plan.domain.model.AnnouncementAttachment

class AnnouncementAttachmentMappingTest {

    @Test
    fun `synthetic attachment without own id falls back to announcementId download attempts`() {
        val synthetic = AnnouncementAttachment(
            id = -421,
            fileName = "",
            announcementId = 421
        )
        val attempts = ArpApiDataSource.buildAttachmentAttempts(studId = 777, attachment = synthetic)
        val labels = attempts.map { it.first }
        assertTrue(labels.contains("announcementId"))
        assertTrue(labels.contains("AnnouncementId"))
        assertTrue(labels.contains("Id=announcementId"))
        assertTrue(labels.contains("id=announcementId"))
        assertTrue(attempts.all { "421" in it.second && "-421" !in it.second })
        assertTrue(attempts.all { "IdLoggedPersonType=777" in it.second })
    }

    @Test
    fun `real attachment id prioritizes attachment id attempts before announcementId fallback`() {
        val real = AnnouncementAttachment(
            id = 98,
            fileName = "regulamin.pdf",
            announcementId = 421
        )
        val attempts = ArpApiDataSource.buildAttachmentAttempts(studId = 777, attachment = real)
        assertEquals("Id", attempts.first().first)
        assertTrue(attempts.first().second.contains("Id=98"))
        assertTrue(attempts.any { it.first == "Id+announcementId" })
    }

    @Test
    fun `MsAnnouncementDto and MsAnnouncementDetailsDto merge alternate attachment fields`() {
        val inlineSingle = MsAnnouncementDto(
            id = 55,
            isAttachment = true,
            attachmentId = 901,
            attachmentName = "wniosek.pdf"
        )
        assertEquals(1, inlineSingle.allAttachments().size)
        assertEquals(901, inlineSingle.allAttachments().single().id)
        assertEquals("wniosek.pdf", inlineSingle.allAttachments().single().name)

        val details = MsAnnouncementDetailsDto(
            id = 55,
            announcement = MsAnnouncementDto(
                id = 55,
                files = listOf(MsAnnouncementAttachmentDto(id = 902, fileName = "zalacznik_1.pdf"))
            )
        )
        assertEquals(1, details.allAttachments().size)
        assertEquals(902, details.allAttachments().single().id)
    }
}

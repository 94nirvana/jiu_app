package com.jiuapp.domain.training

import com.jiuapp.domain.InvalidAttachmentException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MediaLinkTest {

    @Test
    fun `aceita https nos hosts permitidos e subdominios`() {
        assertTrue(MediaLink.isAllowed("https://www.youtube.com/watch?v=abc"))
        assertTrue(MediaLink.isAllowed("https://youtu.be/abc"))
        assertTrue(MediaLink.isAllowed("https://instagram.com/p/xyz"))
        assertTrue(MediaLink.isAllowed("https://drive.google.com/file/d/1"))
    }

    @Test
    fun `rejeita esquemas perigosos e http`() {
        assertFalse(MediaLink.isAllowed("javascript:alert(1)"))
        assertFalse(MediaLink.isAllowed("file:///etc/passwd"))
        assertFalse(MediaLink.isAllowed("http://youtube.com/watch"))
    }

    @Test
    fun `rejeita hosts que apenas terminam com nome permitido`() {
        assertFalse(MediaLink.isAllowed("https://youtube.com.evil.example/x"))
        assertFalse(MediaLink.isAllowed("https://notyoutube.com/x"))
    }

    @Test
    fun `attachment de video invalido lanca excecao de dominio`() {
        assertFailsWith<InvalidAttachmentException> {
            Attachment("a1", AttachmentKind.VIDEO_LINK, "https://evil.example/v")
        }
    }

    @Test
    fun `foto nao passa pela validacao de link`() {
        Attachment("a2", AttachmentKind.PHOTO, "content://media/1")
    }
}

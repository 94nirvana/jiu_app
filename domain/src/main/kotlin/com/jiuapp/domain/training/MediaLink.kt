package com.jiuapp.domain.training

import com.jiuapp.domain.InvalidAttachmentException
import java.net.URI

enum class AttachmentKind { PHOTO, VIDEO_LINK }

data class Attachment(
    val id: String,
    val kind: AttachmentKind,
    val uri: String,
) {
    init {
        if (kind == AttachmentKind.VIDEO_LINK) MediaLink.requireAllowed(uri)
    }
}

/**
 * Allowlist de hosts para links de vídeo. Evita que o app abra URLs arbitrárias
 * (phishing, esquemas perigosos como javascript: ou file:).
 */
object MediaLink {

    private val allowedHosts = setOf(
        "youtube.com",
        "youtu.be",
        "instagram.com",
        "drive.google.com",
    )

    fun isAllowed(url: String): Boolean {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return false
        if (uri.scheme != "https") return false
        val host = uri.host?.lowercase() ?: return false
        return allowedHosts.any { allowed -> host == allowed || host.endsWith(".$allowed") }
    }

    fun requireAllowed(url: String) {
        if (!isAllowed(url)) {
            throw InvalidAttachmentException("Link de vídeo não permitido. Use HTTPS do YouTube, Instagram ou Drive.")
        }
    }
}

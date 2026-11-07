package com.jiuapp.ui.sessions

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.jiuapp.R
import com.jiuapp.domain.DomainException
import com.jiuapp.domain.InvalidSessionException
import com.jiuapp.domain.belt.Belt
import com.jiuapp.domain.belt.Rank
import com.jiuapp.domain.repository.GraduationRepository
import com.jiuapp.domain.repository.SessionRepository
import com.jiuapp.domain.repository.SettingsRepository
import com.jiuapp.domain.repository.TechniqueRepository
import com.jiuapp.domain.stats.latestGraduation
import com.jiuapp.domain.technique.Technique
import com.jiuapp.domain.training.Attachment
import com.jiuapp.domain.training.AttachmentKind
import com.jiuapp.domain.training.MediaLink
import com.jiuapp.domain.training.TrainingSession
import com.jiuapp.domain.training.TrainingType
import com.jiuapp.ui.nav.NEW
import com.jiuapp.ui.nav.SessionFormRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionFormState(
    val isNew: Boolean = true,
    val id: String = "",
    val date: LocalDate? = null,
    val startTime: LocalTime? = null,
    val durationText: String = "",
    val type: TrainingType = TrainingType.GI,
    val intensity: Int = DEFAULT_INTENSITY,
    val belt: Belt = Belt.WHITE,
    val degree: Int = 0,
    val academy: String = "",
    val professor: String = "",
    val fatigue: Int? = null,
    val pain: Int? = null,
    val mood: Int? = null,
    val energy: Int? = null,
    val learned: String = "",
    val toFix: String = "",
    val notes: String = "",
    val tagsText: String = "",
    val focusTagsText: String = "",
    val techniqueIds: Set<String> = emptySet(),
    val techniques: List<Technique> = emptyList(),
    val photoUris: List<String> = emptyList(),
    val videoLinksText: String = "",
    val loading: Boolean = true,
    val saving: Boolean = false,
    val saved: Boolean = false,
    @StringRes val error: Int? = null,
) {
    companion object {
        const val DEFAULT_INTENSITY = 3
    }
}

@HiltViewModel
class SessionFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessions: SessionRepository,
    private val techniques: TechniqueRepository,
    private val graduations: GraduationRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<SessionFormRoute>()
    private val _state = MutableStateFlow(SessionFormState())
    val state: StateFlow<SessionFormState> = _state

    private var existing: TrainingSession? = null

    init {
        viewModelScope.launch {
            val options = techniques.observeAll().first().filter { it.deletedAt == null }.sortedBy { it.name }
            val appSettings = settings.observeSettings().first()
            if (route.id == NEW) {
                val today = LocalDate.now(clock)
                val rank = latestGraduation(graduations.observeAll().first())?.rank ?: Rank(Belt.WHITE, 0)
                _state.value = SessionFormState(
                    isNew = true,
                    id = UUID.randomUUID().toString(),
                    date = today,
                    durationText = appSettings.defaultSessionMinutes.toString(),
                    belt = rank.belt,
                    degree = rank.degree,
                    techniques = options,
                    loading = false,
                )
            } else {
                val session = sessions.get(route.id)
                existing = session
                _state.value = if (session == null) {
                    _state.value.copy(loading = false, error = R.string.session_not_found)
                } else {
                    session.toFormState(options)
                }
            }
        }
    }

    fun update(transform: (SessionFormState) -> SessionFormState) = _state.update { transform(it).copy(error = null) }

    fun addPhoto(uri: String) = update { it.copy(photoUris = it.photoUris + uri) }

    fun removePhoto(uri: String) = update { it.copy(photoUris = it.photoUris - uri) }

    fun save() {
        val current = _state.value
        val validation = validateLinks(current.videoLinksText)
        if (validation != null) {
            _state.update { it.copy(error = validation) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            try {
                val session = current.toSession(existing, Instant.now(clock))
                sessions.save(session)
                _state.update { it.copy(saving = false, saved = true) }
            } catch (error: DomainException) {
                _state.update { it.copy(saving = false, error = R.string.error_invalid_data) }
            } catch (error: NumberFormatException) {
                _state.update { it.copy(saving = false, error = R.string.error_number_format) }
            }
        }
    }

    private fun validateLinks(text: String): Int? {
        val links = parseLines(text)
        return if (links.all(MediaLink::isAllowed)) null else R.string.error_video_link
    }

    private fun SessionFormState.toSession(previous: TrainingSession?, now: Instant): TrainingSession {
        val duration = durationText.trim().toIntOrNull() ?: throw NumberFormatException()
        val attachments = photoUris.map { Attachment(UUID.randomUUID().toString(), AttachmentKind.PHOTO, it) } +
            parseLines(videoLinksText).map { Attachment(UUID.randomUUID().toString(), AttachmentKind.VIDEO_LINK, it) }
        return TrainingSession(
            id = id,
            date = date ?: throw InvalidSessionException("Data obrigatória"),
            durationMinutes = duration,
            type = type,
            intensity = intensity,
            rankOnDay = Rank(belt, degree),
            fatigue = fatigue,
            pain = pain,
            mood = mood,
            energy = energy,
            startTime = startTime,
            academy = academy.trim().ifEmpty { null },
            professor = professor.trim().ifEmpty { null },
            learned = learned.trim().ifEmpty { null },
            toFix = toFix.trim().ifEmpty { null },
            notes = notes.trim().ifEmpty { null },
            tags = parseTags(tagsText),
            focusTags = parseTags(focusTagsText),
            techniqueIds = techniqueIds,
            attachments = attachments,
            updatedAt = now,
            deletedAt = previous?.deletedAt,
        )
    }

    private fun TrainingSession.toFormState(options: List<Technique>) = SessionFormState(
        isNew = false,
        id = id,
        date = date,
        startTime = startTime,
        durationText = durationMinutes.toString(),
        type = type,
        intensity = intensity,
        belt = rankOnDay.belt,
        degree = rankOnDay.degree,
        academy = academy.orEmpty(),
        professor = professor.orEmpty(),
        fatigue = fatigue,
        pain = pain,
        mood = mood,
        energy = energy,
        learned = learned.orEmpty(),
        toFix = toFix.orEmpty(),
        notes = notes.orEmpty(),
        tagsText = tags.sorted().joinToString(", "),
        focusTagsText = focusTags.sorted().joinToString(", "),
        techniqueIds = techniqueIds,
        techniques = options,
        photoUris = attachments.filter { it.kind == AttachmentKind.PHOTO }.map { it.uri },
        videoLinksText = attachments.filter { it.kind == AttachmentKind.VIDEO_LINK }.joinToString("\n") { it.uri },
        loading = false,
    )

    companion object {
        /** Tags separadas por vírgula, normalizadas em minúsculas e sem repetição. */
        fun parseTags(text: String): Set<String> =
            text.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

        fun parseLines(text: String): List<String> =
            text.lines().map { it.trim() }.filter { it.isNotEmpty() }
    }
}

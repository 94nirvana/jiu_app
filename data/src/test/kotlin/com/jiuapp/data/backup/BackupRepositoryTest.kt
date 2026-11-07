package com.jiuapp.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jiuapp.data.db.JiuDatabase
import com.jiuapp.data.repository.RoomSessionRepository
import com.jiuapp.data.security.DatabaseKeyProvider
import com.jiuapp.data.settings.DataStoreSettingsRepository
import com.jiuapp.domain.belt.Belt
import com.jiuapp.domain.belt.Rank
import com.jiuapp.domain.training.TrainingSession
import com.jiuapp.domain.training.TrainingType
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupRepositoryTest {

    private lateinit var db: JiuDatabase
    private lateinit var sessions: RoomSessionRepository
    private lateinit var backup: BackupRepository
    private val clock = Clock.systemUTC()

    @BeforeTest
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, JiuDatabase::class.java).allowMainThreadQueries().build()
        sessions = RoomSessionRepository(db)
        backup = BackupRepository(
            context = context,
            db = db,
            keys = mockk<DatabaseKeyProvider>(relaxed = true),
            settings = mockk<DataStoreSettingsRepository>(relaxed = true),
            clock = clock,
        )
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    private fun session(id: String, notes: String, updated: Instant) = TrainingSession(
        id = id,
        date = LocalDate.of(2026, 3, 10),
        durationMinutes = 75,
        type = TrainingType.SPARRING,
        intensity = 4,
        rankOnDay = Rank(Belt.BLUE, 2),
        notes = notes,
        tags = setOf("guarda"),
        focusTags = setOf("passagem"),
        updatedAt = updated,
    )

    @Test
    fun `exportar e importar preserva treinos com tags`() = runTest {
        val original = session("s1", "rola boa", Instant.parse("2026-03-10T20:00:00Z"))
        sessions.save(original)
        val json = backup.exportBackup()

        db.clearAllTables()
        assertNull(sessions.get("s1"))
        val applied = backup.importBackup(json)

        assertEquals(original, sessions.get("s1"))
        assertEquals(true, applied > 0)
    }

    @Test
    fun `registro mais antigo do backup nao sobrescreve o local`() = runTest {
        sessions.save(session("s1", "versao antiga", Instant.parse("2026-03-10T20:00:00Z")))
        val oldBackup = backup.exportBackup()
        sessions.save(session("s1", "versao nova", Instant.parse("2026-03-11T20:00:00Z")))

        backup.importBackup(oldBackup)

        assertEquals("versao nova", sessions.get("s1")?.notes)
    }

    @Test
    fun `backup com formato desconhecido e rejeitado`() = runTest {
        val json = """{"format":"outro","version":1,"exportedAt":"2026-01-01T00:00:00Z"}"""

        assertFailsWith<IllegalArgumentException> { backup.importBackup(json) }
    }

    @Test
    fun `backup maior que o limite e rejeitado sem ser lido`() = runTest {
        val huge = "x".repeat(25_000_001)

        assertFailsWith<IllegalArgumentException> { backup.importBackup(huge) }
    }
}

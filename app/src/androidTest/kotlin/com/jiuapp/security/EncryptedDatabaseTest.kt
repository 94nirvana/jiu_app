package com.jiuapp.security

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.jiuapp.data.db.JiuDatabase
import com.jiuapp.data.db.ProfileEntity
import com.jiuapp.data.di.SqlCipherLibrary
import com.jiuapp.data.security.DatabaseKeyProvider
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Roda no aparelho: valida o caminho crítico de segurança. Chave no Keystore, banco SQLCipher
 * gravando e lendo, e arquivo sem cabeçalho SQLite em texto puro.
 */
class EncryptedDatabaseTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dbName = "jiuapp-test.db"
    private val keys = DatabaseKeyProvider(context)

    @Before
    fun setUp() {
        SqlCipherLibrary.load()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun passphraseEhEstavelEntreChamadas() = runBlocking {
        val first = keys.passphrase()
        val second = keys.passphrase()

        assertEquals(32, first.size)
        assertArrayEquals(first, second)
    }

    @Test
    fun gravaELeOBancoCriptografado() = runBlocking {
        val passphrase = keys.passphrase()
        val db = Room.databaseBuilder(context, JiuDatabase::class.java, dbName)
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .build()
        db.profileDao().upsert(
            ProfileEntity(
                id = "profile",
                name = "Teste Dispositivo",
                startDate = "2024-01-02",
                birthDate = null,
                academy = null,
                professor = null,
                photoUri = null,
                updatedAt = "2026-11-02T10:00:00Z",
                deletedAt = null,
            ),
        )
        val profile = db.profileDao().observe().first()
        db.close()

        assertNotNull(profile)
        assertEquals("Teste Dispositivo", profile?.name)

        val file = context.getDatabasePath(dbName)
        assertTrue("banco não foi criado", file.exists())
        assertFalse("arquivo do banco não pode estar em texto puro", hasPlainSqliteHeader(file))
    }

    private fun hasPlainSqliteHeader(file: File): Boolean {
        val header = file.inputStream().use { it.readNBytes(SQLITE_HEADER.size) }
        return header.contentEquals(SQLITE_HEADER)
    }

    private companion object {
        val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
    }
}

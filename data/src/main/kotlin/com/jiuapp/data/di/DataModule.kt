package com.jiuapp.data.di

import android.content.Context
import androidx.room.Room
import com.jiuapp.data.backup.BackupRepository
import com.jiuapp.data.db.JiuDatabase
import com.jiuapp.data.repository.RoomAchievementRepository
import com.jiuapp.data.repository.RoomCompetitionRepository
import com.jiuapp.data.repository.RoomGamePlanRepository
import com.jiuapp.data.repository.RoomGraduationRepository
import com.jiuapp.data.repository.RoomHealthRepository
import com.jiuapp.data.repository.RoomOpponentRepository
import com.jiuapp.data.repository.RoomProfileRepository
import com.jiuapp.data.repository.RoomRosterRepository
import com.jiuapp.data.repository.RoomSessionRepository
import com.jiuapp.data.repository.RoomTechniqueRepository
import com.jiuapp.data.security.DatabaseKeyProvider
import com.jiuapp.data.settings.DataStoreSettingsRepository
import com.jiuapp.domain.repository.AchievementRepository
import com.jiuapp.domain.repository.CompetitionRepository
import com.jiuapp.domain.repository.DataRepository
import com.jiuapp.domain.repository.GamePlanRepository
import com.jiuapp.domain.repository.GraduationRepository
import com.jiuapp.domain.repository.HealthRepository
import com.jiuapp.domain.repository.OpponentRepository
import com.jiuapp.domain.repository.ProfileRepository
import com.jiuapp.domain.repository.RosterRepository
import com.jiuapp.domain.repository.SessionRepository
import com.jiuapp.domain.repository.SettingsRepository
import com.jiuapp.domain.repository.TechniqueRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keys: DatabaseKeyProvider,
    ): JiuDatabase {
        // A senha é lida do DataStore uma única vez, na primeira abertura do banco, fora da thread principal.
        val passphrase = runBlocking(Dispatchers.IO) { keys.passphrase() }
        SqlCipherLibrary.load()
        return Room.databaseBuilder(context, JiuDatabase::class.java, DATABASE_NAME)
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .build()
    }

    private const val DATABASE_NAME = "jiuapp.db"
}

/** A biblioteca nativa do SQLCipher precisa estar carregada antes de qualquer conexão. */
object SqlCipherLibrary {
    private const val NATIVE_NAME = "sqlcipher"

    fun load() = System.loadLibrary(NATIVE_NAME)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun profile(impl: RoomProfileRepository): ProfileRepository
    @Binds @Singleton abstract fun graduation(impl: RoomGraduationRepository): GraduationRepository
    @Binds @Singleton abstract fun session(impl: RoomSessionRepository): SessionRepository
    @Binds @Singleton abstract fun technique(impl: RoomTechniqueRepository): TechniqueRepository
    @Binds @Singleton abstract fun gamePlan(impl: RoomGamePlanRepository): GamePlanRepository
    @Binds @Singleton abstract fun opponent(impl: RoomOpponentRepository): OpponentRepository
    @Binds @Singleton abstract fun health(impl: RoomHealthRepository): HealthRepository
    @Binds @Singleton abstract fun competition(impl: RoomCompetitionRepository): CompetitionRepository
    @Binds @Singleton abstract fun achievement(impl: RoomAchievementRepository): AchievementRepository
    @Binds @Singleton abstract fun roster(impl: RoomRosterRepository): RosterRepository
    @Binds @Singleton abstract fun settings(impl: DataStoreSettingsRepository): SettingsRepository
    @Binds @Singleton abstract fun data(impl: BackupRepository): DataRepository
}

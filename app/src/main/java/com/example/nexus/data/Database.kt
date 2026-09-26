package com.example.nexus.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.nexus.api.ChecklistItem
import com.example.nexus.api.HabitFrequency
import com.example.nexus.api.TaskPriority
import com.example.nexus.api.TaskStatus
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow

/**
 * `userId` is the signed-in account's stable cache owner id (see `AuthSession.cacheOwnerId`).
 * Every row is stamped with it so cached data can never be read across accounts on a shared
 * device, and so a single account's cache can be purged independently.
 */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val description: String?,
    val createdAt: String?,
    val updatedAt: String?,
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val projectId: String,
    val title: String,
    val description: String?,
    val isCompleted: Boolean,
    val dueDate: String?,
    val priority: TaskPriority,
    val status: TaskStatus,
    val labels: List<String>,
    val checklist: List<ChecklistItem>,
    val createdAt: String?,
    val updatedAt: String?,
)

/** One dashboard snapshot per account, keyed by the cache owner id. */
@Entity(tableName = "dashboard", primaryKeys = ["userId"])
data class DashboardEntity(
    val userId: String,
    val projects: Int,
    val tasks: Int,
    val activity: Int,
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val description: String?,
    val frequency: HabitFrequency,
    val targetDays: List<Int>,
    val completedDates: List<String>,
    val createdAt: String?,
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE userId = :userId")
    fun getProjectsFlow(userId: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE userId = :userId")
    suspend fun getProjects(userId: String): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE userId = :userId AND id = :projectId")
    suspend fun getProjectById(
        userId: String,
        projectId: String,
    ): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<ProjectEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE userId = :userId AND id = :projectId")
    suspend fun deleteProjectById(
        userId: String,
        projectId: String,
    )

    @Query("DELETE FROM projects WHERE userId = :userId")
    suspend fun clearForUser(userId: String)

    @Query("DELETE FROM projects")
    suspend fun clearAll()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE userId = :userId AND projectId = :projectId")
    fun getTasksFlow(
        userId: String,
        projectId: String,
    ): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND projectId = :projectId")
    suspend fun getTasksByProjectId(
        userId: String,
        projectId: String,
    ): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE userId = :userId")
    suspend fun getAllTasks(userId: String): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE userId = :userId AND id = :taskId")
    suspend fun deleteTaskById(
        userId: String,
        taskId: String,
    )

    @Query("DELETE FROM tasks WHERE userId = :userId AND projectId = :projectId")
    suspend fun deleteTasksByProjectId(
        userId: String,
        projectId: String,
    )

    @Query("DELETE FROM tasks WHERE userId = :userId")
    suspend fun clearForUser(userId: String)

    @Query("DELETE FROM tasks")
    suspend fun clearAll()
}

@Dao
interface DashboardDao {
    @Query("SELECT * FROM dashboard WHERE userId = :userId")
    suspend fun getDashboard(userId: String): DashboardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDashboard(dashboard: DashboardEntity)

    @Query("DELETE FROM dashboard WHERE userId = :userId")
    suspend fun clearForUser(userId: String)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE userId = :userId ORDER BY name")
    suspend fun getHabits(userId: String): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(habits: List<HabitEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE userId = :userId AND id = :habitId")
    suspend fun deleteHabit(
        userId: String,
        habitId: String,
    )

    @Query("DELETE FROM habits WHERE userId = :userId")
    suspend fun clearForUser(userId: String)
}

class TaskConverters {
    private val gson = Gson()

    @TypeConverter fun labelsToString(value: List<String>): String = gson.toJson(value)

    @TypeConverter fun stringToLabels(value: String): List<String> = gson.fromJson(value, object : TypeToken<List<String>>() {}.type) ?: emptyList()

    @TypeConverter fun checklistToString(value: List<ChecklistItem>): String = gson.toJson(value)

    @TypeConverter fun stringToChecklist(value: String): List<ChecklistItem> =
        gson.fromJson(value, object : TypeToken<List<ChecklistItem>>() {}.type) ?: emptyList()

    @TypeConverter fun intListToString(value: List<Int>): String = gson.toJson(value)

    @TypeConverter fun stringToIntList(value: String): List<Int> = gson.fromJson(value, object : TypeToken<List<Int>>() {}.type) ?: emptyList()

    @TypeConverter fun frequencyToString(value: HabitFrequency): String = value.name

    @TypeConverter fun stringToFrequency(value: String): HabitFrequency = HabitFrequency.valueOf(value)
}

@TypeConverters(TaskConverters::class)
@Database(
    entities = [ProjectEntity::class, TaskEntity::class, DashboardEntity::class, HabitEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class NexusDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    abstract fun taskDao(): TaskDao

    abstract fun dashboardDao(): DashboardDao

    abstract fun habitDao(): HabitDao

    companion object {
        /**
         * Version 4 scopes every cached row to the account that produced it.
         *
         * Rows written by version 3 or earlier carry no owner and therefore cannot be attributed
         * to an account, so they are discarded rather than risk showing one account another
         * account's data. This database is a read-through cache of server data, so nothing that
         * exists only on the device is lost.
         */
        val MIGRATION_3_4: Migration =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("DELETE FROM projects")
                    db.execSQL("DELETE FROM tasks")
                    db.execSQL("DELETE FROM habits")
                    db.execSQL("DROP TABLE IF EXISTS dashboard")

                    // SQLite cannot add a NOT NULL column without a default, and Room ignores a
                    // database-side default when the entity declares none.
                    db.execSQL("ALTER TABLE projects ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE tasks ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE habits ADD COLUMN userId TEXT NOT NULL DEFAULT ''")

                    // The dashboard row is now keyed by account rather than a fixed id of 1, which
                    // changes the primary key and therefore requires a rebuild of the table.
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `dashboard` (" +
                            "`userId` TEXT NOT NULL, " +
                            "`projects` INTEGER NOT NULL, " +
                            "`tasks` INTEGER NOT NULL, " +
                            "`activity` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`userId`))",
                    )
                }
            }

        /** Every known migration. Add a new entry here for each future version bump. */
        private val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_3_4)

        @Volatile
        private var INSTANCE: NexusDatabase? = null

        fun getDatabase(context: Context): NexusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance =
                    Room.databaseBuilder(
                        context.applicationContext,
                        NexusDatabase::class.java,
                        "nexus_database",
                    )
                        // Deliberately no fallbackToDestructiveMigration(): a version bump without a
                        // matching Migration must fail loudly instead of silently wiping the cache.
                        .addMigrations(*MIGRATIONS)
                        .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

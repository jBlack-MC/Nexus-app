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
import com.example.nexus.api.ChecklistItem
import com.example.nexus.api.TaskPriority
import com.example.nexus.api.TaskStatus
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val createdAt: String?,
    val updatedAt: String?
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
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
    val updatedAt: String?
)

@Entity(tableName = "dashboard")
data class DashboardEntity(
    @PrimaryKey val id: Int = 1,
    val projects: Int,
    val tasks: Int,
    val activity: Int
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects")
    fun getProjectsFlow(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects")
    suspend fun getProjects(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectById(projectId: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<ProjectEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: String)

    @Query("DELETE FROM projects")
    suspend fun clearAll()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE projectId = :projectId")
    fun getTasksFlow(projectId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId")
    suspend fun getTasksByProjectId(projectId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("DELETE FROM tasks WHERE projectId = :projectId")
    suspend fun deleteTasksByProjectId(projectId: String)

    @Query("DELETE FROM tasks")
    suspend fun clearAll()
}

@Dao
interface DashboardDao {
    @Query("SELECT * FROM dashboard WHERE id = 1")
    suspend fun getDashboard(): DashboardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDashboard(dashboard: DashboardEntity)
}

class TaskConverters {
    private val gson = Gson()

    @TypeConverter fun labelsToString(value: List<String>): String = gson.toJson(value)
    @TypeConverter fun stringToLabels(value: String): List<String> =
        gson.fromJson(value, object : TypeToken<List<String>>() {}.type) ?: emptyList()
    @TypeConverter fun checklistToString(value: List<ChecklistItem>): String = gson.toJson(value)
    @TypeConverter fun stringToChecklist(value: String): List<ChecklistItem> =
        gson.fromJson(value, object : TypeToken<List<ChecklistItem>>() {}.type) ?: emptyList()
}

@TypeConverters(TaskConverters::class)
@Database(entities = [ProjectEntity::class, TaskEntity::class, DashboardEntity::class], version = 2, exportSchema = false)
abstract class NexusDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun taskDao(): TaskDao
    abstract fun dashboardDao(): DashboardDao

    companion object {
        @Volatile
        private var INSTANCE: NexusDatabase? = null

        fun getDatabase(context: Context): NexusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NexusDatabase::class.java,
                    "nexus_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

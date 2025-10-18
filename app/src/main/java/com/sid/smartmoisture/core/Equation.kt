package com.sid.smartmoisture.core

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update

@Entity("equations")
data class Equation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val formula: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface EquationDao {
    @Query("SELECT * FROM equations ORDER BY createdAt DESC")
    suspend fun all(): List<Equation>

    @Insert
    suspend fun insert(equation: Equation): Long

    @Update
    suspend fun update(equation: Equation)

    @Delete
    suspend fun delete(equation: Equation)
}

class EquationRepo(private val dao: EquationDao) {
    suspend fun list() = dao.all()
    suspend fun add(name: String, formula: String) =
        dao.insert(Equation(name = name, formula = formula))

    suspend fun update(equation: Equation) = dao.update(equation)
    suspend fun delete(equation: Equation) = dao.delete(equation)
}

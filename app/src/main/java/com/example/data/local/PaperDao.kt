package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PaperDao {
    @Query("SELECT * FROM papers WHERE folderUriString = :folderUri ORDER BY updatedAt DESC")
    fun getPapersByFolder(folderUri: String): Flow<List<PaperEntity>>

    @Query("SELECT * FROM papers ORDER BY updatedAt DESC")
    fun getAllPapers(): Flow<List<PaperEntity>>

    @Query("SELECT * FROM papers WHERE id = :id")
    suspend fun getPaperById(id: Long): PaperEntity?

    @Query("SELECT * FROM papers WHERE uriString = :uriString LIMIT 1")
    suspend fun getPaperByUri(uriString: String): PaperEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaper(paper: PaperEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPapers(papers: List<PaperEntity>)

    @Update
    suspend fun updatePaper(paper: PaperEntity)

    @Query("DELETE FROM papers WHERE id = :id")
    suspend fun deletePaperById(id: Long)

    @Query("DELETE FROM papers WHERE folderUriString = :folderUri")
    suspend fun deletePapersByFolder(folderUri: String)

    @Query("DELETE FROM papers")
    suspend fun deleteAll()
}

package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "vault_media")
data class VaultMediaEntity(
  @PrimaryKey val id: String,
  val originalMediaId: String?,
  val title: String,
  val originalFileName: String,
  val encryptedFileName: String,
  val fileSizeBytes: Long,
  val mediaType: String = "PHOTO", // "PHOTO" or "VIDEO"
  val mimeType: String = "image/jpeg",
  val width: Int = 0,
  val height: Int = 0,
  val durationMs: Long = 0L,
  val dateAddedMillis: Long = System.currentTimeMillis(),
  val albumId: String = "personal",
  val isFavorite: Boolean = false,
  val ivBase64: String,
  val originalUriBeforeMove: String? = null
)

@Entity(tableName = "vault_albums")
data class VaultAlbumEntity(
  @PrimaryKey val id: String,
  val name: String,
  val createdAtMillis: Long = System.currentTimeMillis(),
  val coverMediaId: String? = null
)

@Dao
interface VaultDao {
  @Query("SELECT * FROM vault_media ORDER BY dateAddedMillis DESC")
  fun getAllVaultMediaFlow(): Flow<List<VaultMediaEntity>>

  @Query("SELECT * FROM vault_media ORDER BY dateAddedMillis DESC")
  suspend fun getAllVaultMedia(): List<VaultMediaEntity>

  @Query("SELECT * FROM vault_media WHERE id = :id LIMIT 1")
  suspend fun getVaultMediaById(id: String): VaultMediaEntity?

  @Query("SELECT * FROM vault_media WHERE albumId = :albumId ORDER BY dateAddedMillis DESC")
  fun getMediaByAlbumFlow(albumId: String): Flow<List<VaultMediaEntity>>

  @Query("SELECT * FROM vault_media WHERE isFavorite = 1 ORDER BY dateAddedMillis DESC")
  fun getFavoritesFlow(): Flow<List<VaultMediaEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertVaultMedia(media: VaultMediaEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertVaultMediaList(mediaList: List<VaultMediaEntity>)

  @Update
  suspend fun updateVaultMedia(media: VaultMediaEntity)

  @Query("DELETE FROM vault_media WHERE id = :id")
  suspend fun deleteVaultMediaById(id: String)

  @Query("DELETE FROM vault_media")
  suspend fun clearAllVaultMedia()

  // Albums
  @Query("SELECT * FROM vault_albums ORDER BY createdAtMillis ASC")
  fun getAllVaultAlbumsFlow(): Flow<List<VaultAlbumEntity>>

  @Query("SELECT * FROM vault_albums ORDER BY createdAtMillis ASC")
  suspend fun getAllVaultAlbums(): List<VaultAlbumEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertVaultAlbum(album: VaultAlbumEntity)

  @Query("UPDATE vault_albums SET name = :newName WHERE id = :id")
  suspend fun renameVaultAlbum(id: String, newName: String)

  @Query("DELETE FROM vault_albums WHERE id = :id")
  suspend fun deleteVaultAlbum(id: String)

  @Query("UPDATE vault_media SET albumId = 'personal' WHERE albumId = :deletedAlbumId")
  suspend fun resetAlbumMediaToPersonal(deletedAlbumId: String)
}

package com.nkming.clipboard.model.room

import android.arch.paging.DataSource
import android.arch.persistence.room.*
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import io.reactivex.Flowable

@Dao
abstract class ClipDao
{
	@Transaction
	@Query("SELECT * FROM ${ClipMeta.TABLE} "
			+ "ORDER BY ${ClipMeta.COL_CREATE_AT} DESC")
	abstract fun loadAllClips(): DataSource.Factory<Int, Clip>

	@Transaction
	@Query("SELECT * FROM ${ClipMeta.TABLE} "
			+ "WHERE ${ClipMeta.TABLE}.${ClipMeta.COL_CREATE_AT} = :createAt "
			+ "LIMIT 1")
	abstract fun loadClipByCreateAt(createAt: Long): Clip

	@Transaction
	@Query("SELECT * FROM ${ClipMeta.TABLE} "
			+ "ORDER BY ${ClipMeta.TABLE}.${ClipMeta.COL_CREATE_AT} DESC "
			+ "LIMIT :limit OFFSET :offset")
	abstract fun loadClipByLatest(limit: Int = 1, offset: Int = 0): List<Clip>

	@Transaction
	@Query("SELECT * FROM ${ClipMeta.TABLE} "
			+ "ORDER BY ${ClipMeta.TABLE}.${ClipMeta.COL_CREATE_AT} DESC "
			+ "LIMIT :limit OFFSET :offset")
	abstract fun observeClipByLatest(limit: Int = 1, offset: Int = 0): Flowable<Clip>

	@Transaction
	open fun insertClips(vararg clips: Clip)
	{
		val ids = _insertClipMetas(*clips.map{it.meta}.toTypedArray())
		for ((c, id) in clips.zip(ids))
		{
			for (m in c.mimes)
			{
				m.clipId = id
			}
			_insertClipMimes(*c.mimes.toTypedArray())
			for (i in c.items)
			{
				i.clipId = id
			}
			_insertClipItems(*c.items.toTypedArray())
		}
	}

	@Insert(onConflict = REPLACE)
	abstract fun _insertClipMetas(vararg metas: ClipMeta): List<Long>

	@Insert(onConflict = REPLACE)
	abstract fun _insertClipMimes(vararg mimes: ClipMime)

	@Insert(onConflict = REPLACE)
	abstract fun _insertClipItems(vararg items: ClipItem)

	@Transaction
	open fun updateClips(vararg clips: Clip)
	{
		_updateClipMetas(*clips.map{it.meta}.toTypedArray())
		for (c in clips)
		{
			_updateClipMimes(*c.mimes.toTypedArray())
			_updateClipItems(*c.items.toTypedArray())
		}
	}

	@Update
	abstract fun _updateClipMetas(vararg metas: ClipMeta)

	@Update
	abstract fun _updateClipMimes(vararg mimes: ClipMime)

	@Update
	abstract fun _updateClipItems(vararg items: ClipItem)

	@Transaction
	open fun deleteClips(vararg clips: Clip)
	{
		for (c in clips)
		{
			_deleteClipMimes(*c.mimes.toTypedArray())
			_deleteClipItems(*c.items.toTypedArray())
		}
		_deleteClipMetas(*clips.map{it.meta}.toTypedArray())
	}

	@Delete
	abstract fun _deleteClipMetas(vararg metas: ClipMeta)

	@Delete
	abstract fun _deleteClipMimes(vararg mimes: ClipMime)

	@Delete
	abstract fun _deleteClipItems(vararg items: ClipItem)

	@Transaction
	open fun nukeClips()
	{
		_nukeClipMimes()
		_nukeClipItems()
		_nukeClipMetas()
	}

	@Query("DELETE FROM ${ClipMime.TABLE}")
	abstract fun _nukeClipMimes()

	@Query("DELETE FROM ${ClipItem.TABLE}")
	abstract fun _nukeClipItems()

	@Query("DELETE FROM ${ClipMeta.TABLE}")
	abstract fun _nukeClipMetas()
}

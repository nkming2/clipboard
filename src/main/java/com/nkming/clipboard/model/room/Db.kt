package com.nkming.clipboard.model.room

import android.arch.lifecycle.LiveData
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import com.nkming.clipboard.ClipboardApp
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory

@Database(entities = [ClipItem::class, ClipMeta::class, ClipMime::class],
		version = 1)
abstract class Db : RoomDatabase()
{
	companion object
	{
		fun loadAllClips(): LiveData<PagedList<Clip>>
		{
			val config = PagedList.Config.Builder()
					.setPageSize(60)
					.build()
			return LivePagedListBuilder(instance.clipDao().loadAllClips(),
					config).build()
		}

		fun loadClipByCreateAt(createAt: Long, onNext: (Clip) -> Unit = {},
				onError: (Throwable) -> Unit = {throw it}): Disposable
		{
			return Single.fromCallable{instance.clipDao().loadClipByCreateAt(
							createAt)}
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(onNext, onError)
		}

		fun loadClipByLatest(limit: Int = 1, offset: Int = 0,
				onNext: (List<Clip>) -> Unit = {},
				onError: (Throwable) -> Unit = {throw it}): Disposable
		{
			return Single.fromCallable{instance.clipDao().loadClipByLatest(
							limit = limit, offset = offset)}
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(onNext, onError)
		}

		fun observeClipByLatest(limit: Int = 1, offset: Int = 0,
				onNext: (Clip) -> Unit = {},
				onError: (Throwable) -> Unit = {throw it}): Disposable
		{
			return instance.clipDao().observeClipByLatest(limit = limit,
							offset = offset)
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(onNext, onError)
		}

		fun insertClips(vararg clips: Clip, onNext: () -> Unit = {},
				onError: (Throwable) -> Unit = {throw it}): Disposable
		{
			return Completable.fromCallable{
							instance.clipDao().insertClips(*clips)}
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(onNext, onError)
		}

		fun updateClips(vararg clips: Clip, onNext: () -> Unit = {},
				onError: (Throwable) -> Unit = {throw it}): Disposable
		{
			return Completable.fromCallable{
							instance.clipDao().updateClips(*clips)}
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(onNext, onError)
		}

		fun deleteClips(vararg clips: Clip, onNext: () -> Unit = {},
				onError: (Throwable) -> Unit = {throw it}): Disposable
		{
			return Completable.fromCallable{
							instance.clipDao().deleteClips(*clips)}
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(onNext, onError)
		}

		fun nukeClips(onNext: () -> Unit = {},
				onError: (Throwable) -> Unit = {throw it}): Disposable
		{
			return Completable.fromCallable{instance.clipDao().nukeClips()}
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(onNext, onError)
		}

		fun transaction(run: () -> Unit,onNext: () -> Unit = {},
				onError: (Throwable) -> Unit = {throw it}): Disposable
		{
			return Completable.fromCallable{
						instance.runInTransaction(run)
					}
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(onNext, onError)
		}

		val instance by lazy{
			Room.databaseBuilder(ClipboardApp.context, Db::class.java, "db")
					.openHelperFactory(RequerySQLiteOpenHelperFactory())
					.build()
		}
	}

	abstract fun clipDao(): ClipDao
}

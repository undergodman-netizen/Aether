package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.example.data.AppDatabase
import com.example.data.VideoRepository
import com.example.media.PlayerManager
import com.example.viewmodel.PlayerViewModel
import com.example.viewmodel.VideoViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

val appModule = module {
    single { AppDatabase.getDatabase(androidContext()) }
    single { get<AppDatabase>().videoDao() }
    single { get<AppDatabase>().pdfDao() }
    single { get<AppDatabase>().videoShotDao() }
    single { get<AppDatabase>().pdfAnnotationDao() }
    single { get<AppDatabase>().videoNoteDao() }
    single { VideoRepository(get(), get(), get(), get()) }
    factory { PlayerManager(androidContext()) }
    viewModel { VideoViewModel(get()) }
    viewModel { PlayerViewModel(get(), get()) }
}

class AetherApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@AetherApplication)
            modules(appModule)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }
}

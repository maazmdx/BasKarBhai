package com.baskarbhai.roast

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.baskarbhai.roast.data.PreferencesManager

/**
 * BasKarBhai Application class.
 * Configures Coil with animated GIF decoding support and initializes preferences.
 * Engineered by maazmdx.
 */
class BasKarBhaiApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        PreferencesManager.init(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // 50MB disk cache for memes
                    .build()
            }
            .crossfade(true)
            .build()
    }
}

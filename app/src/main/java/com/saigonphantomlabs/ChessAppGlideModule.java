package com.saigonphantomlabs;

import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

/**
 * Khai báo AppGlideModule rỗng để Glide sinh {@code GeneratedAppGlideModule} — dứt warning
 * "Failed to find GeneratedAppGlideModule" lúc khởi động và kích hoạt annotationProcessor
 * (glide:compiler đã khai báo trong build.gradle). Không cấu hình gì thêm → giữ mặc định.
 */
@GlideModule
public final class ChessAppGlideModule extends AppGlideModule {
    // Mặc định: không override applyOptions/registerComponents.
}

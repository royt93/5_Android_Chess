package com.saigonphantomlabs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * REGRESSION leak-guard cho fix: callback truyền vào {@code AdManager.initSplashScreen} /
 * {@code requestConsentInfoUpdate} bị lib giữ trong static {@code splashTimeoutRunnable} →
 * nếu là anonymous/inner class sẽ ngầm giữ {@code SplashActivity.this} → leak Activity (~918KB).
 *
 * Fix: callback PHẢI là static nested class + WeakReference. Test này khoá hợp đồng đó:
 * nếu ai đổi lại anonymous/inner (non-static, có synthetic {@code this$0}) → fail ngay.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class SplashCallbackLeakGuardTest {

    private Class<?> nested(String simpleName) {
        for (Class<?> c : SplashActivity.class.getDeclaredClasses()) {
            if (c.getSimpleName().equals(simpleName)) return c;
        }
        return null;
    }

    private void assertStaticWeakRefCallback(String simpleName) {
        Class<?> clazz = nested(simpleName);
        assertNotNull("Thiếu callback class " + simpleName + " (đừng đổi về anonymous/lambda)", clazz);

        // 1) PHẢI static (anonymous/inner = non-static)
        assertTrue(simpleName + " phải là static nested class",
                Modifier.isStatic(clazz.getModifiers()));

        // 2) KHÔNG có synthetic outer ref this$0 (inner class mới có)
        for (Field f : clazz.getDeclaredFields()) {
            assertFalse(simpleName + " không được giữ outer this$0 (leak Activity)",
                    f.getName().startsWith("this$"));
        }

        // 3) PHẢI giữ Activity bằng WeakReference
        boolean hasWeakRef = false;
        for (Field f : clazz.getDeclaredFields()) {
            if (WeakReference.class.isAssignableFrom(f.getType())) { hasWeakRef = true; break; }
        }
        assertTrue(simpleName + " phải giữ SplashActivity bằng WeakReference", hasWeakRef);
    }

    @Test
    public void splashNavCallback_isStaticWeakRef() {
        assertStaticWeakRefCallback("SplashNavCallback");
    }

    @Test
    public void consentCallback_isStaticWeakRef() {
        assertStaticWeakRefCallback("ConsentCallback");
    }
}

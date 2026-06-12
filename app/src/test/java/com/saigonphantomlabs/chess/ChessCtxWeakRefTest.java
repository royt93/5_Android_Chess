package com.saigonphantomlabs.chess;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.ContextWrapper;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;

/**
 * Test hợp đồng {@link Chess#getCtx()} / {@link Chess#setCtx(Context)} sau khi đổi
 * {@code ctx} sang {@link WeakReference} (chống leak Activity).
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class ChessCtxWeakRefTest {

    private Context appCtx() {
        return ApplicationProvider.getApplicationContext();
    }

    @Test
    public void modelMode_noArgCtor_getCtxIsNull() {
        // new Chess() = model-only (unit test) → không có ctx
        assertNull(new Chess().getCtx());
    }

    @Test
    public void setCtx_thenGetCtx_returnsSame() {
        Chess c = new Chess();
        Context ctx = appCtx();
        c.setCtx(ctx);
        assertSame(ctx, c.getCtx());
    }

    @Test
    public void setCtx_null_returnsNull() {
        Chess c = new Chess();
        c.setCtx(appCtx());
        c.setCtx(null);
        assertNull(c.getCtx());
    }

    @Test
    public void ctxField_isWeakReference() throws Exception {
        // Reflection guard: field giữ ctx PHẢI là WeakReference (nếu đổi lại strong ref → fail).
        Field f = Chess.class.getDeclaredField("ctxRef");
        assertTrue("ctxRef phải là WeakReference",
                WeakReference.class.isAssignableFrom(f.getType()));
    }

    @Test
    public void weakRef_clearsAfterGc_noLeak() {
        Chess c = new Chess();
        // Context throwaway (ContextWrapper) — drop strong ref rồi GC → WeakRef phải clear.
        Context throwaway = new ContextWrapper(appCtx());
        c.setCtx(throwaway);
        assertSame(throwaway, c.getCtx());
        throwaway = null; // bỏ strong ref

        boolean cleared = false;
        for (int i = 0; i < 20 && !cleared; i++) {
            System.gc();
            Runtime.getRuntime().runFinalization();
            try { Thread.sleep(20); } catch (InterruptedException ignored) { }
            cleared = (c.getCtx() == null);
        }
        assertTrue("WeakReference ctx phải được GC thu hồi (chống leak Activity)", cleared);
    }

    @Test
    public void undoEmptyHistory_ctxNull_noCrash() {
        // Path đã thêm null-guard: undo khi rỗng + ctx null không được NPE (model mode).
        Chess c = new Chess();
        c.setBoardViewForTest(new NoOpChessBoardView());
        c.undoLastMove(); // không ném exception
    }
}

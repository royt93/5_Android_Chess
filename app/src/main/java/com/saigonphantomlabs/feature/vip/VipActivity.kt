package com.saigonphantomlabs.feature.vip

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.saigonphantomlabs.BaseActivity
import com.saigonphantomlabs.chess.R
import com.saigonphantomlabs.chess.databinding.AVipManagementBinding
import com.saigonphantomlabs.common.consts.AdKeys
import com.roy.sdkadbmob.AdManager
import com.roy.sdkadbmob.UIUtils
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Màn quản lý VIP — host bằng Activity (project activity-based, không Navigation Component).
 *
 * Tuân memory-rule §10.5 của doc/AD_PROMPT_AOS.MD:
 * - Countdown bằng [CountDownTimer], cancel ở [onDestroy] (KHÔNG Handler.postDelayed).
 * - Mọi animator nullable + cancel ở [onDestroy].
 * - Binding nullable, guard `_binding == null` trong callback async (rewarded).
 */
class VipActivity : BaseActivity() {

    private var _binding: AVipManagementBinding? = null
    private val binding get() = _binding!!

    private lateinit var vipPrefs: VipPrefs

    // VIP timeline hiện tại (set khi bindUi với state active)
    private var grantedAtMs = 0L
    private var expiresAtMs = 0L
    private var lastMinuteShown = -1

    // Animators / timer — tất cả cancel ở onDestroy
    private var countDownTimer: CountDownTimer? = null
    private var pulseAnimator: ObjectAnimator? = null
    private var countUpAnimator: ValueAnimator? = null

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UIUtils.setupEdgeToEdge1(window)
        _binding = AVipManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UIUtils.setupEdgeToEdge2(binding.rootLayout, true, true)

        vipPrefs = VipPrefs(this)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnActivate.setOnClickListener { onActivateKeyClicked() }
        binding.btnWatchAd.setOnClickListener { onWatchAdClicked() }
        binding.btnRevoke.setOnClickListener { confirmRevoke() }
        binding.btnRevokeAll.setOnClickListener { confirmRevoke() }
        binding.tvPrivacy.setOnClickListener { openPrivacyPolicy() }
        // GDPR re-consent (EEA) — lib chỉ show form nếu privacyOptions == REQUIRED, else no-op.
        binding.tvPrivacyOptions.setOnClickListener { AdManager.showConsentFormIfAvailable(this) }

        playEntryAnimation()
    }

    override fun onResume() {
        super.onResume()
        // Preload rewarded cho nút "Xem QC → 3 ngày VIP"
        AdManager.loadRewarded(applicationContext)
        bindUi()
        binding.shimmerCrown.startShimmer()            // anim #3 crown shimmer
        // anim #1 pulse — chỉ chạy khi nút watch-ad đang hiển thị (user chưa VIP)
        if (binding.btnWatchAd.visibility == View.VISIBLE) startPulse()
    }

    override fun onPause() {
        _binding?.shimmerCrown?.stopShimmer()
        pulseAnimator?.cancel()
        super.onPause()
    }

    // ───────────────────────── UI binding ─────────────────────────

    private fun bindUi() {
        val b = _binding ?: return
        val active = AdManager.isVipByKeyActive()

        if (active) {
            expiresAtMs = AdManager.getVipByKeyExpiry()
            val saved = vipPrefs.getGrantedAtMs()
            // Grace entry (auto-trial 1 ngày của SDK) không lưu grantedAt ở app →
            // fallback granted = expiry - 24h để vẽ progress đúng cửa sổ grace.
            grantedAtMs = if (saved in 1 until expiresAtMs) saved else (expiresAtMs - VipMath.ONE_DAY_MS)

            val isGrace = !vipPrefs.userRedeemedAtLeastOnce()

            b.headerBg.setBackgroundResource(R.drawable.bg_vip_status_header_active)
            b.tvStatusTitle.setText(R.string.vip_active)
            b.tvStatusSubtitle.text = getString(R.string.vip_until, dateFormat.format(Date(expiresAtMs)))

            b.tvActivatedAt.visibility = View.VISIBLE
            b.tvActivatedAt.text = getString(R.string.vip_activated_at, dateFormat.format(Date(grantedAtMs)))
            b.tvExpiresAt.visibility = View.VISIBLE
            b.tvExpiresAt.text = getString(R.string.vip_expires_at, dateFormat.format(Date(expiresAtMs)))
            b.progressVip.visibility = View.VISIBLE
            b.tvCountdown.visibility = View.VISIBLE

            // Active VIP card (single-entry)
            b.cardActiveVip.visibility = View.VISIBLE
            b.tvEntryLabel.text = if (isGrace) {
                getString(R.string.vip_entry_first_install)
            } else {
                val days = ((expiresAtMs - grantedAtMs).toDouble() / VipMath.ONE_DAY_MS).toInt().coerceAtLeast(1)
                getString(R.string.vip_entry_redeemed, days)
            }
            val daysLeft = VipMath.daysLeftCeil(expiresAtMs, System.currentTimeMillis())
            b.tvEntryRemaining.text = getString(R.string.vip_days_left, daysLeft)

            // Đã VIP → ẩn acquire-controls (nhập key / xem QC) để tránh activate ghi đè
            // làm RÚT NGẮN thời hạn (lib set expiry = now + days, không cộng dồn).
            setAcquireControlsVisible(false)

            b.btnRevoke.isEnabled = true
            b.btnRevokeAll.isEnabled = true

            startCountdown()
        } else {
            cancelCountdown()
            b.headerBg.setBackgroundResource(R.drawable.bg_vip_status_header_free)
            b.tvStatusTitle.setText(R.string.vip_free_user)
            b.tvStatusSubtitle.text = ""
            b.tvActivatedAt.visibility = View.GONE
            b.tvExpiresAt.visibility = View.GONE
            b.progressVip.visibility = View.GONE
            b.tvCountdown.visibility = View.GONE
            b.cardActiveVip.visibility = View.GONE
            setAcquireControlsVisible(true)
            b.btnRevoke.isEnabled = false
            b.btnRevokeAll.isEnabled = false
        }
    }

    /** Ẩn/hiện nhóm "acquire VIP" (nhập key + xem QC). Ẩn khi user đã VIP. */
    private fun setAcquireControlsVisible(visible: Boolean) {
        val b = _binding ?: return
        val vis = if (visible) View.VISIBLE else View.GONE
        b.tilKey.visibility = vis
        b.btnActivate.visibility = vis
        b.btnWatchAd.visibility = vis
        if (!visible) pulseAnimator?.cancel()
    }

    // ───────────────────────── Countdown + progress ─────────────────────────

    private fun startCountdown() {
        cancelCountdown()
        val remaining = expiresAtMs - System.currentTimeMillis()
        if (remaining <= 0L) {
            bindUi()
            return
        }
        lastMinuteShown = -1
        countDownTimer = object : CountDownTimer(remaining, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val b = _binding ?: return
                b.tvCountdown.text = formatRemaining(millisUntilFinished)
                b.progressVip.setProgressCompat(
                    VipMath.elapsedProgress(grantedAtMs, expiresAtMs, System.currentTimeMillis()),
                    true,
                )
                // anim #4: count-up bump khi sang phút mới (không chạy mỗi giây → tránh hot loop).
                // Tick đầu chỉ khởi tạo mốc, KHÔNG bump (tránh giật khi vừa mở màn).
                val currentMinute = (millisUntilFinished / 60_000L).toInt()
                if (lastMinuteShown == -1) {
                    lastMinuteShown = currentMinute
                } else if (currentMinute != lastMinuteShown) {
                    lastMinuteShown = currentMinute
                    bumpCountdown()
                }
            }

            override fun onFinish() {
                bindUi()
            }
        }.start()
    }

    private fun cancelCountdown() {
        countDownTimer?.cancel()
        countDownTimer = null
    }

    private fun formatRemaining(ms: Long): String {
        val r = VipMath.remaining(ms)
        return getString(R.string.vip_remaining, r.days, r.hours, r.minutes, r.seconds)
    }

    // ───────────────────────── Actions ─────────────────────────

    private fun onActivateKeyClicked() {
        val input = binding.etKey.text?.toString()?.trim().orEmpty()
        // VipKeys = whitelist riêng của app (key → số ngày). Validate cục bộ ở đây.
        val days = VipKeys.lookupDays(input)
        if (days == null) {
            Toast.makeText(this, R.string.vip_key_invalid, Toast.LENGTH_SHORT).show()
            return
        }
        applyVip(days, R.string.vip_key_invalid)
    }

    /**
     * Activate VIP cho [days] ngày.
     *
     * ⚠️ Lib là **single-secret**: [AdManager.activateVipByKey] chỉ chấp nhận key == [AdKeys.VIP_SECRET]
     * (số ngày do caller truyền). Vì vậy app validate key của mình qua [VipKeys] (whitelist nhiều
     * key → nhiều mốc ngày), rồi LUÔN grant bằng [AdKeys.VIP_SECRET] + [days]. KHÔNG truyền key
     * người dùng gõ thẳng vào lib — sẽ fail nếu khác secret (vd key 3-ngày ≠ secret 30-ngày).
     *
     * Extend-guard: chỉ apply khi kéo dài thêm (lib ghi đè expiry = now + days, không cộng dồn).
     */
    private fun applyVip(days: Int, failMsgRes: Int) {
        val newExpiry = System.currentTimeMillis() + days * VipMath.ONE_DAY_MS
        if (!VipMath.isExtension(AdManager.isVipByKeyActive(), AdManager.getVipByKeyExpiry(), newExpiry)) {
            Toast.makeText(this, R.string.vip_already_longer, Toast.LENGTH_SHORT).show()
            return
        }
        if (AdManager.activateVipByKey(this, AdKeys.VIP_SECRET, days)) {
            onVipGranted(days)
        } else {
            Toast.makeText(this, failMsgRes, Toast.LENGTH_SHORT).show()
        }
    }

    private fun onWatchAdClicked() {
        AdManager.showRewarded(this) { earned ->
            if (_binding == null || isDestroyed) return@showRewarded
            if (earned) {
                grantViaRewarded()
            } else {
                // Fallback: rewarded chưa sẵn sàng → thử interstitial rồi vẫn grant 3 ngày.
                AdManager.showInterstitial(this) { _ ->
                    if (_binding == null || isDestroyed) return@showInterstitial
                    grantViaRewarded()
                }
            }
        }
    }

    /** Grant 3 ngày VIP sau khi xem rewarded (grant bằng VIP_SECRET + 3 ngày — xem [applyVip]). */
    private fun grantViaRewarded() {
        applyVip(3, R.string.vip_reward_failed)
    }

    private fun onVipGranted(days: Int) {
        vipPrefs.saveGrantedAtMs(System.currentTimeMillis())
        vipPrefs.markUserRedeemed()
        binding.etKey.text?.clear()
        Toast.makeText(this, getString(R.string.vip_activated_toast, days), Toast.LENGTH_SHORT).show()
        celebrate()      // anim #5 confetti + haptic
        bindUi()
    }

    private fun confirmRevoke() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.vip_revoke_confirm_title)
            .setMessage(R.string.vip_revoke_confirm_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.confirm) { _, _ ->
                AdManager.clearVipByKey()
                vipPrefs.clearGrantedAtMs()
                bindUi()
            }
            .show()
    }

    private fun openPrivacyPolicy() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AdKeys.PRIVACY_POLICY_URL)))
        } catch (_: Exception) {
            Toast.makeText(this, R.string.vip_reward_failed, Toast.LENGTH_SHORT).show()
        }
    }

    // ───────────────────────── Animations ─────────────────────────

    /** anim #2: slide-in từ dưới khi mở màn. */
    private fun playEntryAnimation() {
        binding.contentContainer.apply {
            alpha = 0f
            translationY = 120f
            animate().alpha(1f).translationY(0f).setDuration(450L).start()
        }
    }

    /** anim #1: pulse nút "Xem QC". */
    private fun startPulse() {
        pulseAnimator?.cancel()
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
            binding.btnWatchAd,
            android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.05f),
            android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.05f),
        ).apply {
            duration = 1600L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }

    /** anim #4: bump nhẹ countdown khi sang phút mới. */
    private fun bumpCountdown() {
        val b = _binding ?: return
        countUpAnimator?.cancel()
        countUpAnimator = ValueAnimator.ofFloat(1.15f, 1.0f).apply {
            duration = 400L
            addUpdateListener { va ->
                val v = va.animatedValue as Float
                _binding?.tvCountdown?.scaleX = v
                _binding?.tvCountdown?.scaleY = v
            }
            start()
        }
        b.tvCountdown.scaleX = 1.15f
        b.tvCountdown.scaleY = 1.15f
    }

    /** anim #5: confetti + haptic khi activate thành công. */
    private fun celebrate() {
        val party = Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            colors = listOf(0xFCE18A.toInt(), 0xFF726D.toInt(), 0xF4306D.toInt(), 0xB48DEF.toInt()),
            position = Position.Relative(0.5, 0.3),
            emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(120),
        )
        binding.viewKonfetti.start(party)
        performHaptic()
    }

    private fun performHaptic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            binding.root.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            @Suppress("DEPRECATION")
            val vib = getSystemService(VIBRATOR_SERVICE) as? Vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(50L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(50L)
            }
        }
    }

    override fun onDestroy() {
        cancelCountdown()
        pulseAnimator?.cancel(); pulseAnimator = null
        countUpAnimator?.cancel(); countUpAnimator?.removeAllUpdateListeners(); countUpAnimator = null
        _binding?.shimmerCrown?.stopShimmer()
        _binding = null
        super.onDestroy()
    }
}

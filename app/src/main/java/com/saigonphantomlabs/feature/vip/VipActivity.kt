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
import com.google.android.ump.ConsentInformation
import com.google.android.ump.UserMessagingPlatform
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

    // Cờ: rewarded đang show/dở. onResume KHÔNG được reload rewarded lúc này — reload sẽ
    // hủy instance ad vừa xem TRƯỚC KHI onAdHidden fire (nơi SDK trả reward) → mất grant.
    private var rewardedInFlight = false

    // Animators / timer — tất cả cancel ở onDestroy
    private var countDownTimer: CountDownTimer? = null
    private var pulseAnimator: ObjectAnimator? = null
    private var countUpAnimator: ValueAnimator? = null
    private var activateAnimator: ObjectAnimator? = null

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UIUtils.setupEdgeToEdge1(window)
        _binding = AVipManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UIUtils.setupEdgeToEdge2(binding.rootLayout, true, true)

        // applicationContext: grant VIP có thể xảy ra khi activity đã bị recreate
        // (rewarded callback) → prefs phải gắn app context, không phải instance này.
        vipPrefs = VipPrefs(applicationContext)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnActivate.setOnClickListener { onActivateKeyClicked() }
        // Nút Activate chỉ enable khi user đã nhập key (tránh bấm rỗng). Pop nhẹ khi
        // chuyển disabled→enabled để thu hút chú ý.
        binding.etKey.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) = updateActivateEnabled()
        })
        updateActivateEnabled()
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
        // Preload rewarded cho nút "Xem QC → 3 ngày VIP".
        // ⚠️ KHÔNG reload khi rewarded đang dở (vừa xem xong, đang dismiss) — reload hủy ad cũ
        // trước onAdHidden → mất reward. Reload chỉ khi an toàn.
        if (!rewardedInFlight) AdManager.loadRewarded(applicationContext)
        bindUi()
        bindPrivacyOptions()                           // điểm 2: Ad Choices chỉ hiện khi EEA
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

            // Card gom activation + expiry + progress + countdown (chỉ hiện khi active)
            b.cardStatusDetail.visibility = View.VISIBLE
            b.progressVip.visibility = View.VISIBLE
            b.tvCountdown.visibility = View.VISIBLE
            b.tvActivatedAt.text = getString(R.string.vip_activated_at, dateFormat.format(Date(grantedAtMs)))
            b.tvExpiresAt.text = getString(R.string.vip_expires_at, dateFormat.format(Date(expiresAtMs)))

            // Active VIP card (single-entry)
            b.cardActiveVip.visibility = View.VISIBLE
            b.tvEntryLabel.text = if (isGrace) {
                getString(R.string.vip_entry_first_install)
            } else {
                // round (không floor): expiry-granted ≈ 3.0 ngày nhưng lệch vài ms (giữa
                // activate và saveGrantedAt) → floor ra 2. Round cho đúng số ngày đã grant.
                val days = Math.round((expiresAtMs - grantedAtMs).toDouble() / VipMath.ONE_DAY_MS)
                    .toInt().coerceAtLeast(1)
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
            b.cardStatusDetail.visibility = View.GONE
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
        // Ẩn/hiện cả section "Get VIP" (header + card) lẫn từng control bên trong
        // (control vẫn toggle riêng để onResume biết có nên chạy pulse hay không).
        b.tvSectionGet.visibility = vis
        b.cardGetVip.visibility = vis
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

    /** Bật/tắt nút Activate theo việc field key có nội dung; pop khi vừa bật. */
    private fun updateActivateEnabled() {
        val b = _binding ?: return
        val enable = !b.etKey.text.isNullOrBlank()
        val wasEnabled = b.btnActivate.isEnabled
        b.btnActivate.isEnabled = enable
        if (enable && !wasEnabled) popActivate()
    }

    /** Pop scale 1 lần khi Activate chuyển sang enabled (an toàn clip: max 1.04 < padding). */
    private fun popActivate() {
        val b = _binding ?: return
        activateAnimator?.cancel()
        activateAnimator = ObjectAnimator.ofPropertyValuesHolder(
            b.btnActivate,
            android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 0.96f, 1.04f, 1.0f),
            android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.96f, 1.04f, 1.0f),
        ).apply { duration = 320L; start() }
    }

    private fun onActivateKeyClicked() {
        val input = binding.etKey.text?.toString()?.trim().orEmpty()
        // VipKeys = whitelist riêng của app (key → số ngày). Validate cục bộ ở đây.
        val days = VipKeys.lookupDays(input)
        if (days == null) {
            showVipFailedDialog(R.string.vip_key_invalid)
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
            if (_binding != null && !isFinishing) {
                Toast.makeText(this, R.string.vip_already_longer, Toast.LENGTH_SHORT).show()
            }
            return
        }
        // GRANT = DỮ LIỆU: dùng applicationContext + persist NGAY, không phụ thuộc activity còn sống.
        // (rewarded callback có thể fire sau khi VipActivity bị recreate do memory/fullscreen-ad
        // churn → nếu gate theo _binding thì grant bị nuốt — đây chính là bug "watch ad không work".)
        if (AdManager.activateVipByKey(applicationContext, AdKeys.VIP_SECRET, days)) {
            vipPrefs.saveGrantedAtMs(System.currentTimeMillis())
            vipPrefs.markUserRedeemed()
            // UI (refresh + confetti + dialog) chỉ chạy nếu activity còn sống; nếu không,
            // lần onResume kế tiếp sẽ bindUi() và hiển thị trạng thái active.
            if (_binding != null && !isFinishing) {
                binding.etKey.text?.clear()
                bindUi()
                showVipSuccessDialog(days)   // Material You dialog + confetti + haptic
            }
        } else {
            if (_binding != null && !isFinishing) showVipFailedDialog(failMsgRes)
        }
    }

    private fun onWatchAdClicked() {
        // Đánh dấu rewarded đang dở → onResume (khi quay lại lúc ad dismiss) sẽ KHÔNG reload,
        // để onAdHidden của ad cũ fire bình thường và trả reward.
        rewardedInFlight = true
        AdManager.showRewarded(this) { earned ->
            // onAdHidden đã fire xong (đây là callback của nó) → cho phép reload lại từ đây.
            rewardedInFlight = false
            // KHÔNG guard theo _binding — grant phải chạy dù activity đã pause/recreate.
            // earned=false (chưa sẵn sàng/đóng sớm) → vẫn thưởng 3 ngày (thiết kế AD.MD).
            grantViaRewarded()
            // Preload cho lượt xem kế tiếp (giờ ad cũ đã dismiss xong, an toàn).
            AdManager.loadRewarded(applicationContext)
        }
    }

    /** Grant 3 ngày VIP sau khi xem rewarded (grant bằng VIP_SECRET + 3 ngày — xem [applyVip]). */
    private fun grantViaRewarded() {
        applyVip(3, R.string.vip_reward_failed)
    }

    /** Dialog Material You báo kích hoạt VIP thành công (kèm confetti + haptic). */
    private fun showVipSuccessDialog(days: Int) {
        if (_binding == null || isFinishing) return
        celebrate()      // anim #5 confetti + haptic
        MaterialAlertDialogBuilder(this)
            .setIcon(R.drawable.ic_workspace_premium)
            .setTitle(R.string.vip_dialog_success_title)
            .setMessage(getString(R.string.vip_dialog_success_msg, days))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    /** Dialog Material You báo kích hoạt thất bại ([messageRes] = lý do cụ thể). */
    private fun showVipFailedDialog(messageRes: Int) {
        if (_binding == null || isFinishing) return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.vip_dialog_failed_title)
            .setMessage(messageRes)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    /**
     * Điểm 2: "Ad Choices" (form đồng ý quảng cáo GDPR) chỉ có nghĩa với user EEA.
     * Chỉ hiện entry + divider khi UMP báo privacy options là REQUIRED; non-EEA → ẩn.
     */
    private fun bindPrivacyOptions() {
        val b = _binding ?: return
        val required = try {
            UserMessagingPlatform.getConsentInformation(this).privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        } catch (_: Exception) {
            false
        }
        val vis = if (required) View.VISIBLE else View.GONE
        b.tvPrivacyOptions.visibility = vis
        b.privacyDivider.visibility = vis
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
            // Scale nhỏ (1.03) + nút có margin ngang 6dp + parent clipChildren=false →
            // pulse không bị card bo góc cắt mép (lỗi cũ ở scale 1.05).
            android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.03f),
            android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.03f),
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
        activateAnimator?.cancel(); activateAnimator = null
        countUpAnimator?.cancel(); countUpAnimator?.removeAllUpdateListeners(); countUpAnimator = null
        _binding?.shimmerCrown?.stopShimmer()
        _binding = null
        super.onDestroy()
    }
}

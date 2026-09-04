package com.omega7.messenger

import android.app.AlertDialog
import android.graphics.Bitmap
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.omega7.messenger.data.DeviceTrustRepository
import com.omega7.messenger.data.LocalMessageRepository
import com.omega7.messenger.data.PairingRepository
import com.omega7.messenger.domain.Group
import com.omega7.messenger.domain.Message
import com.omega7.messenger.pairing.DeviceProvisioningCoordinator
import com.omega7.messenger.pairing.PairingApproval
import com.omega7.messenger.pairing.PairingInvite
import com.omega7.messenger.pairing.PairingRequest
import com.omega7.messenger.security.DeviceIdentity
import com.omega7.messenger.security.DeviceKeyManager
import com.omega7.messenger.security.PanicWipe
import com.omega7.messenger.security.PinVault
import com.omega7.messenger.security.SecuritySettings
import com.omega7.messenger.security.SessionController
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var vault: PinVault
    private lateinit var repository: LocalMessageRepository
    private lateinit var settings: SecuritySettings
    private lateinit var trust: DeviceTrustRepository
    private lateinit var pairing: PairingRepository
    private lateinit var identity: DeviceIdentity
    private lateinit var deviceKeys: DeviceKeyManager
    private lateinit var provisioning: DeviceProvisioningCoordinator
    private var activeInvite: PairingInvite? = null
    private var pendingOwnerRequest: PairingRequest? = null
    private var pendingOwnerApproval: PairingApproval? = null
    private val background = Executors.newSingleThreadExecutor()
    private val session = SessionController()
    private val group = Group("omega7-main", "Grupa Ω7", 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vault = PinVault(this)
        repository = LocalMessageRepository(this)
        settings = SecuritySettings(this)
        trust = DeviceTrustRepository(this)
        pairing = PairingRepository(this)
        identity = DeviceIdentity(this)
        deviceKeys = DeviceKeyManager()
        provisioning = DeviceProvisioningCoordinator(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        showLock()
    }

    override fun onDestroy() {
        background.shutdownNow()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data) ?: return
        result.contents?.let(::handleScannedCode)
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && settings.lockOnBackground && session.state == com.omega7.messenger.security.SessionState.UNLOCKED) {
            session.lock()
        }
    }

    private fun showLock() {
        val configured = vault.isConfigured()
        val root = verticalRoot()
        root.addView(text("Ω7", 34f, Gravity.CENTER), lp())
        root.addView(text("Prywatny komunikator dla maksymalnie 7 osób", 17f, Gravity.CENTER), lp())
        val status = text(if (configured) "Wprowadź kod dostępu" else "Utwórz kod dostępu — minimum 6 znaków", 16f, Gravity.CENTER)
        root.addView(status, lp())
        val code = EditText(this).apply {
            hint = "Kod dostępu"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }
        root.addView(code, lp())
        val action = button(if (configured) "Odblokuj" else "Utwórz kod") {}
        root.addView(action, lp())
        val biometric = button("Odblokuj biometrią") {}
        biometric.visibility = if (configured && canUseBiometric() && vault.failedAttempts() < PinVault.MAX_ATTEMPTS) View.VISIBLE else View.GONE
        root.addView(biometric, lp())
        root.addView(text("Po 3 kolejnych błędnych próbach Ω7 wymazuje lokalny stan aplikacji.", 13f, Gravity.CENTER), lp())
        if (vault.failedAttempts() > 0 && configured) root.addView(text("Pozostało prób: ${vault.remainingAttempts()}", 13f, Gravity.CENTER), lp())
        setContentView(root)

        action.setOnClickListener {
            val chars = code.text.toString().toCharArray()
            code.text.clear()
            if (!configured) {
                if (chars.size < 6) {
                    chars.fill('\u0000')
                    status.text = "Kod musi mieć co najmniej 6 znaków."
                    return@setOnClickListener
                }
                vault.setPin(chars)
                chars.fill('\u0000')
                unlock()
                return@setOnClickListener
            }
            val ok = vault.verify(chars)
            chars.fill('\u0000')
            if (ok) unlock() else if (vault.failedAttempts() >= PinVault.MAX_ATTEMPTS) {
                PanicWipe.execute(this)
                session.panicWipe()
                showWiped()
            } else status.text = "Nieprawidłowy kod. Pozostało prób: ${vault.remainingAttempts()}."
        }
        biometric.setOnClickListener { authenticateBiometric { vault.resetFailedAttempts(); unlock() } }
    }

    private fun unlock() {
        session.unlock()
        showMessenger()
    }

    private fun showMessenger() {
        val root = verticalRoot()
        root.addView(text("Ω7  •  ${group.name}", 23f, Gravity.START), lp())
        root.addView(text("Magazyn lokalny: szyfrowany AES-GCM\nE2EE sieciowe: AKTYWNE tylko po pełnej rejestracji i ustanowieniu sesji\nUrządzenia: ${1 + trustedCount()}/${group.maxMembers}", 14f, Gravity.START), lp())
        val search = EditText(this).apply { hint = "Szukaj w wiadomościach…"; singleLine = true }
        root.addView(search, lp())
        val messages = TextView(this).apply { textSize = 16f; setPadding(4, 12, 4, 12); isVerticalScrollBarEnabled = true }
        root.addView(messages, weightLp(1f))
        val compose = EditText(this).apply {
            hint = "Napisz wiadomość…"; minLines = 2; maxLines = 6; gravity = Gravity.TOP
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        root.addView(compose, lp())
        val send = button("Zapisz lokalnie") {}
        root.addView(send, lp())
        val members = button("Urządzenia (${1 + trustedCount()}/7)") {}
        val securityButton = button("Bezpieczeństwo") {}
        val lock = button("Zablokuj Ω7") {}
        root.addView(members, lp()); root.addView(securityButton, lp()); root.addView(lock, lp())
        setContentView(root)
        renderMessages(messages, "")

        send.setOnClickListener {
            val body = compose.text.toString().trim()
            if (body.isEmpty()) return@setOnClickListener
            repository.append(Message(UUID.randomUUID().toString(), "Ty", body, System.currentTimeMillis(), Message.Status.LOCAL_ONLY))
            compose.text.clear()
            renderMessages(messages, search.text.toString())
        }
        search.addTextChangedListener(SimpleTextWatcher { renderMessages(messages, search.text.toString()) })
        members.setOnClickListener { showMembers() }
        securityButton.setOnClickListener { showSecurity() }
        lock.setOnClickListener { session.lock(); finishAndRemoveTask() }
    }

    private fun trustedCount() = trust.list().count { it.state == com.omega7.messenger.security.DeviceTrust.State.VERIFIED }

    private fun renderMessages(view: TextView, query: String) {
        val q = query.trim().lowercase(Locale.getDefault())
        val list = repository.list().filter { q.isEmpty() || it.body.lowercase(Locale.getDefault()).contains(q) || it.sender.lowercase(Locale.getDefault()).contains(q) }
        view.text = if (list.isEmpty()) {
            if (q.isEmpty()) "Brak wiadomości.\n\nWiadomości są obecnie zapisywane lokalnie."
            else "Brak wyników dla: $query"
        } else list.joinToString("\n\n") {
            val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it.timestampMillis))
            "${it.sender}  •  $time\n${it.body}\n• ${status(it.status)}"
        }
    }

    private fun status(s: Message.Status) = when (s) {
        Message.Status.LOCAL_ONLY -> "zapisano lokalnie — nie wysłano przez sieć"
        Message.Status.QUEUED -> "oczekuje na wysłanie"
        Message.Status.SENT -> "wysłano"
        Message.Status.DELIVERED -> "dostarczono"
        Message.Status.READ -> "odczytano"
        Message.Status.FAILED -> "błąd wysyłania"
    }

    private fun showMembers() {
        val root = verticalRoot()
        root.addView(text("Uczestnicy grupy", 25f, Gravity.START), lp())
        val trusted = trust.list().filter { it.state == com.omega7.messenger.security.DeviceTrust.State.VERIFIED }
        root.addView(text("1. To urządzenie — ${android.os.Build.MODEL}\n\nZaufane urządzenia: ${trusted.size}/6\n\nDodawaj kolejne telefony pojedynczo. Parowanie wymaga krótkotrwałego zaproszenia, podpisanej tożsamości urządzenia i jawnej akceptacji właściciela.\n\nUrządzenie nie trafia do listy zaufanych po samym zeskanowaniu QR: najpierw musi zakończyć rejestrację relay, a właściciel musi ustanowić sesję Signal.", 16f, Gravity.START), lp())
        trusted.forEachIndexed { index, d -> root.addView(text("${index + 2}. ${d.displayName}\n   ${d.fingerprint.take(24)}…", 14f, Gravity.START), lp()) }
        root.addView(button("Dodaj urządzenie — pokaż QR") { showInviteQr() }, lp())
        root.addView(button("Zeskanuj QR urządzenia do zatwierdzenia") { startQrScan() }, lp())
        root.addView(button("Dołącz ten telefon przez QR") { startQrScanForJoin() }, lp())
        root.addView(button("Wróć") { showMessenger() }, lp())
        setContentView(root)
    }

    private fun showInviteQr() {
        if (trustedCount() >= 6) {
            Toast.makeText(this, "Osiągnięto limit 7 urządzeń.", Toast.LENGTH_LONG).show()
            return
        }
        showBusy("Tworzenie jednorazowego zaproszenia…")
        background.execute {
            val result = provisioning.createOwnerInvite()
            runOnUiThread {
                result.fold(
                    { invite ->
                        activeInvite = invite
                        val payload = "omega7://pair/invite/" + PairingInvite.encode(invite)
                        showQrScreen("Zaproszenie do Grupy Ω7", payload, "Wygasa za 5 minut. Nowy telefon skanuje ten kod i generuje podpisane żądanie.")
                    },
                    { error -> showMembers(); Toast.makeText(this, "Nie udało się utworzyć zaproszenia: ${error.message ?: "błąd"}", Toast.LENGTH_LONG).show() },
                )
            }
        }
    }

    private fun startQrScan() {
        IntentIntegrator(this).setDesiredBarcodeFormats(IntentIntegrator.QR_CODE).setPrompt("Zeskanuj QR nowego urządzenia").setBeepEnabled(false).setOrientationLocked(true).initiateScan()
    }

    private fun startQrScanForJoin() {
        IntentIntegrator(this).setDesiredBarcodeFormats(IntentIntegrator.QR_CODE).setPrompt("Zeskanuj zaproszenie lub zatwierdzenie Ω7").setBeepEnabled(false).setOrientationLocked(true).initiateScan()
    }

    private fun handleScannedCode(raw: String) {
        val isRequest = raw.startsWith("omega7://pair/request/")
        val isInvite = raw.startsWith("omega7://pair/invite/")
        val isApproval = raw.startsWith("omega7://pair/approval/")
        val prefix = when {
            isRequest -> "omega7://pair/request/"
            isInvite -> "omega7://pair/invite/"
            isApproval -> "omega7://pair/approval/"
            else -> null
        } ?: run { Toast.makeText(this, "Nieznany typ QR Ω7.", Toast.LENGTH_LONG).show(); return }
        val value = raw.removePrefix(prefix)
        try {
            when {
                isRequest -> handleOwnerRequest(PairingRequest.parse(value))
                isInvite -> handleJoinInvite(PairingInvite.parse(value))
                isApproval -> handleJoinApproval(PairingApproval.parse(value))
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Nieprawidłowy QR: ${e.message ?: "błąd"}", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleOwnerRequest(req: PairingRequest) {
        require(PairingRequest.verify(req)) { "Podpis urządzenia jest nieprawidłowy." }
        require(req.groupId == group.id) { "Żądanie dotyczy innej grupy." }
        require(req.deviceId != identity.deviceId) { "To jest już to urządzenie." }
        require(req.signalDeviceId != null && req.signalBundle != null) { "Żądanie nie zawiera bundla Signal." }
        val invite = activeInvite
        require(invite != null && invite.inviteId == req.inviteId && invite.expiresAtMillis > System.currentTimeMillis()) { "Żądanie nie pasuje do aktywnego zaproszenia." }
        require(trustedCount() < 6) { "Osiągnięto limit 7 urządzeń." }
        showBusy("Weryfikacja żądania urządzenia…")
        background.execute {
            val result = provisioning.createApproval(invite, req)
            runOnUiThread {
                result.fold(
                    { approval ->
                        pendingOwnerRequest = req
                        pendingOwnerApproval = approval
                        val payload = "omega7://pair/approval/" + PairingApproval.encode(approval)
                        showQrScreen("Zatwierdzenie właściciela", payload, "Zatwierdzenie nie oznacza jeszcze zaufania. Nowy telefon skanuje ten QR i kończy rejestrację relay.")
                    },
                    { error -> showMembers(); Toast.makeText(this, "Nie udało się zatwierdzić urządzenia: ${error.message ?: "błąd"}", Toast.LENGTH_LONG).show() },
                )
            }
        }
    }

    private fun handleJoinInvite(invite: PairingInvite) {
        require(PairingInvite.verify(invite)) { "Podpis zaproszenia jest nieprawidłowy." }
        require(invite.groupId == group.id) { "Zaproszenie dotyczy innej grupy." }
        require(invite.expiresAtMillis > System.currentTimeMillis()) { "Zaproszenie wygasło." }
        pairing.add(invite)
        showBusy("Tworzenie podpisanego żądania urządzenia…")
        background.execute {
            val result = provisioning.createJoinMaterial(invite)
            runOnUiThread {
                result.fold(
                    { material -> showJoinQr(material.request) },
                    { error -> Toast.makeText(this, "Nie udało się przygotować urządzenia: ${error.message ?: "błąd"}", Toast.LENGTH_LONG).show(); showMembers() },
                )
            }
        }
    }

    private fun showJoinQr(request: PairingRequest) {
        val payload = "omega7://pair/request/" + PairingRequest.encode(request)
        showQrScreen("Zatwierdź ten telefon", payload, "Pokaż ten kod właścicielowi Grupy Ω7. Po zatwierdzeniu zeskanujesz drugi QR, aby dokończyć rejestrację.")
    }

    private fun handleJoinApproval(approval: PairingApproval) {
        require(PairingApproval.verify(approval)) { "Podpis właściciela jest nieprawidłowy." }
        val invite = pairing.pending().firstOrNull { it.inviteId == approval.inviteId }
            ?: throw IllegalStateException("Brak oczekującego zaproszenia dla tego zatwierdzenia.")
        require(invite.expiresAtMillis > System.currentTimeMillis()) { "Zaproszenie wygasło." }
        val request = PairingRequest(
            groupId = approval.groupId,
            inviteId = approval.inviteId,
            deviceId = approval.joiningDeviceId,
            displayName = approval.joiningDisplayName,
            devicePublicKey = approval.joiningDevicePublicKey,
            signature = approval.joiningRequestSignature,
            signalDeviceId = approval.signalDeviceId,
            signalBundle = approval.signalBundle,
        )
        require(PairingApproval.verifyAgainst(invite, request, approval)) { "Zatwierdzenie nie pasuje do zaproszenia i żądania." }
        showBusy("Rejestracja urządzenia w relay…")
        background.execute {
            val result = kotlinx.coroutines.runBlocking { provisioning.completeRegistration(invite, request, approval) }
            runOnUiThread {
                result.fold(
                    {
                        pairing.remove(invite.inviteId)
                        showMembers()
                        Toast.makeText(this, "Rejestracja zakończona. Urządzenie oczekuje na ustanowienie sesji Signal przez właściciela.", Toast.LENGTH_LONG).show()
                    },
                    { error -> Toast.makeText(this, "Rejestracja nieudana: ${error.message ?: "błąd"}", Toast.LENGTH_LONG).show(); showMembers() },
                )
            }
        }
    }

    private fun finalizePendingOwnerEnrollment() {
        val request = pendingOwnerRequest ?: run { Toast.makeText(this, "Brak oczekującego urządzenia.", Toast.LENGTH_LONG).show(); return }
        val approval = pendingOwnerApproval ?: run { Toast.makeText(this, "Brak zatwierdzenia.", Toast.LENGTH_LONG).show(); return }
        require(request.signalDeviceId == approval.signalDeviceId) { "Niespójny DeviceID." }
        showBusy("Pobieranie bundla i ustanawianie sesji Signal…")
        background.execute {
            val result = kotlinx.coroutines.runBlocking { provisioning.finalizeOwnerEnrollment(group.id, approval.signalDeviceId) }
            runOnUiThread {
                result.fold(
                    {
                        trust.put(com.omega7.messenger.security.DeviceTrust.TrustedDevice(request.deviceId, request.displayName, fingerprint(request.devicePublicKey), com.omega7.messenger.security.DeviceTrust.State.VERIFIED))
                        pendingOwnerRequest = null
                        pendingOwnerApproval = null
                        activeInvite = null
                        showMembers()
                        Toast.makeText(this, "Urządzenie zarejestrowane i sesja Signal została ustanowiona.", Toast.LENGTH_LONG).show()
                    },
                    { error -> showMembers(); Toast.makeText(this, "Urządzenie nie jest jeszcze gotowe: ${error.message ?: "błąd"}", Toast.LENGTH_LONG).show() },
                )
            }
        }
    }

    private fun showBusy(message: String) {
        val root = verticalRoot()
        root.addView(text("Ω7", 30f, Gravity.CENTER), lp())
        root.addView(text(message, 17f, Gravity.CENTER), lp())
        setContentView(root)
    }

    private fun showQrScreen(title: String, payload: String, hint: String) {
        val root = verticalRoot()
        root.addView(text(title, 24f, Gravity.CENTER), lp())
        val image = ImageView(this).apply {
            adjustViewBounds = true
            setImageBitmap(makeQr(payload))
            contentDescription = "Kod QR parowania Ω7"
        }
        root.addView(image, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(text(hint, 14f, Gravity.CENTER), lp())
        root.addView(text("Nie udostępniaj zrzutu ekranu tego kodu osobom postronnym.", 13f, Gravity.CENTER), lp())
        if (pendingOwnerApproval != null && title == "Zatwierdzenie właściciela") {
            root.addView(button("Sprawdź rejestrację i ustanów sesję") { finalizePendingOwnerEnrollment() }, lp())
        }
        root.addView(button("Wróć") { showMembers() }, lp())
        setContentView(root)
    }

    private fun makeQr(payload: String): Bitmap {
        val matrix = MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, 900, 900)
        val bitmap = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
        for (x in 0 until matrix.width) for (y in 0 until matrix.height) bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        return bitmap
    }

    private fun fingerprint(publicKey: String): String = java.security.MessageDigest.getInstance("SHA-256")
        .digest(android.util.Base64.decode(publicKey, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING))
        .joinToString("") { "%02x".format(it) }

    private fun showSecurity() {
        val root = verticalRoot()
        root.addView(text("Bezpieczeństwo Ω7", 25f, Gravity.START), lp())
        root.addView(text("Stan sesji: ${session.state}\n\nMagazyn lokalny: AES-256-GCM\nKlucz magazynu: Android Keystore\nUwierzytelnianie: kod dostępu + silna biometria, jeśli dostępna\nKopie zapasowe: wyłączone\nHTTP: zablokowany\nZrzuty ekranu: zablokowane\nBlokada po opuszczeniu aplikacji: ${if (settings.lockOnBackground) "włączona" else "wyłączona"}\nE2EE grupowe: sesje Signal są ustanawiane per urządzenie; wysyłanie sieciowe nadal wymaga pełnego spięcia kolejki wiadomości z transportem.", 15f, Gravity.START), lp())
        val lockBg = Switch().apply { text = "Blokuj po opuszczeniu Ω7"; isChecked = settings.lockOnBackground; setOnCheckedChangeListener { _, checked -> settings.lockOnBackground = checked } }
        root.addView(lockBg, lp())
        root.addView(button("Zmień kod dostępu") { showChangePin() }, lp())
        root.addView(button("Wykonaj panic wipe") { showPanicConfirm() }, lp())
        root.addView(button("Wróć") { showMessenger() }, lp())
        setContentView(root)
    }

    private fun showChangePin() {
        val root = verticalRoot(); root.addView(text("Zmiana kodu dostępu", 25f, Gravity.START), lp())
        val old = EditText(this).apply { hint = "Obecny kod"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val newPin = EditText(this).apply { hint = "Nowy kod — minimum 6 znaków"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val result = text("", 14f, Gravity.CENTER)
        root.addView(old, lp()); root.addView(newPin, lp()); root.addView(result, lp())
        root.addView(button("Zmień kod") {
            val a = old.text.toString().toCharArray(); val b = newPin.text.toString().toCharArray(); old.text.clear(); newPin.text.clear()
            if (b.size < 6) { a.fill('\u0000'); b.fill('\u0000'); result.text = "Nowy kod musi mieć co najmniej 6 znaków."; return@button }
            if (vault.changePin(a, b)) { a.fill('\u0000'); b.fill('\u0000'); result.text = "Kod został zmieniony."; showSecurity() } else { a.fill('\u0000'); b.fill('\u0000'); result.text = "Nieprawidłowy obecny kod." }
        }, lp())
        root.addView(button("Anuluj") { showSecurity() }, lp()); setContentView(root)
    }

    private fun showPanicConfirm() {
        AlertDialog.Builder(this).setTitle("Panic wipe")
            .setMessage("Zostanie usunięty lokalny klucz szyfrujący, dane wiadomości, uwierzytelnianie i zaufane urządzenia. Tej operacji nie można cofnąć.")
            .setNegativeButton("Anuluj", null)
            .setPositiveButton("WYMAŻ") { _, _ -> PanicWipe.execute(this); session.panicWipe(); showWiped() }.show()
    }

    private fun showWiped() {
        val root = verticalRoot()
        root.addView(text("Ω7\n\nWYMAZANIE DANYCH ZAKOŃCZONE", 22f, Gravity.CENTER), lp())
        root.addView(text("Usunięto lokalny materiał uwierzytelniający, klucz magazynu, dane aplikacji i rejestr zaufania.\n\nTelefon z Androidem nie został przywrócony do ustawień fabrycznych.", 16f, Gravity.CENTER), lp())
        setContentView(root)
    }

    private fun canUseBiometric(): Boolean = BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    private fun authenticateBiometric(onSuccess: () -> Unit) {
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
        })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("Odblokuj Ω7").setSubtitle("Użyj silnej biometrii urządzenia").setNegativeButtonText("Anuluj").build())
    }

    private fun verticalRoot() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(36, 36, 36, 36) }
    private fun text(value: String, size: Float, gravityValue: Int) = TextView(this).apply { text = value; textSize = size; gravity = gravityValue; setPadding(0, 8, 0, 8) }
    private fun button(label: String, action: () -> Unit) = Button(this).apply { text = label; setOnClickListener { action() } }
    private fun lp() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 10 }
    private fun weightLp(weight: Float) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, weight).apply { bottomMargin = 10 }

    private class SimpleTextWatcher(private val onChange: () -> Unit) : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = onChange()
        override fun afterTextChanged(s: android.text.Editable?) = Unit
    }
}

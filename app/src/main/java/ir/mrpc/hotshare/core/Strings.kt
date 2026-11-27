package ir.mrpc.hotshare.core

/** All user-facing text. Persian when the system language is Persian, English otherwise. */
class Strings(val fa: Boolean) {
    companion object {
        fun system() = Strings(Prefs.lang == "fa")
    }

    private fun t(f: String, e: String) = if (fa) f else e

    val appName get() = "HotShare"
    val tagline get() = t("اشتراک امن VPN با دستگاه‌های اطراف", "Share your VPN with people nearby")
    val designedBy get() = "Designed by Mr.PC"

    // Navigation
    val navHome get() = t("خانه", "Home")
    val navMenu get() = t("منو", "Menu")
    val navHelp get() = t("راهنما", "Help")
    val back get() = t("بازگشت", "Back")
    val cancel get() = t("انصراف", "Cancel")
    val ok get() = t("تأیید", "OK")
    val close get() = t("بستن", "Close")

    // Home
    val hello get() = t("خوش آمدید", "Welcome")
    val chooseRole get() = t("امروز چه کاری انجام بدهیم؟", "What would you like to do?")
    val shareTitle get() = t("اشتراک VPN", "Share VPN")
    val shareDesc get() = t("VPN گوشی‌ات را با دستگاه‌های نزدیک به اشتراک بگذار", "Let nearby devices use your phone's VPN")
    val receiveTitle get() = t("دریافت VPN", "Get VPN")
    val receiveDesc get() = t("نزدیک‌ترین دستگاه را پیدا کن و وصل شو", "Find the nearest device and connect")
    val chipVpn get() = "VPN"
    val chipWifi get() = "Wi-Fi"
    val chipBluetooth get() = t("بلوتوث", "Bluetooth")
    val on get() = t("روشن", "On")
    val off get() = t("خاموش", "Off")
    val active get() = t("فعال", "Active")
    val inactive get() = t("غیرفعال", "Inactive")

    // Host
    val hostTitle get() = t("اشتراک VPN", "Share VPN")
    val stepHotspot get() = t("هات‌اسپات", "Hotspot")
    val stepVpn get() = "VPN"
    val stepVisible get() = t("قابل مشاهده برای اطراف", "Visible to nearby")
    val hotspotAuto get() = t("هات‌اسپات خودکار آماده است", "Auto hotspot is ready")
    val hotspotSystem get() = t("هات‌اسپات گوشی روشن است", "Phone hotspot is on")
    val hotspotWaiting get() = t("در حال آماده‌سازی…", "Preparing…")
    val hotspotOff get() = t("هات‌اسپات خاموش است", "Hotspot is off")
    val openHotspot get() = t("روشن کردن", "Turn on")
    val hotspotNote get() = t(
        "اندروید اجازه نمی‌دهد برنامه‌ها هات‌اسپات گوشی را مستقیم روشن کنند. صفحه‌ی تنظیمات باز می‌شود؛ پس از روشن شدن، برنامه خودکار متوجه می‌شود.",
        "Android doesn't let apps switch the phone hotspot on directly. Settings will open; the app notices automatically once it's on.")
    val vpnOn get() = t("VPN شما فعال است", "Your VPN is active")
    val vpnOff get() = t("VPN پیدا نشد", "No VPN found")
    val vpnNote get() = t("اول برنامه VPN خودت را باز و روشن کن، بعد به اینجا برگرد. اگر VPN روشن نباشد، اینترنت دیگران بدون VPN خارج می‌شود.", "Open your VPN app and turn it on first, then come back. Without a VPN, others' traffic leaves unprotected.")
    val visibleOn get() = t("دستگاه‌های اطراف شما را می‌بینند", "Nearby devices can see you")
    val visibleOff get() = t("بلوتوث یا مجوز فعال نیست", "Bluetooth or permission is off")
    val bleUnsupported get() = t("این گوشی از «دیده شدن با بلوتوث» پشتیبانی نمی‌کند؛ فقط با QR یا کد می‌شود وصل شد.", "This phone can't be discovered over Bluetooth; others can only join with the QR or the code.")
    val bleUnsupportedShort get() = t("فقط با QR یا کد", "QR or code only")
    val bleNoPerm get() = t("مجوز بلوتوث داده نشده است", "Bluetooth permission not granted")
    val bleOffState get() = t("بلوتوث خاموش است", "Bluetooth is off")
    val bleStarting get() = t("در حال فعال‌سازی…", "Starting…")
    val noWifiHw get() = t("این گوشی وای‌فای/هات‌اسپات ندارد", "This phone has no Wi-Fi / hotspot")
    val ownVpnBusy get() = t("در حال دریافت VPN هستی؛ هم‌زمان نمی‌شود VPN داد. اول اتصال را قطع کن.", "You're receiving a VPN right now; you can't share at the same time. Disconnect first.")
    val hostPortBusy get() = t("پورت اتصال را برنامه‌ی دیگری گرفته است. «کد جدید» بساز و دوباره شروع کن.", "The connection port is used by another app. Create a new code and start again.")
    val tabStatus get() = t("آماده‌سازی", "Setup")
    val tabConnect get() = t("QR و کد", "QR & code")
    val tabDevices get() = t("دستگاه‌ها", "Devices")
    val qrAfterStart get() = t("با شروع اشتراک، QR هم ساخته می‌شود. کد را همین حالا هم می‌شود به دوستت داد.", "The QR appears once sharing starts. You can already give the code to your friend.")
    val codeOnlyNote get() = t("این گوشی دیده نمی‌شود؛ دوستت باید QR را اسکن کند یا کد را بزند.", "This phone can't be found by radar; your friend must scan the QR or type the code.")
    val waitingDevices get() = t("هنوز دستگاهی وصل نشده است؛ منتظر اتصال…", "No device connected yet; waiting…")
    fun devicesConnected(c: Int) = t("${n(c)} دستگاه متصل است", if (c == 1) "1 device connected" else "$c devices connected")
    fun deviceJoined(name: String) = t("«$name» وصل شد", "“$name” connected")
    fun connectedFor(min: Int) = if (min < 1) t("همین الان", "just now") else t("${n(min)} دقیقه پیش", "$min min ago")
    fun notifConnected(c: Int) = t("${n(c)} دستگاه متصل است — برای توقف دکمه‌ی زیر را بزن", if (c == 1) "1 device connected — tap stop to end" else "$c devices connected — tap stop to end")
    val sharingKeepsRunning get() = t("اشتراک در پس‌زمینه ادامه دارد، مگر اینکه «توقف اشتراک» را بزنی.", "Sharing keeps running in the background unless you tap Stop sharing.")
    val connectedKeepsRunning get() = t("VPN وصل است و ادامه دارد، مگر اینکه «قطع اتصال» را بزنی.", "The VPN stays connected unless you tap Disconnect.")
    fun sharingNow(c: Int) = if (c > 0) t("در حال اشتراک — ${n(c)} دستگاه متصل", "Sharing — $c connected") else t("در حال اشتراک — منتظر دستگاه", "Sharing — waiting for a device")
    val goBack get() = t("ادامه کار در این صفحه", "Open")
    val stopShort get() = t("توقف", "Stop")
    val trustedCleared get() = t("دستگاه‌های مورد اعتماد پاک شدند", "Trusted devices cleared")
    val codeRegenerated get() = t("کد جدید ساخته شد", "New code created")
    val codeRegeneratedRestart get() = t("کد جدید ساخته شد و اشتراک دوباره شروع شد؛ دستگاه‌های قبلی باید دوباره وصل شوند", "New code created and sharing restarted; earlier devices must reconnect")
    val trustedForgotten get() = t("فراموش شد", "Forgotten")
    val sharingBlocksReceive get() = t("الان در حال اشتراک‌گذاری هستی. برای دریافت VPN اول اشتراک را متوقف کن.", "You're sharing right now. Stop sharing before you receive a VPN.")
    val stopSharingBtn get() = t("توقف اشتراک", "Stop sharing")
    val noBleReceive get() = t("این گوشی بلوتوث کم‌مصرف (BLE) ندارد و نمی‌تواند رادار را اجرا کند. با QR یا کد وصل شو.", "This phone has no Bluetooth LE, so the radar can't run. Use the QR or the code.")
    val distVeryNear get() = t("خیلی نزدیک", "Very close")
    val distNear get() = t("نزدیک", "Close")
    val distFar get() = t("دورتر", "Farther")
    val startShare get() = t("شروع اشتراک", "Start sharing")
    val stopShare get() = t("توقف اشتراک", "Stop sharing")
    val sharing get() = t("در حال اشتراک‌گذاری", "Sharing now")
    val scanHint get() = t("این QR را روی گوشی دوم اسکن کنید", "Scan this QR on the other phone")
    val codeLabel get() = t("کد اتصال", "Connection code")
    val copy get() = t("کپی", "Copy")
    val copied get() = t("کپی شد", "Copied")
    val newCode get() = t("کد جدید", "New code")
    val devices get() = t("دستگاه", "Devices")
    val upload get() = t("ارسال", "Sent")
    val download get() = t("دریافت", "Received")
    val wifiNetwork get() = t("شبکه‌ی وای‌فای", "Wi-Fi network")
    val wifiPassword get() = t("رمز وای‌فای", "Wi-Fi password")
    val approveTitle get() = t("درخواست اتصال", "Connection request")
    val approveBody get() = t("می‌خواهد از VPN شما استفاده کند.", "wants to use your VPN.")
    val allow get() = t("اجازه", "Allow")
    val deny get() = t("رد کردن", "Deny")
    val allowOnce get() = t("اجازه همین بار", "Allow this time")
    val allowAlways get() = t("اجازه برای همیشه", "Always allow")
    val forgetTrusted get() = t("فراموش کردن دستگاه‌های مورد اعتماد", "Forget trusted devices")
    fun trustedCount(n: Int) = t("$n دستگاه مورد اعتماد", "$n trusted device(s)")
    val notifAskTitle get() = t("درخواست اتصال جدید", "New connection request")

    // Receive / radar
    val receiveHeader get() = t("دریافت VPN", "Get VPN")
    val searching get() = t("در حال جستجوی دستگاه‌های اطراف…", "Looking for devices nearby…")
    val nearest get() = t("نزدیک‌ترین", "Nearest")
    val tapToConnect get() = t("برای اتصال لمس کنید", "Tap to connect")
    val connectNow get() = t("اتصال", "Connect")
    val nothingFound get() = t("دستگاهی پیدا نشد. با QR یا کد وصل شوید.", "No device found. Connect with a QR or a code.")
    val scanQr get() = t("اسکن QR", "Scan QR")
    val enterCode get() = t("وارد کردن کد", "Enter code")
    val permNearby get() = t("برای پیدا کردن دستگاه‌های اطراف، مجوز «دستگاه‌های اطراف» لازم است.", "Allow “Nearby devices” so we can find phones around you.")
    val permGrant get() = t("اجازه دادن", "Grant access")
    val btOffNote get() = t("بلوتوث خاموش است.", "Bluetooth is off.")
    val btTurnOn get() = t("روشن کردن بلوتوث", "Turn on Bluetooth")

    // Scanner
    val scanTitle get() = t("اسکن QR", "Scan QR")
    val scanPrompt get() = t("QR را داخل کادر قرار دهید", "Point the camera at the QR code")
    val scanInvalid get() = t("این QR مربوط به HotShare نیست", "This isn't a HotShare QR code")
    val camPerm get() = t("برای اسکن QR دسترسی به دوربین لازم است.", "Camera access is needed to scan the QR code.")
    val torch get() = t("چراغ قوه", "Torch")

    // Code entry
    val codeTitle get() = t("کد ۸ رقمی", "8-digit code")
    val codeSub get() = t("کدی که روی گوشی میزبان نمایش داده می‌شود را وارد کنید", "Type the code shown on the host phone")

    val showPass get() = t("نمایش رمز", "Show password")

    // Connect flow
    val connectingTo get() = t("در حال اتصال به", "Connecting to")
    val stageRequest get() = t("درخواست از میزبان", "Asking the host")
    val stageRequestDone get() = t("میزبان تأیید کرد", "Host approved")
    val stageWifi get() = t("اتصال به وای‌فای", "Joining Wi-Fi")
    val stageVerify get() = t("بررسی میزبان و کد", "Checking host & code")
    val stageTunnel get() = t("ساخت تونل امن VPN", "Creating secure tunnel")
    val connected get() = t("متصل شدید", "You're connected")
    fun routedVia(h: String) = if (fa) "اینترنت شما از VPN «$h» عبور می‌کند" else "Your traffic goes through $h's VPN"
    val duration get() = t("مدت اتصال", "Duration")
    val disconnect get() = t("قطع اتصال", "Disconnect")
    val retry get() = t("تلاش دوباره", "Try again")
    val failedTitle get() = t("اتصال برقرار نشد", "Couldn't connect")

    val errCode get() = t("کد اشتباه است یا میزبان کد را عوض کرده. کد جدید را بگیر.", "Wrong code, or the host changed it. Get the new code.")
    val errUnreach get() = t("میزبان در دسترس نیست. مطمئن شوید میزبان اشتراک را شروع کرده است.", "Host not reachable. Make sure the host has started sharing.")
    val errNative get() = t("موتور تونل در این نسخه نصب نشده یا اجرا نشد.", "The tunnel engine is missing or failed to start.")
    val errVpn get() = t("مجوز VPN داده نشد.", "VPN permission was not granted.")
    val errLost get() = t("ارتباط با میزبان قطع شد.", "Connection to the host was lost.")
    val errWifiPass get() = t("به وای‌فای میزبان وصل نشد. رمز هات‌اسپات اشتباه است یا عوض شده، یا میزبان دور است.", "Couldn't join the host's Wi-Fi. The hotspot password is wrong or was changed, or the host is out of range.")
    val errWifiLost get() = t("از وای‌فای میزبان جدا شدی (هات‌اسپات خاموش شد، دور شدی یا رمزش عوض شد).", "You left the host's Wi-Fi (hotspot turned off, out of range, or its password changed).")
    val errHostStopped get() = t("وای‌فای وصل است ولی میزبان اشتراک را متوقف کرده یا VPN را بسته است.", "Wi-Fi is fine, but the host stopped sharing or closed the VPN.")
    val openWifiSettings get() = t("باز کردن تنظیمات وای‌فای", "Open Wi-Fi settings")
    val errQr get() = t("QR نامعتبر است.", "Invalid QR code.")
    val errDenied get() = t("میزبان درخواست را رد کرد یا پاسخ نداد.", "The host declined or didn't answer.")
    val errBle get() = t("ارتباط بلوتوثی با میزبان برقرار نشد. دوباره تلاش کنید یا از QR استفاده کنید.", "Couldn't reach the host over Bluetooth. Try again or use the QR code.")
    val errWifi get() = t("اتصال به وای‌فای میزبان انجام نشد.", "Couldn't join the host's Wi-Fi.")
    val errCanceled get() = t("اتصال لغو شد.", "Connection canceled.")

    // Menu / settings
    val menuTitle get() = t("منو", "Menu")
    val secQuick get() = t("دسترسی سریع", "Quick actions")
    val secSharing get() = t("اشتراک‌گذاری", "Sharing")
    val secAppearance get() = t("ظاهر", "Appearance")
    val secDevice get() = t("دستگاه", "Device")
    val secSupport get() = t("پشتیبانی", "Support")
    val theme get() = t("پوسته", "Theme")
    val themeSystem get() = t("سیستم", "System")
    val themeLight get() = t("روشن", "Light")
    val themeDark get() = t("تیره", "Dark")
    val autoHotspot get() = t("هات‌اسپات خودکار", "Auto hotspot")
    val autoHotspotDesc get() = t("هات‌اسپات اختصاصی برنامه را خودکار می‌سازد", "Creates an app hotspot automatically")
    val autoStart get() = t("شروع خودکار اشتراک", "Auto-start sharing")
    val autoStartDesc get() = t("وقتی هات‌اسپات روشن شد", "When your hotspot turns on")
    val autoApprove get() = t("تأیید خودکار دستگاه‌ها", "Approve devices automatically")
    val autoApproveDesc get() = t("بدون پرسیدن، اتصال دستگاه‌های نزدیک را بپذیر", "Accept nearby devices without asking")
    val hotspotDetails get() = t("مشخصات هات‌اسپات", "Hotspot details")
    val hotspotDetailsDesc get() = t("نام و رمز هات‌اسپات گوشی را یک‌بار ذخیره کنید", "Save your phone hotspot name and password once")
    val keepOn get() = t("روشن ماندن صفحه", "Keep screen on")
    val keepOnDesc get() = t("هنگام اشتراک یا اتصال", "While sharing or connected")
    val battery get() = t("بهینه‌سازی باتری", "Battery optimization")
    val batteryDesc get() = t("برای اینکه اشتراک در پس‌زمینه قطع نشود", "So sharing isn't stopped in the background")
    val regen get() = t("ساخت کد جدید", "Generate new code")
    val regenDesc get() = t("دستگاه‌های قبلی دیگر نمی‌توانند وصل شوند", "Previously paired devices can no longer connect")
    val save get() = t("ذخیره", "Save")
    val howTitle get() = t("راهنمای استفاده", "How it works")
    val privacyTitle get() = t("حریم خصوصی", "Privacy")
    val aboutTitle get() = t("درباره‌ی برنامه", "About")
    val version get() = t("نسخه", "Version")
    val helpHost get() = if (fa) listOf(
        "VPN خودت را روشن کن (از خود برنامه VPN).",
        "برنامه را باز کن و «اشتراک VPN» را بزن.",
        "هر وقت برنامه بلوتوث یا مجوزی خواست، فقط «اجازه» را بزن.",
        "دکمه «شروع اشتراک» را بزن. هات‌اسپات خودکار ساخته می‌شود؛ اگر نشد، هات‌اسپات گوشی را دستی روشن کن.",
        "وقتی دوستت درخواست داد «اجازه» را بزن؛ یا QR یا کد را به او بده.",
        "تا وقتی دیگران وصل‌اند برنامه را نبند. برای پایان، «توقف اشتراک» را بزن."
    ) else listOf(
        "Turn on your VPN (from your VPN app).",
        "Open this app and tap Share VPN.",
        "When the app asks for Bluetooth or a permission, just tap Allow.",
        "Tap Start sharing. An app hotspot is created automatically; if not, turn on your phone hotspot manually.",
        "When your friend asks to join tap Allow, or hand over the QR / code.",
        "Keep the app open while others are connected. Tap Stop sharing when you're done."
    )
    val helpClient get() = if (fa) listOf(
        "مطمئن شو VPN دیگری روی گوشی تو روشن نیست.",
        "«دریافت VPN» را بزن و بلوتوث و مجوزها را وقتی خواست روشن کن.",
        "اسم گوشی میزبان روی رادار می‌آید؛ لمسش کن (یا QR را اسکن کن، یا کد را بزن).",
        "اگر روی هات‌اسپات میزبان نبودی، برنامه نام وای‌فای را نشان می‌دهد و تو را به تنظیمات وای‌فای گوشی می‌برد؛ همان را انتخاب کن و رمز را بزن. وقتی برگشتی خودش ادامه می‌دهد و پیام VPN اندروید را تأیید کن.",
        "وقتی «متصل شدید» را دیدی، اینترنتت از VPN دوستت می‌گذرد."
    ) else listOf(
        "Make sure no other VPN is on, on your phone.",
        "Tap Get VPN and turn on Bluetooth / permissions when asked.",
        "The host's name appears on the radar; tap it (or scan the QR, or type the code).",
        "If you're not on the host's hotspot the app shows its name and takes you to your phone's Wi-Fi settings; join it and type the password. The app continues by itself when you return; approve Android's VPN prompt.",
        "When you see “You're connected”, your traffic goes through your friend's VPN."
    )
    val shareHeader get() = t("برای میزبان", "For the host")
    val clientHeader get() = t("برای دریافت‌کننده", "For the receiver")
    val privacy get() = t(
        "HotShare هیچ داده‌ای جمع‌آوری نمی‌کند، تبلیغ ندارد و به سرور خارجی وصل نمی‌شود. ترافیک فقط بین دو گوشی و با کد اتصال رد‌وبدل می‌شود. کد را فقط به افراد مورد اعتماد بدهید.",
        "HotShare collects no data, shows no ads and talks to no external server. Traffic only flows between the two phones, protected by the pairing code. Share the code only with people you trust.")

    // Notifications
    val notifTitle get() = t("HotShare در حال اشتراک‌گذاری است", "HotShare is sharing your VPN")
    val notifText get() = t("برای توقف، دکمه‌ی زیر را بزنید", "Tap stop to end sharing")
    val notifStop get() = t("توقف", "Stop")

    // ───────────── Numbers ─────────────
    fun n(i: Int): String =
        if (fa) i.toString().map { if (it in '0'..'9') '۰' + (it - '0') else it }.joinToString("") else i.toString()

    // ───────────── Language ─────────────
    val language get() = t("زبان", "Language")
    val langFa get() = "فارسی"
    val langEn get() = "English"
    val secLook get() = t("زبان و ظاهر", "Language & look")
    val secStart get() = t("شروع سریع", "Quick start")
    val secHelpGroup get() = t("راهنما و پشتیبانی", "Help & support")

    // ───────────── Fix-it actions ─────────────
    val fixTurnOn get() = t("روشن کردن", "Turn on")
    val fixGrant get() = t("اجازه دادن", "Allow")
    val fixOpen get() = t("باز کردن", "Open")
    val tapToFix get() = t("لمس کن تا روشن شود", "Tap to turn on")
    val homeNotReady get() = t("چند مورد آماده نیست", "A few things aren't ready")
    val homeNotReadyDesc get() = t("بلوتوث یا وای‌فای خاموش است؛ برای درست کردن لمس کنید", "Bluetooth or Wi-Fi is off. Tap to fix it")
    val chipHint get() = t("اگر مورد خاموشی دیدید، رویش بزنید تا برنامه درخواست روشن‌شدن بدهد.", "If something is off, tap it and the app will ask to turn it on.")
    val wifiOffNote get() = t("وای‌فای خاموش است؛ برای اتصال به میزبان لازم است.", "Wi-Fi is off. It's needed to join the host.")
    val turnOnWifi get() = t("روشن کردن وای‌فای", "Turn on Wi-Fi")

    // ───────────── Setup screen ─────────────
    val setupTitle get() = t("آماده‌سازی دستگاه", "Get your phone ready")
    val setupRowDesc get() = t("بلوتوث، وای‌فای، مجوزها و VPN را یک‌جا ببین و روشن کن", "Check and switch on Bluetooth, Wi-Fi, permissions and VPN in one place")
    val setupIntro get() = t(
        "اندروید اجازه نمی‌دهد برنامه‌ها بی‌صدا بلوتوث یا وای‌فای را روشن کنند. برای هر مورد نارنجی دکمه‌اش را بزن؛ پنجره‌ی خود اندروید باز می‌شود و فقط «اجازه» را بزن.",
        "Android doesn't let apps switch Bluetooth or Wi-Fi on silently. Tap the button on any orange item; Android shows its own prompt and you just tap Allow.")
    val setupReceiverTitle get() = t("اگر می‌خواهی VPN بگیری", "If you want to get a VPN")
    val setupHostTitle get() = t("اگر می‌خواهی VPN بدهی", "If you want to share a VPN")
    val setupAllReady get() = t("همه‌چیز آماده است", "All set")
    val reqNearby get() = t("مجوز دستگاه‌های اطراف", "Nearby devices permission")
    val reqNearbyDesc get() = t("برای پیدا کردن گوشی‌های نزدیک", "To find phones around you")
    val reqHostPerm get() = t("مجوزهای اشتراک", "Sharing permissions")
    val reqHostPermDesc get() = t("بلوتوث، هات‌اسپات و اعلان", "Bluetooth, hotspot and notifications")
    val reqBluetooth get() = t("بلوتوث", "Bluetooth")
    val reqBluetoothClientDesc get() = t("تا میزبان روی رادار پیدا شود", "So the host shows up on the radar")
    val reqBluetoothHostDesc get() = t("تا گوشی‌های اطراف تو را ببینند", "So nearby phones can see you")
    val reqWifi get() = t("وای‌فای", "Wi-Fi")
    val reqWifiDesc get() = t("برای وصل شدن به هات‌اسپات میزبان", "To join the host's hotspot")
    val reqCamera get() = t("دوربین", "Camera")
    val reqCameraDesc get() = t("فقط برای اسکن QR (اختیاری)", "Only for scanning QR codes (optional)")
    val reqVpn get() = t("VPN روشن", "VPN on")
    val reqVpnDesc get() = t("برنامه VPN خودت را روشن کن", "Turn on your own VPN app")
    val reqHotspot get() = t("هات‌اسپات", "Hotspot")
    val reqHotspotDesc get() = t("اگر هات‌اسپات خودکار کار نکرد، دستی روشنش کن", "If the auto hotspot doesn't work, turn it on manually")
    val reqBattery get() = t("بهینه‌سازی باتری", "Battery optimization")
    val reqBatteryDesc get() = t("تا اشتراک در پس‌زمینه قطع نشود (پیشنهادی)", "So sharing isn't stopped in the background (recommended)")

    // ───────────── Privacy screen ─────────────
    val privacyIntro get() = t("چه چیزی با اطلاعات تو اتفاق می‌افتد؟", "What happens to your data?")

    // ───────────── Tutorial (one combined walkthrough) ─────────────
    val tutView get() = t("مشاهده‌ی آموزش", "View the tutorial")
    val tutViewDesc get() = t("یک آموزش کوتاه برای کل برنامه", "One short tutorial for the whole app")
    val tutAtStart get() = t("نمایش آموزش هنگام باز شدن", "Show tutorial when the app opens")
    val tutAtStartDesc get() = t("به‌طور پیش‌فرض فقط بار اول نمایش داده می‌شود", "By default it is shown only the first time")
    val obSkip get() = t("رد کردن", "Skip")
    val obNext get() = t("بعدی", "Next")
    val obPrev get() = t("قبلی", "Back")
    val obDone get() = t("شروع کنیم", "Let's start")
    fun obPage(i: Int, total: Int) = if (fa) "${n(i)} از ${n(total)}" else "$i / $total"

    val ob1Title get() = t("به HotShare خوش آمدی", "Welcome to HotShare")
    val ob1Body get() = t(
        "VPN گوشی‌ات را با گوشی نزدیک خودت به اشتراک بگذار؛ یا اگر VPN نداری، از VPN دوستت استفاده کن. بدون حساب، بدون سرور.",
        "Share your phone's VPN with a phone next to you, or use a friend's VPN if you have none. No account, no server.")
    val ob2Title get() = t("اگر VPN داری: «اشتراک VPN»", "You have a VPN: “Share VPN”")
    val ob2Body get() = t(
        "اول VPN خودت را روشن کن. بعد «اشتراک VPN» و «شروع اشتراک» را بزن. وقتی دوستت درخواست داد، «اجازه» را بزن؛ یا QR و کد را به او نشان بده.",
        "Turn your VPN on first. Then tap Share VPN and Start sharing. When your friend asks, tap Allow, or show the QR and code.")
    val ob3Title get() = t("اگر VPN نداری: «دریافت VPN»", "You need a VPN: “Get VPN”")
    val ob3Body get() = t(
        "«دریافت VPN» را بزن. گوشی میزبان روی رادار می‌آید؛ رویش بزن. اگر پیدا نشد با «اسکن QR» یا «وارد کردن کد» وصل شو.",
        "Tap Get VPN. The host phone shows on the radar; tap it. If it doesn't, use Scan QR or Enter code.")
    val ob4Title get() = t("حتماً به وای‌فای میزبان وصل شو", "Be on the host's Wi-Fi")
    val ob4Body get() = t(
        "گوشی دریافت‌کننده باید روی هات‌اسپات میزبان باشد. اگر نبودی، برنامه نام وای‌فای را نشان می‌دهد و به تنظیمات وای‌فای گوشی می‌برد؛ همان را انتخاب کن. وقتی وصل شدی برنامه خودش ادامه می‌دهد.",
        "The receiving phone must be on the host's hotspot. If it isn't, the app shows the network name and opens your Wi-Fi settings; join it and the app carries on by itself.")
    val ob5Title get() = t("آماده‌ای!", "You're ready!")
    val ob5Body get() = t(
        "اگر بلوتوث یا وای‌فای خاموش بود، برنامه خودش درخواست روشن‌شدن می‌دهد. این آموزش را هر وقت خواستی از «منو ← عمومی» دوباره ببین.",
        "If Bluetooth or Wi-Fi is off, the app asks to switch it on. Replay this tutorial any time from Menu → General.")

    // ───────────── Menu pages ─────────────
    val menuSetupDesc get() = t("بلوتوث، وای‌فای، مجوزها و VPN", "Bluetooth, Wi-Fi, permissions and VPN")
    val menuShare get() = t("تنظیمات اشتراک", "Sharing settings")
    val menuShareDesc get() = t("هات‌اسپات، تأیید دستگاه‌ها، کد اتصال", "Hotspot, approvals, connection code")
    val menuLookDesc get() = t("زبان و پوسته", "Language and theme")
    val menuGeneral get() = t("عمومی", "General")
    val menuGeneralDesc get() = t("صفحه، باتری و آموزش", "Screen, battery and tutorial")

    // ───────────── Wi-Fi guide (receiver) ─────────────
    val wgTitle get() = t("به وای‌فای میزبان وصل شو", "Join the host's Wi-Fi")
    val wgNetwork get() = t("نام وای‌فای", "Wi-Fi name")
    val wgPassword get() = t("رمز", "Password")
    val wgUnknownName get() = t("هات‌اسپات گوشی میزبان", "The host phone's hotspot")
    val wgAskName get() = t("نامش را از میزبان بپرس؛ میزبان آن را در صفحه‌ی «اشتراک VPN» می‌بیند.", "Ask the host for its name; the host sees it on the Share VPN screen.")
    fun wgSteps(name: String?, hasPass: Boolean): List<String> = if (fa) listOf(
        "دکمه‌ی «باز کردن تنظیمات وای‌فای» را بزن.",
        if (name != null) "«$name» را انتخاب کن." else "هات‌اسپات گوشی میزبان را انتخاب کن.",
        if (hasPass) "رمز بالا را وارد کن و وصل شو." else "رمز را از میزبان بگیر، وارد کن و وصل شو.",
        "به برنامه برگرد؛ خودش ادامه می‌دهد."
    ) else listOf(
        "Tap “Open Wi-Fi settings”.",
        if (name != null) "Pick “$name”." else "Pick the host phone's hotspot.",
        if (hasPass) "Enter the password above and connect." else "Get the password from the host, enter it and connect.",
        "Come back to the app; it continues by itself."
    )
    val wgWaiting get() = t("منتظر وصل شدن تو به آن وای‌فای هستم و خودکار بررسی می‌کنم…", "Waiting for you to join it; checking automatically…")
    val wgRetryAuto get() = t("تلاش دوباره برای اتصال خودکار", "Try joining automatically again")
    val wgCopy get() = t("کپی رمز", "Copy password")
    fun wgReason(r: ir.mrpc.hotshare.client.WifiReason, expected: String?, current: String?): String {
        val e = expected ?: ""
        val c = current ?: ""
        return when (r) {
            ir.mrpc.hotshare.client.WifiReason.NOT_CONNECTED ->
                t("الان به هیچ وای‌فایی وصل نیستی.", "You're not on any Wi-Fi right now.")
            ir.mrpc.hotshare.client.WifiReason.WRONG_NETWORK ->
                if (expected != null && current != null) t("الان به «$c» وصلی، ولی باید به «$e» وصل شوی.", "You're on “$c”, but you must be on “$e”.")
                else if (current != null) t("الان به «$c» وصلی و میزبان آنجا نیست.", "You're on “$c” and the host isn't there.")
                else t("وای‌فای فعلی‌ات هات‌اسپات میزبان نیست.", "Your current Wi-Fi isn't the host's hotspot.")
            ir.mrpc.hotshare.client.WifiReason.JOIN_FAILED ->
                t("اتصال خودکار به «$e» انجام نشد. رمز اشتباه است یا میزبان آن را عوض کرده، یا گوشی میزبان دور است.", "Joining “$e” automatically failed. The password is wrong or was changed by the host, or the host is out of range.")
            ir.mrpc.hotshare.client.WifiReason.NOT_HOST ->
                t("به یک وای‌فای وصلی ولی گوشی میزبان در آن پیدا نشد. مطمئن شو به هات‌اسپات میزبان وصلی و میزبان «شروع اشتراک» را زده است.", "You're on a Wi-Fi but the host phone wasn't found on it. Make sure you joined the host's hotspot and the host tapped Start sharing.")
        }
    }

    // ───────────── Host: hotspot name ─────────────
    val hostHotspotName get() = t("نام هات‌اسپات", "Hotspot name")
    val hostHotspotNotSet get() = t("ثبت نشده", "Not set")
    val hostHotspotEdit get() = t("ثبت", "Set")
    val hostHotspotHint get() = t("نام و رمز هات‌اسپات را ثبت کن تا گوشی دوم نام را ببیند و بتواند خودکار وصل شود.", "Save the hotspot name and password so the other phone sees the name and can join automatically.")
    val hostPrepare get() = t("پیش‌نیازها", "Before you start")

    // ───────────── Help screen ─────────────
    val helpWhichTitle get() = t("کدام را انتخاب کنم؟", "Which one should I pick?")
    val helpHostCardTitle get() = t("من VPN دارم و می‌خواهم بدهم", "I have a VPN and want to share it")
    val helpHostCardBody get() = t("گوشی تو «میزبان» می‌شود. لازم داری: ۱) VPN روشن ۲) بلوتوث روشن ۳) هات‌اسپات (خودکار ساخته می‌شود).", "Your phone becomes the host. You need: 1) VPN on 2) Bluetooth on 3) a hotspot (created automatically).")
    val helpGoHost get() = t("رفتن به اشتراک VPN", "Go to Share VPN")
    val helpClientCardTitle get() = t("من VPN ندارم و می‌خواهم بگیرم", "I need a VPN and want to get one")
    val helpClientCardBody get() = t("گوشی تو «دریافت‌کننده» می‌شود. لازم داری: ۱) بلوتوث و وای‌فای روشن ۲) مجوز دستگاه‌های اطراف ۳) دوستت میزبان باشد.", "Your phone becomes the receiver. You need: 1) Bluetooth and Wi-Fi on 2) Nearby devices permission 3) a friend who is hosting.")
    val helpGoClient get() = t("رفتن به دریافت VPN", "Go to Get VPN")
    val helpMethodsTitle get() = t("سه روش اتصال (فقط یکی کافی است)", "Three ways to connect (you only need one)")
    val helpMethodRadar get() = t("رادار (ساده‌ترین)", "Radar (easiest)")
    val helpMethodRadarDesc get() = t("بلوتوث را روشن بگذار؛ گوشی میزبان خودش روی رادار می‌آید. فقط لمسش کن.", "Keep Bluetooth on; the host appears on the radar by itself. Just tap it.")
    val helpMethodQr get() = t("اسکن QR", "Scan QR")
    val helpMethodQrDesc get() = t("اگر رادار چیزی پیدا نکرد: میزبان QR را نشان بدهد و تو اسکن کنی.", "If the radar finds nothing: the host shows a QR and you scan it.")
    val helpMethodCode get() = t("کد ۸ رقمی", "8-digit code")
    val helpMethodCodeDesc get() = t("اگر دوربین نداری یا QR خوانده نشد: کدی را که روی گوشی میزبان است تایپ کن.", "No camera, or the QR won't scan: type the code shown on the host phone.")
    val helpStepsTitle get() = t("قدم‌به‌قدم", "Step by step")
    val helpWhichDesc get() = t("اشتراک بدهم یا دریافت کنم؟", "Share or receive?")
    val helpMethodsDesc get() = t("رادار، QR یا کد", "Radar, QR or code")
    val helpStepsDesc get() = t("مراحل کار برای میزبان و دریافت‌کننده", "Steps for host and receiver")
    val helpWifiTitle get() = t("وای‌فای و هات‌اسپات", "Wi-Fi and hotspot")
    val helpWifiDesc get() = t("چرا باید به وای‌فای میزبان وصل شوم؟", "Why must I join the host's Wi-Fi?")
    val helpWifiBody get() = if (fa) listOf(
        "دریافت‌کننده باید روی هات‌اسپات گوشی میزبان باشد؛ بدون آن VPN نمی‌تواند وصل شود.",
        "برنامه وای‌فای‌ها را جستجو نمی‌کند. اگر روی هات‌اسپات میزبان نباشی، نام آن را نشان می‌دهد و با یک دکمه تنظیمات وای‌فای گوشی را باز می‌کند.",
        "اگر نام و رمز هات‌اسپات را داشته باشد (QR، رادار یا ذخیره‌شده در گوشی میزبان)، خودش وصل می‌شود؛ وگرنه فقط رمز را در تنظیمات بزن.",
        "وقتی به وای‌فای درست وصل شدی و به برنامه برگشتی، خودکار ادامه می‌دهد.",
        "اگر رمز عوض شود یا ارتباط قطع شود، برنامه دلیلش را با پیام مشخص نشان می‌دهد."
    ) else listOf(
        "The receiver must be on the host phone's hotspot; without it the VPN can't connect.",
        "The app doesn't scan Wi-Fi. If you're not on the host's hotspot it shows the name and opens your Wi-Fi settings with one button.",
        "If it knows the name and password (QR, radar, or saved on the host) it joins by itself; otherwise just type the password in Settings.",
        "Once you're on the right Wi-Fi and return to the app, it carries on automatically.",
        "If the password changes or the link drops, the app tells you exactly why."
    )
    val helpFaqTitle get() = t("مشکل دارم", "Something's wrong")
    val helpFaq get() = if (fa) listOf(
        "دستگاه روی رادار نمی‌آید" to "بلوتوث هر دو گوشی روشن باشد، میزبان «شروع اشتراک» را زده باشد و فاصله کم باشد. اگر باز هم نیامد از QR یا کد استفاده کن.",
        "به وای‌فای وصل نمی‌شود" to "در تنظیمات وای‌فای گوشی، همان نامی را که برنامه نشان می‌دهد انتخاب کن و رمز را دقیق بزن. مطمئن شو هات‌اسپات میزبان روشن است. اگر رمز را میزبان عوض کرده، رمز جدید را از او بگیر.",
        "وصل شدم ولی اینترنت ندارم" to "VPN روی گوشی میزبان باید خودش وصل و سالم باشد. اول روی همان گوشی تستش کن.",
        "بلوتوث یا وای‌فای خودش روشن نمی‌شود" to "اندروید اجازه‌ی روشن کردن بی‌صدا را نمی‌دهد؛ برنامه پنجره‌ی درخواست را نشان می‌دهد و تو فقط «اجازه» را بزن. «آماده‌سازی دستگاه» در منو همه را یک‌جا نشان می‌دهد.",
        "هات‌اسپات خودکار ساخته نشد" to "بعضی گوشی‌ها پشتیبانی نمی‌کنند. هات‌اسپات گوشی را از تنظیمات روشن کن؛ برنامه خودش تشخیص می‌دهد."
    ) else listOf(
        "The device doesn't show on the radar" to "Bluetooth must be on for both phones, the host must have tapped Start sharing, and you should be close. Otherwise use the QR or the code.",
        "It won't join the Wi-Fi" to "In your phone's Wi-Fi settings pick the exact name the app shows and type the password. Make sure the host's hotspot is on. If the host changed the password, ask for the new one.",
        "Connected but no internet" to "The VPN on the host phone must be connected and working. Test it on that phone first.",
        "Bluetooth or Wi-Fi won't turn on by itself" to "Android doesn't allow silent switching; the app shows the prompt and you just tap Allow. “Get your phone ready” in the menu shows everything in one place.",
        "The auto hotspot wasn't created" to "Some phones don't support it. Turn on the phone hotspot in Settings; the app detects it automatically."
    )
}

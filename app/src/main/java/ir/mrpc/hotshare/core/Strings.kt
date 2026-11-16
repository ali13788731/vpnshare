package ir.mrpc.hotshare.core

/** همه‌ی متن‌های برنامه (فارسی / انگلیسی). */
class Strings(val fa: Boolean) {
    private fun t(f: String, e: String) = if (fa) f else e

    val appName get() = t("هات‌شیر", "HotShare")
    val tagline get() = t("اشتراک امن VPN با هات‌اسپات", "Share your VPN through your hotspot")
    val designedBy get() = t("طراحی شده توسط Mr.PC", "Designed by Mr.PC")

    val chooseRole get() = t("چه کاری می‌خواهید انجام دهید؟", "What would you like to do?")
    val shareTitle get() = t("اشتراک‌گذاری VPN", "Share my VPN")
    val shareDesc get() = t("VPN گوشی شما از طریق هات‌اسپات در اختیار دیگران قرار می‌گیرد", "Let others use your phone's VPN through your hotspot")
    val receiveTitle get() = t("دریافت VPN", "Use a shared VPN")
    val receiveDesc get() = t("QR را اسکن کنید یا کد را وارد کنید", "Scan the QR code or type the code")

    val settings get() = t("تنظیمات", "Settings")
    val helpAbout get() = t("راهنما و درباره", "Help & About")

    // میزبان
    val stepHotspot get() = t("هات‌اسپات", "Hotspot")
    val hotspotOn get() = t("هات‌اسپات روشن است", "Hotspot is on")
    val hotspotOff get() = t("هات‌اسپات خاموش است", "Hotspot is off")
    val openHotspot get() = t("روشن کردن هات‌اسپات", "Turn on hotspot")
    val hotspotNote get() = t(
        "اندروید اجازه نمی‌دهد برنامه‌ها مستقیم هات‌اسپات را روشن کنند. صفحه‌ی تنظیمات باز می‌شود؛ پس از روشن کردن، برنامه خودکار متوجه می‌شود.",
        "Android doesn't let apps switch the hotspot on directly. Settings will open; the app detects it automatically once it's on.")
    val stepVpn get() = t("VPN", "VPN")
    val vpnOn get() = t("VPN فعال است", "VPN is active")
    val vpnOff get() = t("VPN پیدا نشد", "No VPN found")
    val vpnNote get() = t("اول VPN دلخواه خود را روشن کنید، وگرنه ترافیک دیگران بدون VPN خارج می‌شود.", "Turn on your VPN first, otherwise others' traffic leaves without it.")
    val startShare get() = t("شروع اشتراک", "Start sharing")
    val stopShare get() = t("توقف اشتراک", "Stop sharing")
    val sharing get() = t("در حال اشتراک‌گذاری", "Sharing now")
    val scanHint get() = t("گوشی دوم این QR را اسکن کند", "Scan this QR on the other phone")
    val codeLabel get() = t("کد اتصال", "Connection code")
    val copy get() = t("کپی", "Copy")
    val copied get() = t("کپی شد", "Copied")
    val newCode get() = t("کد جدید", "New code")
    val devices get() = t("دستگاه", "Devices")
    val upload get() = t("ارسال", "Sent")
    val download get() = t("دریافت", "Received")

    // کلاینت
    val connectTitle get() = t("اتصال به VPN", "Connect to VPN")
    val stepWifi get() = t("وای‌فای", "Wi-Fi")
    val wifiOk get() = t("به وای‌فای متصل هستید", "Connected to Wi-Fi")
    val wifiNo get() = t("به وای‌فای متصل نیستید", "Not connected to Wi-Fi")
    val wifiNote get() = t("اول به هات‌اسپات گوشی میزبان وصل شوید.", "First join the host phone's hotspot.")
    val openWifi get() = t("تنظیمات وای‌فای", "Wi-Fi settings")
    val scanQr get() = t("اسکن QR", "Scan QR")
    val orEnter get() = t("یا کد را وارد کنید", "or enter the code")
    val enterCode get() = t("کد ۸ رقمی", "8-digit code")
    val connect get() = t("اتصال", "Connect")
    val disconnect get() = t("قطع اتصال", "Disconnect")
    val connecting get() = t("در حال اتصال…", "Connecting…")
    val connected get() = t("متصل شدید", "You're connected")
    val duration get() = t("مدت اتصال", "Duration")
    val scanPrompt get() = t("QR را داخل کادر قرار دهید", "Place the QR code inside the frame")

    val errCode get() = t("کد اشتباه است.", "Wrong code.")
    val errUnreach get() = t("میزبان در دسترس نیست. مطمئن شوید به هات‌اسپات وصل هستید و میزبان اشتراک را شروع کرده است.", "Host not reachable. Make sure you're on the hotspot and the host started sharing.")
    val errNative get() = t("موتور تونل در این نسخه نصب نشده است.", "The tunnel engine isn't included in this build.")
    val errVpn get() = t("مجوز VPN داده نشد.", "VPN permission was not granted.")
    val errLost get() = t("ارتباط با میزبان قطع شد.", "Connection to the host was lost.")
    val errQr get() = t("QR نامعتبر است.", "Invalid QR code.")
    val errNoWifi get() = t("اول به وای‌فای میزبان وصل شوید.", "Join the host's Wi-Fi first.")
    val errBadCode get() = t("کد باید ۸ رقم باشد.", "The code must be 8 digits.")

    // تنظیمات
    val language get() = t("زبان", "Language")
    val theme get() = t("پوسته", "Theme")
    val themeSystem get() = t("سیستم", "System")
    val themeLight get() = t("روشن", "Light")
    val themeDark get() = t("تیره", "Dark")
    val autoStart get() = t("شروع خودکار اشتراک با روشن شدن هات‌اسپات", "Auto-start sharing when hotspot turns on")
    val keepOn get() = t("روشن ماندن صفحه هنگام اشتراک/اتصال", "Keep screen on while sharing/connected")
    val battery get() = t("مستثنی کردن از بهینه‌سازی باتری", "Exclude from battery optimization")
    val batteryDesc get() = t("برای اینکه اشتراک در پس‌زمینه قطع نشود", "So sharing isn't killed in the background")
    val regen get() = t("ساخت کد جدید", "Generate new code")
    val regenDesc get() = t("دستگاه‌های قبلی دیگر نمی‌توانند وصل شوند", "Previously paired devices will no longer connect")

    // راهنما
    val howTitle get() = t("راهنما", "How it works")
    val helpHost get() = if (fa) listOf(
        "VPN خود را روشن کنید.",
        "در حالت «اشتراک‌گذاری» هات‌اسپات را روشن کنید.",
        "QR یا کد نمایش‌داده‌شده را به گوشی دوم بدهید."
    ) else listOf(
        "Turn on your VPN.",
        "In Share mode, turn on your hotspot.",
        "Give the QR or code shown to the other phone."
    )
    val helpClient get() = if (fa) listOf(
        "به هات‌اسپات گوشی میزبان وصل شوید.",
        "در حالت «دریافت»، QR را اسکن کنید یا کد را بزنید.",
        "مجوز VPN اندروید را یک‌بار تأیید کنید؛ تمام."
    ) else listOf(
        "Join the host phone's hotspot.",
        "In Receive mode, scan the QR or type the code.",
        "Approve Android's VPN prompt once; done."
    )
    val shareHeader get() = t("برای میزبان", "For the host")
    val receiveHeader get() = t("برای دریافت‌کننده", "For the receiver")
    val privacyTitle get() = t("حریم خصوصی", "Privacy")
    val privacy get() = t(
        "هات‌شیر هیچ داده‌ای جمع‌آوری نمی‌کند، تبلیغ ندارد و به سرور خارجی وصل نمی‌شود. ترافیک فقط بین دو گوشی و با کد اتصال رد‌وبدل می‌شود. کد را فقط به افراد مورد اعتماد بدهید.",
        "HotShare collects no data, shows no ads and talks to no external server. Traffic only flows between the two phones, protected by the pairing code. Share the code only with people you trust.")
    val version get() = t("نسخه", "Version")

    // اعلان
    val notifTitle get() = t("هات‌شیر در حال اشتراک‌گذاری است", "HotShare is sharing your VPN")
    val notifText get() = t("برای توقف، دکمه‌ی زیر را بزنید", "Tap stop to end sharing")
    val notifStop get() = t("توقف", "Stop")
}

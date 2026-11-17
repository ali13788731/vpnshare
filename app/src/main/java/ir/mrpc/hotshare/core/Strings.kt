package ir.mrpc.hotshare.core

import java.util.Locale

/** All user-facing text. Persian when the system language is Persian, English otherwise. */
class Strings(val fa: Boolean) {
    companion object {
        fun system() = Strings(Locale.getDefault().language == "fa")
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
    val statusTitle get() = t("وضعیت شما", "Your status")
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
    val openHotspot get() = t("روشن کردن هات‌اسپات", "Turn on hotspot")
    val hotspotNote get() = t(
        "اندروید اجازه نمی‌دهد برنامه‌ها هات‌اسپات گوشی را مستقیم روشن کنند. صفحه‌ی تنظیمات باز می‌شود؛ پس از روشن شدن، برنامه خودکار متوجه می‌شود.",
        "Android doesn't let apps switch the phone hotspot on directly. Settings will open; the app notices automatically once it's on.")
    val vpnOn get() = t("VPN شما فعال است", "Your VPN is active")
    val vpnOff get() = t("VPN پیدا نشد", "No VPN found")
    val vpnNote get() = t("اول VPN دلخواه خود را روشن کنید، وگرنه ترافیک دیگران بدون VPN خارج می‌شود.", "Turn on your VPN first, otherwise others' traffic leaves without it.")
    val visibleOn get() = t("دستگاه‌های اطراف شما را می‌بینند", "Nearby devices can see you")
    val visibleOff get() = t("بلوتوث یا مجوز فعال نیست", "Bluetooth or permission is off")
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

    // Wi-Fi sheet
    val wifiSheetTitle get() = t("اتصال به وای‌فای میزبان", "Join the host's Wi-Fi")
    val wifiSheetSub get() = t("نام و رمز هات‌اسپات گوشی میزبان را وارد کنید", "Enter the host phone's hotspot name and password")
    val wifiWrong get() = t("اتصال انجام نشد؛ نام یا رمز را بررسی کنید.", "Couldn't join. Check the name and password.")
    val joinWifi get() = t("اتصال به وای‌فای", "Join Wi-Fi")
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

    val errCode get() = t("کد اشتباه است.", "Wrong code.")
    val errUnreach get() = t("میزبان در دسترس نیست. مطمئن شوید میزبان اشتراک را شروع کرده است.", "Host not reachable. Make sure the host has started sharing.")
    val errNative get() = t("موتور تونل در این نسخه نصب نشده یا اجرا نشد.", "The tunnel engine is missing or failed to start.")
    val errVpn get() = t("مجوز VPN داده نشد.", "VPN permission was not granted.")
    val errLost get() = t("ارتباط با میزبان قطع شد.", "Connection to the host was lost.")
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
        "VPN خود را روشن کنید.",
        "در بخش «اشتراک VPN» روی شروع اشتراک بزنید.",
        "درخواست دستگاه نزدیک را تأیید کنید یا QR/کد را به او بدهید."
    ) else listOf(
        "Turn on your VPN.",
        "Open Share VPN and tap Start sharing.",
        "Approve the nearby device, or hand over the QR / code."
    )
    val helpClient get() = if (fa) listOf(
        "در بخش «دریافت VPN» منتظر بمانید تا میزبان نزدیک پیدا شود.",
        "روی دستگاه میزبان بزنید (یا QR را اسکن کنید / کد را بزنید).",
        "اگر لازم شد رمز وای‌فای را وارد کنید و مجوز VPN اندروید را تأیید کنید."
    ) else listOf(
        "Open Get VPN and wait for a nearby host to appear.",
        "Tap the host (or scan its QR / type its code).",
        "Enter the Wi-Fi password if asked and approve Android's VPN prompt."
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
}

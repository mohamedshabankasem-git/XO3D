package com.example.xo.ui

enum class AppLanguage { ARABIC, ENGLISH }

fun translate(value: String, language: AppLanguage): String {
    if (language == AppLanguage.ENGLISH) return value
    val exact = mapOf(
        "A fresh take on the classic game" to "نسخة جديدة من اللعبة الكلاسيكية",
        "Difficulty" to "مستوى الصعوبة",
        "EASY" to "سهل",
        "MEDIUM" to "متوسط",
        "HARD" to "صعب",
        "Play vs Computer" to "العب ضد الكمبيوتر",
        "Two Players – Same Device" to "لاعبان على نفس الجهاز",
        "Bluetooth Multiplayer" to "اللعب عبر البلوتوث",
        "Arabic" to "العربية",
        "English" to "الإنجليزية",
        "Language" to "اللغة",
        "Wins" to "مرات الفوز",
        "Player X" to "اللاعب X",
        "Player O" to "اللاعب O",
        "You: X    •    Computer: O" to "أنت: X    •    الكمبيوتر: O",
        "Player X    •    Player O" to "اللاعب X    •    اللاعب O",
        "Match finished" to "انتهت المباراة",
        "Tap an empty tile to make your move." to "اضغط على مربع فارغ للعب",
        "Restart" to "إعادة اللعب",
        "Main Menu" to "القائمة الرئيسية",
        "Exit game?" to "الخروج من اللعبة؟",
        "Do you want to exit the game?" to "هل تريد الخروج من اللعبة؟",
        "Exit" to "خروج",
        "Stay" to "البقاء",
        "MATCH COMPLETE" to "انتهت المباراة",
        "X wins" to "فاز اللاعب X",
        "O wins" to "فاز اللاعب O",
        "It's a draw" to "تعادل",
        "Play Again" to "العب مرة أخرى",
        "Bluetooth multiplayer" to "اللعب الجماعي عبر البلوتوث",
        "Pairing is optional: nearby devices can now be discovered here." to "الاقتران اختياري: يمكنك البحث عن الأجهزة القريبة من هنا.",
        "Permission granted. Search for a nearby device or create a game." to "تم منح الإذن. ابحث عن جهاز قريب أو أنشئ لعبة.",
        "Bluetooth permissions are required to discover and connect to devices." to "يلزم السماح بالبلوتوث للبحث عن الأجهزة والاتصال بها.",
        "Turn on Bluetooth, then try again." to "شغّل البلوتوث ثم حاول مرة أخرى.",
        "Waiting for the other player to connect…" to "في انتظار اتصال اللاعب الآخر…",
        "Could not create the Bluetooth game." to "تعذر إنشاء لعبة عبر البلوتوث.",
        "The phone must be discoverable so the other player can find it." to "يجب جعل الهاتف قابلاً للاكتشاف حتى يتمكن اللاعب الآخر من العثور عليه.",
        "This device does not support Bluetooth." to "هذا الجهاز لا يدعم البلوتوث.",
        "Turn on Bluetooth, then search again." to "شغّل البلوتوث ثم أعد البحث.",
        "Searching for nearby Bluetooth devices…" to "جارٍ البحث عن أجهزة بلوتوث قريبة…",
        "Could not start discovery. Turn Bluetooth off and on, then retry." to "تعذر بدء البحث. أوقف البلوتوث ثم شغّله وحاول مجددًا.",
        "Search finished. No new devices found; try again or choose a paired device." to "انتهى البحث دون العثور على أجهزة جديدة. حاول مرة أخرى أو اختر جهازًا مقترنًا.",
        "Available devices" to "الأجهزة المتاحة",
        "Refresh" to "تحديث",
        "No devices yet. Make the other phone discoverable, then search." to "لا توجد أجهزة بعد. اجعل الهاتف الآخر قابلاً للاكتشاف ثم ابحث.",
        "Create game • Make this phone discoverable" to "إنشاء لعبة • اجعل هذا الهاتف قابلاً للاكتشاف",
        "Searching…" to "جارٍ البحث…",
        "Search nearby devices" to "البحث عن أجهزة قريبة",
        "Selected" to "محدد",
        "Join selected device" to "الاتصال بالجهاز المحدد",
        "Back" to "رجوع",
        "Reconnect" to "إعادة الاتصال",
        "Connection lost • Tap reconnect" to "انقطع الاتصال • اضغط لإعادة الاتصال",
        "The other device did not start an XO3D game." to "الجهاز الآخر لم يبدأ لعبة XO3D.",
        "Invalid player handshake." to "تعذر التحقق من اتصال اللاعب الآخر.",
        "The host did not send the current match state." to "لم يرسل الجهاز المضيف حالة المباراة الحالية.",
        "Invalid match state." to "بيانات المباراة غير صالحة.",
        "Could not connect to the selected device." to "تعذر الاتصال بالجهاز المحدد.",
        "Could not start discovery. Turn Bluetooth off and on, then retry." to "تعذر بدء البحث. أوقف البلوتوث ثم شغّله وحاول مجددًا.",
        "Bluetooth device" to "جهاز بلوتوث",
        "Paired" to "مقترن",
        "Nearby" to "قريب",
        "Your turn" to "دورك",
        "Opponent turn" to "دور الخصم",
        "Connection lost" to "انقطع الاتصال",
        "Reconnecting…" to "جارٍ إعادة الاتصال…",
        "Computer is thinking…" to "الكمبيوتر يفكر…",
        "Draw" to "تعادل"
    )
    exact[value]?.let { return it }
    if (value.startsWith("Found ") && value.contains(" nearby device(s). Select one to connect.")) {
        val count = value.removePrefix("Found ").substringBefore(" nearby")
        return "تم العثور على $count جهاز قريب. اختر جهازًا للاتصال."
    }
    if (value.startsWith("Connecting to ")) return "جارٍ الاتصال بـ " + value.removePrefix("Connecting to ").removeSuffix("…") + "…"
    if (value.startsWith("Player ") && value.endsWith("'s turn")) return "دور اللاعب " + value.removePrefix("Player ").removeSuffix("'s turn")
    if (value.endsWith(" wins!")) return "فاز " + value.removeSuffix(" wins!")
    if (value.startsWith("Connection lost")) return "انقطع الاتصال • اضغط لإعادة الاتصال"
    if (value.startsWith("Paired • ")) return "مقترن • " + value.removePrefix("Paired • ")
    if (value.startsWith("Nearby • ")) return "قريب • " + value.removePrefix("Nearby • ")
    return "حدث خطأ غير متوقع. حاول مرة أخرى."
}

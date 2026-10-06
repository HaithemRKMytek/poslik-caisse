#!/usr/bin/env bash
# Scénario de bout en bout sur émulateur Android, contre les émulateurs Firebase lancés par la CI.
# Deux lancements de l'instrumentation, donc deux processus : la phase 2 vérifie le redémarrage.
set -uo pipefail

OUT=e2e
APP=com.poslik.caisse
RUNNER="$APP.test/$APP.FirebaseEmulatorRunner"
CLASS="$APP.e2e.CaisseEndToEndTest"
mkdir -p "$OUT"

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb logcat -c
echo "Heure hôte : $(date -u '+%F %T')   heure appareil : $(adb shell date -u '+%F %T')"

run_phase() {
  adb shell am instrument -w -e firebaseEmulator true -e class "$CLASS#$1" "$RUNNER" | tee "$OUT/$1.txt"
  grep -q "OK (1 test)" "$OUT/$1.txt"
}

status=0
run_phase phase1_onlineOfflineAndPrinterFailure || status=1
if [ "$status" -eq 0 ]; then
  adb shell am force-stop "$APP"
  run_phase phase2_failedTicketReprintedAtStartup || status=1
fi

adb pull "/sdcard/Android/data/$APP/files/e2e" "$OUT/captures" || true
adb logcat -d > "$OUT/logcat.txt" || true
echo "----- Journal de l'app (synchro, impression, Firebase, plantages) -----"
grep -E "SyncWorker|FakeTicketPrinter|FirebaseAuth|PersistentConnection|Connection|WebSocket|AuthTokenProvider|AndroidRuntime|FATAL" "$OUT/logcat.txt" | grep -v "WM-" | cut -c1-400 | head -250 || true
exit "$status"

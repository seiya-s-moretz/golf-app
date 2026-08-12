/**
 * `ConsoleSmsSender`（`functions/src/modules/auth/sms/ConsoleSmsSender.ts`）が
 * `firebase-functions/logger`経由で出力するOTPコードをテストから読み取るためのキャプチャ機構。
 *
 * `firebase-functions/logger`は初期化時点の`console.info`参照（`UNPATCHED_CONSOLE.info`）を固定的に
 * 保持するため、ロガーモジュールが最初にrequireされる前（＝`setupFiles`実行時点）に`console.info`を
 * 差し替えておく必要がある。`jest.config.js`の`setupFiles`は`app.ts`等のimportより先に実行されるため、
 * ここで安全にフックできる。`functions/src`側の実装は一切変更しない（ログ出力を横取りするだけ）。
 */
const capturedLogs: string[] = [];
const originalConsoleInfo = console.info.bind(console);

console.info = (...args: unknown[]): void => {
  capturedLogs.push(args.map((a) => (typeof a === "string" ? a : JSON.stringify(a))).join(" "));
  originalConsoleInfo(...(args as []));
};

export function clearCapturedLogs(): void {
  capturedLogs.length = 0;
}

/**
 * 指定した電話番号宛の直近のOTPコード（6桁）をログから抽出する。
 * `ConsoleSmsSender`の出力形式（`【ゴルフマッチング】確認コード: 123456（5分間有効）`、
 * `auth.service.ts`の`requestPhoneOtp`参照）が変わった場合はこのヘルパーの更新が必要。
 */
export function extractLatestOtpCode(phoneNumber: string): string {
  for (let i = capturedLogs.length - 1; i >= 0; i -= 1) {
    const line = capturedLogs[i];
    if (line.includes(phoneNumber)) {
      const match = /確認コード[:：]\s*(\d{6})/.exec(line);
      if (match) return match[1];
    }
  }
  throw new Error(
    `OTPコードのログが見つかりません（phoneNumber=${phoneNumber}）。ConsoleSmsSenderの出力形式が変わっていないか確認してください。`
  );
}

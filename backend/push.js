// Real-time push notifications through Firebase Cloud Messaging (FCM).
//
// The server sends to the device registration tokens each user's app registers via
// POST /api/users/me/devices. Sending is enabled only when a Firebase service account is supplied:
//   FIREBASE_SERVICE_ACCOUNT_BASE64  base64 of the service-account JSON (preferred for hosts), or
//   GOOGLE_APPLICATION_CREDENTIALS    path to the JSON file.
// Without either, every send is a logged no-op, so the API runs normally without Firebase.
// Reference: https://firebase.google.com/docs/cloud-messaging/send-message (accessed 2026-11-05).

let sender = null; // async ({ tokens, title, body, data }) => { successTokens, invalidTokens }
let configured = false;

export async function initPush(logger) {
  const encoded = process.env.FIREBASE_SERVICE_ACCOUNT_BASE64;
  if (!encoded && !process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    logger.info("Push notifications disabled: no Firebase service account configured");
    return false;
  }
  const admin = (await import("firebase-admin")).default;
  const credential = encoded
    ? admin.credential.cert(JSON.parse(Buffer.from(encoded, "base64").toString("utf8")))
    : admin.credential.applicationDefault();
  admin.initializeApp({ credential });
  const messaging = admin.messaging();
  sender = async ({ tokens, title, body, data }) => {
    const result = await messaging.sendEachForMulticast({
      tokens,
      notification: { title, body },
      data,
      android: { priority: "high", notification: { channelId: "nexus_updates" } },
    });
    const invalidTokens = [];
    result.responses.forEach((response, index) => {
      const code = response.error?.code;
      if (code === "messaging/registration-token-not-registered" || code === "messaging/invalid-argument") {
        invalidTokens.push(tokens[index]);
      }
    });
    return { sent: result.successCount, invalidTokens };
  };
  configured = true;
  logger.info("Push notifications enabled (Firebase Cloud Messaging)");
  return true;
}

export const isPushConfigured = () => configured;

/** Test hook: replaces FCM with a recording stub. */
export function setPushSender(fn) {
  sender = fn;
  configured = Boolean(fn);
}

/**
 * Sends a notification to every registered device of `user`, except `excludeToken` (the device
 * that caused the change). Invalid tokens are pruned from the user record. Never throws.
 */
export async function notifyUser(user, { title, body, data = {} }, { excludeToken, logger } = {}) {
  const tokens = (user.deviceTokens || []).filter((token) => token !== excludeToken);
  if (!configured || !sender || tokens.length === 0) return { sent: 0, configured };
  try {
    const { sent, invalidTokens } = await sender({ tokens, title, body, data });
    if (invalidTokens?.length) {
      user.deviceTokens = user.deviceTokens.filter((token) => !invalidTokens.includes(token));
    }
    logger?.info({ userId: user.id, sent }, "Push notification sent");
    return { sent, configured };
  } catch (error) {
    logger?.error({ err: error, userId: user.id }, "Push notification failed");
    return { sent: 0, configured };
  }
}

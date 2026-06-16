/*
 * OPTIONAL — Push notifications when the app is closed.
 *
 * Live in-app updates work WITHOUT this. Deploy this only if you want
 * WhatsApp-style notifications while the app is in the background/closed.
 *
 * Prerequisites:
 *   - Upgrade the Firebase project to the Blaze plan (still free within quotas).
 *   - Install the Firebase CLI:   npm install -g firebase-tools
 *   - From this project folder:   firebase login
 *                                 firebase init functions   (choose JS, use existing project)
 *                                 (replace the generated functions/index.js with this file)
 *                                 firebase deploy --only functions
 *
 * What it does: when a new message document is created under any chat, it sends
 * an FCM push to every other member of that chat using the fcmToken stored on
 * their /users/{uid} document.
 */

const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

exports.notifyOnMessage = onDocumentCreated(
  "chats/{chatId}/messages/{msgId}",
  async (event) => {
    const msg = event.data && event.data.data();
    if (!msg) return;

    const chatId = event.params.chatId;
    const db = getFirestore();
    const chatSnap = await db.collection("chats").doc(chatId).get();
    const chat = chatSnap.data();
    if (!chat) return;

    const recipients = (chat.members || []).filter((uid) => uid !== msg.senderId);
    if (recipients.length === 0) return;

    // Collect FCM tokens for recipients.
    const tokens = [];
    for (const uid of recipients) {
      const u = await db.collection("users").doc(uid).get();
      const token = u.exists ? u.data().fcmToken : null;
      if (token) tokens.push(token);
    }
    if (tokens.length === 0) return;

    const title = chat.type === "group"
      ? `${msg.senderName} • ${chat.name}`
      : msg.senderName;
    const body =
      msg.type === "text" ? msg.text :
      msg.type === "image" ? "📷 Photo" :
      msg.type === "video" ? "🎥 Video" : "📎 " + (msg.mediaName || "File");

    await getMessaging().sendEachForMulticast({
      tokens,
      notification: { title, body },
      data: { chatId, title, body },
      android: { priority: "high" },
    });
  }
);

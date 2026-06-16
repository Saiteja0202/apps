# FriendsChat — Firebase setup (one-time, ~10 minutes)

The app is built, but it needs **your own free Firebase project** to actually send
messages over the internet. Do these steps once, then either rebuild the app or
send me the 5 config values and I'll rebuild it for you.

---

## 1. Create the Firebase project
1. Go to https://console.firebase.google.com → **Add project** → name it (e.g. `FriendsChat`).
2. You can disable Google Analytics (not needed). Create the project.

## 2. Register the Android app
1. In the project, click the **Android** icon ("Add app").
2. **Android package name:** `com.friendschat.app`  ← must match exactly.
3. Nickname/SHA-1: optional, skip. Click **Register app**.
4. Download **`google-services.json`** (you only need it to read 5 values — see step 5).
5. Skip the remaining SDK instructions (the app is already wired up).

## 3. Turn on the two free services
In the left sidebar (both are free on the **Spark** plan — no card):
- **Build → Authentication → Get started → Sign-in method → Email/Password → Enable → Save.**
- **Build → Firestore Database → Create database → Start in *production mode* → pick a region → Enable.**

> Do **NOT** enable Firebase **Storage** — it now requires the paid Blaze plan.
> Media (photos/videos/files) is hosted free on **Cloudinary** instead (step 6).
> You can ignore `storage.rules` in this folder; it's not used anymore.

## 4. Paste in the Firestore security rules
**Firestore → Rules tab**, replace everything with the contents of
[`firestore.rules`](firestore.rules) → **Publish**.

## 5. Put your config into the app
Open the downloaded `google-services.json` and copy these values into
[`app/src/main/res/values/firebase.xml`](app/src/main/res/values/firebase.xml):

| In google-services.json                 | → string in firebase.xml |
|------------------------------------------|--------------------------|
| `client[0].client_info.mobilesdk_app_id` | `google_app_id`          |
| `client[0].api_key[0].current_key`       | `google_api_key`         |
| `project_info.project_number`            | `gcm_defaultSenderId`    |
| `project_info.project_id`                | `project_id`             |
| `project_info.storage_bucket`            | `google_storage_bucket`  |

(The `google_storage_bucket` value is no longer used — leave it as-is.)

## 6. Set up Cloudinary for free media hosting (no card)
1. Sign up at https://cloudinary.com (free "Programmable Media" plan — no credit card).
2. On the **dashboard**, copy your **Cloud name** (e.g. `dxxxxxx`).
3. **Settings (gear) → Upload → Upload presets → Add upload preset**:
   - Set **Signing Mode = Unsigned**.
   - Note the preset **name** (e.g. `friendschat_unsigned`).
   - Save.
4. Put both values into
   [`app/src/main/res/values/cloudinary.xml`](app/src/main/res/values/cloudinary.xml):
   `cloudinary_cloud_name` and `cloudinary_upload_preset`.

Then rebuild the APK (see README), or just send me the **5 Firebase values + 2
Cloudinary values** and I'll rebuild it for you.

> Free Cloudinary limits: ~25 GB storage / 25 GB monthly bandwidth, unsigned
> uploads up to 10 MB (images) / 100 MB (video) each — ample for a friends circle.

---

## How your friends use it
1. Everyone installs the same APK on their Android phone.
2. Each person taps **Create an account** (display name + email + password).
3. Tap the chat button → pick a friend for a 1-on-1, or switch to **Group** mode,
   name the group, tick members, and **Create group**.
4. Send text, photos, videos, and files (paperclip → Photo / Video / File).
   Messages and media sync live through your Firebase project.

> Free-tier limits (Spark plan) are generous for a friends circle: 1 GiB Firestore
> storage, 5 GB Cloud Storage, 50K reads/day. Plenty for a small group.

---

## (Optional) Push notifications when the app is closed
Live updates work whenever the app is open. To get WhatsApp-style notifications
when the app is **closed**, deploy the small Cloud Function in
[`functions/index.js`](functions/index.js). This requires upgrading to the
**Blaze** (pay-as-you-go) plan — it stays free within the same free quotas, it
just needs a billing account attached. Steps are in that file's header comment.

# GenZ — Promotion kit (advanced)

High-res, brand-matched social assets. Design language: **aurora mesh gradients, film
grain, glassmorphism, neon glow, a phone-mockup product shot, and HD motion**.
Regenerate / tweak anything with: `python _generate.py` (needs `pillow numpy imageio imageio-ffmpeg`).

## stickers/  — die-cut (white outline + drop shadow), 1024px, transparent PNG
| File | Use |
|------|-----|
| `logo_diecut_1024.png` | App logo sticker |
| `sticker_match_1024.png` | "It's a match! swipe right →" |
| `sticker_vibe_1024.png` | "Vibe check ✦ certified ✦" |
| `sticker_genz_1024.png` | "GenZ — Match. Chat. Vibe." |

> Import into any sticker-maker (Sticker.ly / Personal Stickers) to make a WhatsApp/Telegram pack — they convert to WebP.

## banners/  — high-resolution
| File | Platform / placement |
|------|----------------------|
| `instagram_post_2048.png` | IG/FB feed — hero (logo + rating/now-live chips + CTA) |
| `instagram_post_mockup_2048.png` | IG/FB feed — phone product shot |
| `instagram_story_1440x2560.png` | IG/FB Stories & Reels cover |
| `whatsapp_status_1440x2560.png` | WhatsApp Status |
| `facebook_1920x1008.png` | Facebook share / link card (landscape mockup) |
| `x_1920x1080.png` | X / Twitter (landscape mockup) |
| `linkedin_1920x1004.png` | LinkedIn post |
| `ig_carousel_1/2/3_2048.png` | **3-slide Instagram carousel** (hook → features → CTA) |

## videos/  — real HD MP4 (H.264, looping motion: drifting mesh, floating hearts, heartbeat logo, kinetic text)
| File | Use |
|------|-----|
| `promo_square_1080.mp4` (~13 MB, 5s) | Feed / in-stream |
| `promo_story_1080x1920.mp4` (~25 MB, 5s) | Stories / Reels / Status |

> WhatsApp **Status** caps video at ~16 MB. If the story clip is rejected, shrink it:
> `ffmpeg -i promo_story_1080x1920.mp4 -crf 28 -vf scale=720:1280 out.mp4`

## Suggested captions
- **IG / FB:** "Your circle is closer than you think. 💜 Match. Chat. Vibe. — GenZ #GenZapp #dating #makefriends"
- **LinkedIn:** "Introducing GenZ — a dating & friends app built for how our generation actually connects. Meet people in your city. Now live."
- **WhatsApp Status / X:** "On GenZ now — come find me 👀💜"
- **Carousel:** Slide 1 hook → Slide 2 features → Slide 3 "Download GenZ, it's free." CTA.

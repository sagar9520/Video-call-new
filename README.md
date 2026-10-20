# Video-call-new
Auto reply

# ChatSetu Live (com.myfuturecode.chatsetu)

Screens: Splash → Sign-up (18+ check) → Discover / Match / Messages / Me, host profile, chat (text, photo, voice, video),
video call (host video + aapka front camera, per-minute gems), gifts, recharge (6 packs), daily check-in, "gems kam" popup,
purchase history, block/report, account delete.

## Sab content server se (abhi demo)
`sagar_apps/com.myfuturecode.chatsetu/data.json`, format = `app/src/main/assets/default_config.json`.
- `hosts`: `id, name, age, city, lang, bio, tags, photos[] (image URLs), video (call me chalne wali video URL), online, rate (💎/min), color, virtual, script[]`
  - `virtual: true` (default) par "Virtual" tag dikhta hai. Real host aane par `false` karna.
- `chat_script`: host ke messages ki line — `type` = text / image / voice / video, `text`, `url`, `duration`, `delay`. Host-wise `script` bhi de sakte ho.
- `gifts`, `packs` (`id` = Play Console product id, `gems`, `price`, `off`, `cashfree_url`), `checkin` (7 din), `msg_cost`, `filter_cost`, `rewarded_gems`, `welcome_gems`
- `payment_mode`: `play` / `cashfree` / `both`. `verify_url`: Cashfree order check — app `verify_url?order_id=..&user_id=..&pack=..` bulata hai,
  jawab `{"status":"paid"}` ho to gems milte hain.
- `ads`, `privacy_url`, `cross_promo` (proxy app jaisa)

## Play policy dhyan
- Gems = digital goods: Play Billing zaroori. Cashfree sirf India "User Choice Billing" enroll karke, Play ke saath.
- Fake incoming calls, real-person ban ke scripted chat, sexual text/photos mat daalna: account ban ka risk.
- Content rating 18+, Data safety form me camera + purchase history declare karo.

## Build secrets
Proxy app jaise: `CONFIG_APP_KEY`, `ADMOB_APP_ID`, `APPLOVIN_SDK_KEY`.

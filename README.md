# 📱 KernelSung-Builder

An Android app that automates custom kernel compilation for Samsung devices to always keep your builder version up to date, using **GitHub Actions** as the build engine and the repository **[WildKernels/Samsung_KernelSU_SUSFS](https://github.com/WildKernels/Samsung_KernelSU_SUSFS)** as the source code base.

With it, you don't need a PC, WSL, ARM toolchain, or any Linux environment set up — all you need is a GitHub account and a phone with the app installed.

---

<!-- ═══════════════════════════════════════════════════════════════ -->
<!--                    ⚠️  IMPORTANT NOTICE  ⚠️                    -->
<!-- ═══════════════════════════════════════════════════════════════ -->


<h1 align="center">
  <img src="https://img.shields.io/badge/FLASHING%20A%20KERNEL%20CAN%20BRICK%20YOUR%20PHONE-critical?style=for-the-badge&labelColor=black&color=red" alt="Can Brick Your Phone">
</h1>

> ## 🔴 **READ CAREFULLY**
>
> ### ⚠️ **THIS APP AND THE KERNELS IT BUILDS CAN DAMAGE YOUR PHONE.**
>
> - **Flashing the wrong kernel can "brick" your device** (black screen / won't boot).
>
> ### **DISCLAIMER OF LIABILITY**
>
> - **I AM NOT RESPONSIBLE FOR ANY DAMAGE CAUSED TO YOUR DEVICE.**
>

> - **Use at your own risk.**
>
> ### ✅ **MANDATORY RECOMMENDATIONS BEFORE YOU CONTINUE**
>
> 1. 💾 **Save the original `boot.img`** from your stock firmware somewhere safe.
> 2. 🧰 **Have Odin + stock firmware** downloaded on your PC, ready for emergencies.
> 3. 📖 **Read this entire README** before touching any setting.
> 4. 🧠 **Only continue if you know what you're doing.**
>

<h1 align="center">
  <img src="https://img.shields.io/badge/%E2%9A%A0%EF%B8%8F%20USE%20AT%20YOUR%20OWN%20RISK%20%F0%9F%9A%A8-red?style=for-the-badge&labelColor=black&color=darkred" alt="Use at your own risk">
</h1>

---

<!-- ═══════════════════════════════════════════════════════════════ -->
<!--                       END OF NOTICE                             -->
<!-- ═══════════════════════════════════════════════════════════════ -->



## 🚀 What the app does

**KernelSU Next Builder** automates the heavy lifting of compiling a kernel with the following features:

| Feature | What it is |
|---|---|
| **KernelSU-Next** | Kernel-based root (does not modify `/system`) |
| **SUSFS** | Module that hides root, modules and mounts from root-detecting apps |
| **ZeroMount** | More aggressive file hiding (optional) |
| **NoMount** | Hides the KernelSU module mount directory |
| **BBG (Baseband Guard)** | Protects against modem/baseband tampering |
| **Unicode Fix** | Fixes filter bypasses using Unicode characters |
| **Droidspace** | Linux container support on Android |
| **NTSync** | NT synchronization (improves performance in games/Proton/Wine) |
| **BBRv3** | Low-latency TCP congestion control algorithm |
| **IPv6 NAT** | IPv6 NAT support (useful for hotspots) |
| **TTL** | TTL manipulation (share internet without carrier blocking) |
| **Optimization Patches** | WildKernels performance patches |
| **Build cache** | Reuses objects between builds to speed things up |
| **LTO** | Link-Time Optimization (smaller/faster binary) |

And in the end it delivers **two ready-to-use outputs**:

- 📦 **AnyKernel3.zip** — to install via TWRP / KernelSU app / Kernel Flasher
- 📦 **boot.tar.md5** — to install via **Odin** (PC) in Download Mode

---

## 🧰 Before you start

You'll need:

- ✅ A **Client ID** from a GitHub OAuth App that **you'll create yourself** (explained below)
- ✅ An original **boot.img** from your device (optional — only if you want the `boot.tar.md5` for Odin)

> 💡 **Important:** the app does **NOT use a pre-made personal token**. It asks you to paste a **Client ID** from an OAuth App created on your own GitHub account. That guarantees only **you** control access.

---

## 🔑 Creating your OAuth App on GitHub

The **Client ID** is the public key the app uses to identify itself to GitHub when you log in. You need to create an OAuth App on your account — it takes 2 minutes.

### Step by step

1. Go to: **[https://github.com/settings/developers](https://github.com/settings/developers)**

2. Click the **OAuth Apps** tab → **New OAuth App** button

3. Fill it in **exactly like this** (names and URLs can be anything, but the fields below matter):

   | Field | What to enter |
   |---|---|
   | **Application name** | `KernelSung-Builder` |
   | **Homepage URL** | `https://github.com/YOUR_USERNAME` _(replace with your username)_ |
   | **Application description** | _(optional)_ `Samsung kernel compiler` |
   | **Authorization callback URL** | `ksunbuilder://oauth` |
   | **Enable Device Flow** | ☑️ **Check this option** |

4. Click **Register application**

5. On the next screen you'll see:

   - **Client ID** → format `Ov23li...`  
     **COPY this value.** It's what you'll paste into the app.

   - **Client secrets** → **NOT needed** for this app. **Ignore this section.**

> ⚠️ **Never share your Client Secret** (it's not the Client ID). The Client Secret is **not used** by this app.

---

## 📲 Logging into the app (with the Client ID)

On the app's **login screen**, you'll find the fields:

Tap **Login / Connect**.

The app will:
1. Open the browser at GitHub's authorization page
2. Ask you to review the permissions (scopes `repo` + `workflow`)
3. You tap **Authorize**
4. GitHub redirects back to the app via `ksunbuilder://oauth`
5. The app exchanges the code for a **temporary token** and takes you to the main screen

Done — you're logged in. The token is stored encrypted on the device.

> 🔎 **To revoke access later:** go to [https://github.com/settings/applications](https://github.com/settings/applications) and click **Revoke access** next to "KernelSung-Builder".

---

## 🗂 Configuring the repository

On the main screen, the app shows the tab:

- **Single Device** → build for a single model (the mode available in this version)
- ~~Custom~~ → removed in this version

From then on, every time you trigger a build, it appears at:
**`https://github.com/YOUR_USERNAME/YOUR_REPO/actions`**

---

## ⚡ Triggering a build (Single Device)

On the **Single Device** tab, fill in:

| Field | Example | Description |
|---|---|---|
| **Device model** | `SM-SXXXXX` | If automatically detected |
| **Device branch** | `SM-SXXXX-Oneui7` | Corresponding branch in the WildKernels repository |
| **boot.img (optional)** | URL | Only needed if you want the `.tar.md5` for Odin |

Toggle the switches on/off according to what you want in the kernel (see the section below).

Tap **🚀 Build**.

The app:
1. Triggers the workflow via the GitHub API
2. Shows status in real time (queued → running → completed)
3. When it finishes, shows buttons to **download the artifacts**

---

## ⚙️ Understanding each option

### 🟢 Root & Hiding

| Option | Default | What it does |
|---|---|---|
| **ksun** (KernelSU-Next) | ✅ On | Base root. Without it, the kernel has no root. |
| **susfs** | ✅ On | Hides root/modules/mounts from detector apps (banks, games, etc.) |
| **zeromount** | ❌ Off | Extra aggressive hiding (may conflict with ROMs) |
| **nomount** | ✅ On | Hides the `/data/adb/modules` directory from KernelSU |

### 🟡 Compatibility extras

| Option | Default | What it does |
|---|---|---|
| **bbg** | ✅ On | Baseband Guard — protects against modem tampering |
| **unicodefix** | ✅ On | Fixes filter bypasses with Unicode characters |
| **droidspace** | ✅ On | Container support (Ubuntu/Debian) on Android |

### 🔵 Network & Performance

| Option | Default | What it does |
|---|---|---|
| **ntsync** | ✅ On | NT synchronization (games, emulators) |
| **bbrv3** | ✅ On | Better TCP on mobile networks |
| **ipv6_nat** | ✅ On | IPv6 NAT (hotspot over IPv6) |
| **ttl** | ✅ On | TTL manipulation (bypasses carrier tethering blocks) |
| **optimization** | ✅ On | WildKernels performance patches |
| **cache** | ✅ On | Reuses objects between builds (next build is faster) |

> ⚠️ **If you don't know what it does, leave it as is.** The defaults are community-recommended.

---

## 📦 Generated outputs

At the end of the build, the app automatically downloads from the GitHub **Artifacts** section:

### 1. `AnyKernel3-<model>.zip`

- Installation via **TWRP**, **Kernel Flasher** or **Horizon Kernel Flasher**
- No PC needed
- Recommended for daily use

### 2. `boot-<model>-odin.tar.md5` _(only if you provided a boot.img)_

- Installation via **Odin** on PC
- Boot into **Download Mode** on the phone and flash in **AP**
- Recommended when AnyKernel3 fails or when you want a clean flash

---

## 📥 Installing the kernel

> ⚠️ **WARNING:** before installing, read the notice at the top of this page again. Kernel flashing is a risky operation.

### ✅ Method 1 — AnyKernel3 (easiest)

1. Download the `AnyKernel3-<model>.zip` from the app
2. Open one of the apps below on your phone:

   - **[Kernel Flasher](https://github.com/fatalcoder524/KernelFlasher/releases)**
   - **[Horizon Kernel Flasher](https://github.com/libxzr/HorizonKernelFlasher/releases)**
   - Or reboot into **TWRP** → Install

3. Select the ZIP and flash
4. Reboot

### 🔧 Method 2 — Odin (PC)

1. Download the `boot.tar.md5`
2. Open **Odin** on your PC
3. Place the file in **AP**
4. Put your phone in **Download Mode**:
   - Power off → hold **Volume Down + Power** (or as per your model)
5. Connect via USB and click **Start**

---

## 🔗 Useful links

### GitHub

- 🌱 **Base repository:** [https://github.com/WildKernels/Samsung_KernelSU_SUSFS/fork](https://github.com/WildKernels/Samsung_KernelSU_SUSFS)
- 🧑‍💻 **GitHub Developer Settings (create OAuth App):** [https://github.com/settings/developers](https://github.com/settings/developers)
- 🎫 **Authorized apps (revoke access):** [https://github.com/settings/applications](https://github.com/settings/applications)

### Projects that bring the kernel to life

| Project | Link |
|---|---|
| KernelSU-Next | [https://github.com/KernelSU-Next/KernelSU-Next](https://github.com/KernelSU-Next/KernelSU-Next) |
| SUSFS | [https://gitlab.com/simonpunk/susfs4ksu](https://gitlab.com/simonpunk/susfs4ksu) |
| NoMount | [https://github.com/maxsteeel/nomount](https://github.com/maxsteeel/nomount) |
| Baseband Guard | [https://github.com/vc-teahouse/Baseband-guard](https://github.com/vc-teahouse/Baseband-guard) |
| WildKernels Patches | [https://github.com/WildKernels/kernel_patches](https://github.com/WildKernels/kernel_patches) |
| AnyKernel3 | [https://github.com/WildKernels/AnyKernel3](https://github.com/WildKernels/AnyKernel3) |

### Android tools

| Tool | Link |
|---|---|
| KernelSU-Next app | [https://github.com/KernelSU-Next/KernelSU-Next/releases](https://github.com/KernelSU-Next/KernelSU-Next/releases) |
| Kernel Flasher | [https://github.com/fatalcoder524/KernelFlasher/releases](https://github.com/fatalcoder524/KernelFlasher/releases) |
| Horizon Kernel Flasher | [https://github.com/libxzr/HorizonKernelFlasher/releases](https://github.com/libxzr/HorizonKernelFlasher/releases) |
| Odin (PC) | [https://odindownload.com/](https://odindownload.com/) |
| Platform Tools (ADB/Fastboot) | [https://developer.android.com/tools/releases/platform-tools](https://developer.android.com/tools/releases/platform-tools) |

### Samsung

- 🧬 **Models supported by WildKernels:** [https://github.com/WildKernels/Samsung_KernelSU_SUSFS/branches](https://github.com/WildKernels/Samsung_KernelSU_SUSFS/branches)
  _(pick your model's branch — e.g. `SM-S928B-Oneui7`)_

---

## ❓ FAQ / Common issues

### 💥 "Invalid Client ID" or "Bad credentials"

- You pasted the **Client Secret** instead of the **Client ID**. The correct one starts with `Iv1.` or `Ov23li...` (short). The Client Secret is long and starts with `ghs_` or similar.
- You pasted with extra spaces at the start/end — clear the field and paste again.
- The OAuth App was deleted at [settings/developers](https://github.com/settings/developers) — recreate it.

### 💥 "Error 422 when triggering the build"

- You don't have Actions permission on the repository → go to **Settings → Actions → General → Allow all actions and reusable workflows**.
- The OAuth token didn't get the `workflow` scope → revoke access at [settings/applications](https://github.com/settings/applications) and log in again.

### 💥 "Error 401 / Bad credentials"

- The OAuth token expired → tap **Logout** and log in again.
- You revoked the app at [github.com/settings/applications](https://github.com/settings/applications) → log in again.

### 💥 "Authorize" opens in the browser but doesn't return to the app

- Check that the **Authorization callback URL** in your OAuth App is exactly `ksunbuilder://oauth`.
- If the app uses **Device Flow**, an 8-digit code will appear — type it in the browser and wait for the app to detect it.

### 💥 "SIGSEGV in libc.so.6" during the build

- Being fixed. If it persists, open an issue with the full run log.

### 💥 "Image not found"

- The chosen branch doesn't match the model → check [WildKernels branches](https://github.com/WildKernels/Samsung_KernelSU_SUSFS/branches).
- The build failed due to a patch → check the **build-log** in the artifacts.

### 💥 Build passes but the kernel won't boot

- Do a **cache/dalvik wipe** in TWRP before rebooting.
- If it still hangs, go back to stock via **Odin** and try a build with fewer features (turn off `zeromount`, `lto`, `optimization`).
- **Always keep the original boot.img** as a backup.

### 💥 "Where is the Custom tab?"

- The **Custom** tab was **removed** in this version. Only **Single Device** is available for now.

### 💥 Can I use my personal account?

Yes. But note: the app will **commit** to the repository every time you configure it, and will **trigger Actions** — both consume your GitHub minutes quota (for private repos). For **public repositories**, the Actions quota is unlimited and free.

### 💥 How many builds can I run per day?

- **Public:** unlimited.
- **Private:** 2,000 minutes/month on the free plan. A kernel build usually takes ~30 min → ~66 builds/month.

---

## 🛡 Legal notice

- **Flashing a kernel can brick your device.** Back up the original `boot.img` **before** doing anything.
- This project is **not affiliated with Samsung, Google, GitHub or WildKernels**.
- Use at your own risk. Nobody is responsible for damage caused by use.
- Respect the GPL licenses of the kernel components.

---

## 🙏 Credits

- **[WildKernels](https://github.com/WildKernels)** — for the code base and patches
- **[KernelSU-Next](https://github.com/KernelSU-Next)** — for the root system
- **[simonpunk](https://gitlab.com/simonpunk)** — for SUSFS
- **[pershoot](https://github.com/pershoot)** — for KernelSU-Next dev-susfs
- Samsung KernelSU community — for testing and feedback

---

<p align="center">
  Made for the Samsung modding community<br>
  <b>KernelSung-Builder</b> — build without a PC, straight from your phone.
</p>

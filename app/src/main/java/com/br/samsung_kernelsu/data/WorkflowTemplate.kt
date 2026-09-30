package com.br.samsung_kernelsu.data

object WorkflowTemplate {

    const val SOURCE_OWNER = "WildKernels"
    const val SOURCE_REPO = "Samsung_KernelSU_SUSFS"
    const val SOURCE_BRANCH = "main"

    const val SINGLE_DEVICE_WORKFLOW_FILENAME = "single-device.yml"
    const val SINGLE_DEVICE_WORKFLOW_PATH = ".github/workflows/single-device.yml"

    val SINGLE_DEVICE_CONTENT: String = """
name: Build Single Device

on:
  workflow_dispatch:
    inputs:
      device_branch:
        required: true
        type: string
      device_model:
        required: true
        type: string
      build_type:
        required: true
        type: string
        default: 'Bazel'
      release_type:
        required: true
        type: string
        default: 'Actions'
      lto:
        required: true
        type: string
        default: 'default'
      ksun:
        required: true
        type: string
        default: 'true'
      susfs:
        required: true
        type: string
        default: 'true'
      zeromount:
        required: true
        type: string
        default: 'false'
      nomount:
        required: true
        type: string
        default: 'true'
      bbg:
        required: true
        type: string
        default: 'true'
      unicodefix:
        required: true
        type: string
        default: 'true'
      droidspace:
        required: true
        type: string
        default: 'true'
      ntsync:
        required: true
        type: string
        default: 'true'
      bbrv3:
        required: true
        type: string
        default: 'true'
      cache:
        required: true
        type: string
        default: 'true'
      ipv6_nat:
        required: true
        type: string
        default: 'true'
      optimization:
        required: true
        type: string
        default: 'true'
      ttl:
        required: true
        type: string
        default: 'true'
      boot_img_url:
        required: false
        type: string
        default: ''
      boot_img_b64:
        required: false
        type: string
        default: ''

permissions:
  contents: write
  actions: write

jobs:
  build-single:
    uses: ./.github/workflows/build.yml
    secrets: inherit
    with:
      branch: ${'$'}{{ inputs.device_branch }}
      build_type: ${'$'}{{ inputs.build_type }}
      susfs: ${'$'}{{ inputs.susfs }}
      bbg: ${'$'}{{ inputs.bbg }}
      unicodefix: ${'$'}{{ inputs.unicodefix }}
      zeromount: ${'$'}{{ inputs.zeromount }}
      nomount: ${'$'}{{ inputs.nomount }}
      ksun: ${'$'}{{ inputs.ksun }}
      droidspace: ${'$'}{{ inputs.droidspace }}
      ntsync: ${'$'}{{ inputs.ntsync }}
      bbrv3: ${'$'}{{ inputs.bbrv3 }}
      cache: ${'$'}{{ inputs.cache }}
      ipv6_nat: ${'$'}{{ inputs.ipv6_nat }}
      optimization: ${'$'}{{ inputs.optimization }}
      ttl: ${'$'}{{ inputs.ttl }}
      lto: ${'$'}{{ inputs.lto }}

  # ==========================================================
  #  REPACK para Odin (opcional — só roda se boot.img for dado)
  # ==========================================================
  repackage-odin:
    needs: build-single
    runs-on: ubuntu-22.04
    if: ${'$'}{{ inputs.boot_img_url != '' || inputs.boot_img_b64 != '' }}
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Install dependencies
        run: |
          sudo apt-get update
          sudo apt-get install -y lz4 unzip zip curl

      - name: Download AnyKernel3 artifact
        uses: actions/download-artifact@v4
        with:
          path: ./artifacts

      - name: Extract Image from AnyKernel3
        run: |
          echo "=== Artifacts baixados ==="
          find ./artifacts -type f | head -20

          IMAGE=""
          for f in ${'$'}(find ./artifacts -name "*.zip" -type f); do
            echo "Extraindo: ${'$'}f"
            mkdir -p ./ak3_extract
            unzip -o "${'$'}f" -d ./ak3_extract 2>/dev/null || continue
            FOUND=${'$'}(find ./ak3_extract -name "Image" -type f | head -1)
            if [ -n "${'$'}FOUND" ]; then
              IMAGE="${'$'}FOUND"
              break
            fi
            rm -rf ./ak3_extract
          done

          if [ -z "${'$'}IMAGE" ]; then
            IMAGE=${'$'}(find ./artifacts -name "Image" -type f | head -1)
          fi

          if [ -z "${'$'}IMAGE" ]; then
            echo "❌ Image não encontrada nos artefatos"
            exit 1
          fi

          cp "${'$'}IMAGE" ./Image
          echo "✅ Image: ${'$'}IMAGE"
          ls -lh ./Image

      - name: Download boot.img
        run: |
          if [ -n "${'$'}{{ inputs.boot_img_url }}" ]; then
            curl -fSL --retry 3 -o boot.img.download "${'$'}{{ inputs.boot_img_url }}"
          elif [ -n "${'$'}{{ inputs.boot_img_b64 }}" ]; then
            echo "${'$'}{{ inputs.boot_img_b64 }}" | base64 -d > boot.img.download
          else
            echo "❌ Nenhum boot.img fornecido"; exit 1
          fi
          ls -lh boot.img.download

          MAGIC=${'$'}(od -An -tx1 -N4 boot.img.download | tr -d ' \n')
          echo "Magic bytes: ${'$'}MAGIC"

          if [ "${'$'}MAGIC" = "04224d18" ]; then
            echo "▶ Arquivo é LZ4 — descomprimindo..."
            lz4 -d -f boot.img.download boot.img
          else
            echo "▶ Arquivo NÃO é LZ4 — usando direto..."
            mv boot.img.download boot.img
          fi

          echo "=== Tipo do arquivo final ==="
          file boot.img
          ls -lh boot.img

      - name: Repack with magiskboot
        run: |
          curl -fSL --retry 3 -o magiskboot \
            "https://raw.githubusercontent.com/Uevo001/magiskboot-linux/main/x86_64/magiskboot"
          chmod +x magiskboot
          ./magiskboot --version || true

          ./magiskboot unpack boot.img
          cp ./Image kernel
          ./magiskboot repack boot.img boot_patched.img

          mv boot_patched.img boot.img
          mkdir -p odin_package
          cp boot.img odin_package/boot.img
          cd odin_package
          tar -cvf ../boot.tar boot.img
          cd ..
          md5sum -t boot.tar >> boot.tar
          mv boot.tar boot.tar.md5

          echo "=== Conteúdo do tar.md5 ==="
          tar -tvf boot.tar.md5
          ls -lh boot.tar.md5

      - name: Upload boot.tar.md5
        uses: actions/upload-artifact@v4
        with:
          name: boot-${'$'}{{ inputs.device_model }}-odin
          path: boot.tar.md5
          if-no-files-found: warn
""".trimIndent()

    // ============================================================
    //          CUSTOM (kernel WildKernels + auto-detect)
    // ============================================================

    const val CUSTOM_WORKFLOW_FILENAME = "custom-kernel.yml"
    const val CUSTOM_WORKFLOW_PATH = ".github/workflows/custom-kernel.yml"

    val CUSTOM_CONTENT: String = """
name: Custom Kernel Build

on:
  workflow_dispatch:
    inputs:
      device_model:
        required: true
        type: string
      device_branch:
        required: true
        type: string
      enable_susfs:
        required: true
        type: string
        default: 'true'
      enable_zeromount:
        required: true
        type: string
        default: 'false'
      enable_nomount:
        required: true
        type: string
        default: 'true'
      enable_bbg:
        required: true
        type: string
        default: 'true'
      enable_unicodefix:
        required: true
        type: string
        default: 'true'
      enable_droidspace:
        required: true
        type: string
        default: 'true'
      enable_ntsync:
        required: true
        type: string
        default: 'true'
      enable_bbrv3:
        required: true
        type: string
        default: 'true'
      enable_ipv6_nat:
        required: true
        type: string
        default: 'true'
      enable_optimization:
        required: true
        type: string
        default: 'true'
      enable_ttl:
        required: true
        type: string
        default: 'true'
      osrc_url:
        required: false
        type: string
        default: ''
      boot_img_url:
        required: false
        type: string
        default: ''
      boot_img_b64:
        required: false
        type: string
        default: ''

permissions:
  contents: write
  actions: write

jobs:
  build-custom:
    runs-on: ubuntu-22.04
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Free disk space
        uses: endersonmenezes/free-disk-space@v3
        with:
          remove_android: true
          remove_dotnet: true
          remove_haskell: true
          remove_tool_cache: true
          remove_swap: true

      - name: Setup more Swap (for LTO)
        uses: pierotofy/set-swap-space@master
        with:
          swap-size-gb: 25

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
          java-package: jdk
          architecture: x64

      - name: Verify JDK version
        run: |
          echo "JAVA_HOME=${'$'}JAVA_HOME"
          java -version
          javac -version

      - name: Install dependencies
        run: |
          sudo apt-get update
          sudo apt-get install -y lz4 lzop build-essential libssl-dev bc flex bison \
            dwarves git zip unzip curl wget tar xz-utils python3 python3-pip ccache cpio libelf-dev \
            clang lld llvm make gcc-aarch64-linux-gnu libncurses-dev rsync zstd
          pip3 install --quiet pyelftools jinja2 2>/dev/null || true

      # ==========================================================
      #  Clona WildKernels/Samsung_Kernels como base
      # ==========================================================
      - name: Clone WildKernels base repository
        run: |
          CONFIG_NAME="${'$'}{{ inputs.device_branch }}-custom"
          echo "CONFIG_NAME=${'$'}CONFIG_NAME" >> ${'$'}GITHUB_ENV

          echo "=== Clonando WildKernels/Samsung_Kernels branch ${'$'}{{ inputs.device_branch }} ==="
          rm -rf "${'$'}GITHUB_WORKSPACE/${'$'}CONFIG_NAME"
          git clone --depth 1 -b "${'$'}{{ inputs.device_branch }}" --single-branch \
            https://github.com/WildKernels/Samsung_Kernels.git \
            "${'$'}GITHUB_WORKSPACE/${'$'}CONFIG_NAME" || {
              echo "⚠ Branch não encontrada, tentando main..."
              git clone --depth 1 -b main --single-branch \
                https://github.com/WildKernels/Samsung_Kernels.git \
                "${'$'}GITHUB_WORKSPACE/${'$'}CONFIG_NAME"
            }

          echo "=== Estrutura clonada ==="
          ls -la "${'$'}GITHUB_WORKSPACE/${'$'}CONFIG_NAME" | head -40

      # ==========================================================
      #  [NOVO — AUTO-DETECÇÃO]
      #  Detecta:
      #   - KERNEL_VER (ex: 6.1) lendo Makefile
      #   - ANDROID_VER (ex: android14)
      #   - SUSFS_BRANCH (ex: gki-android14-6.1)
      #   - SUSFS_PATCH_FILENAME
      #   - BUILD_MODE (qual script/comando usar)
      # ==========================================================
      - name: Auto-detect kernel version and build mode
        run: |
          CONFIG_DIR="${'$'}GITHUB_WORKSPACE/${'$'}CONFIG_NAME"
          BRANCH="${'$'}{{ inputs.device_branch }}"

          echo "=== Detectando versão do kernel ==="

          # Procura Makefile em vários locais possíveis
          MAKEFILE_PATH=""
          for candidate in \
            "${'$'}CONFIG_DIR/kernel_platform/common/Makefile" \
            "${'$'}CONFIG_DIR/kernel/Makefile" \
            "${'$'}CONFIG_DIR/kernel-6.6/Makefile" \
            "${'$'}CONFIG_DIR/kernel-6.1/Makefile" \
            "${'$'}CONFIG_DIR/kernel-5.15/Makefile" \
            "${'$'}CONFIG_DIR/kernel-5.10/Makefile" \
            "${'$'}CONFIG_DIR/Makefile" ; do
            if [ -f "${'$'}candidate" ]; then
              MAKEFILE_PATH="${'$'}candidate"
              break
            fi
          done

          if [ -z "${'$'}MAKEFILE_PATH" ]; then
            echo "❌ Makefile não encontrado em ${'$'}CONFIG_DIR"
            find "${'$'}CONFIG_DIR" -maxdepth 4 -name "Makefile" | head -10
            exit 1
          fi

          VERSION=${'$'}(grep -E '^VERSION[[:space:]]*=' "${'$'}MAKEFILE_PATH" | awk '{print ${'$'}3}' | head -1)
          PATCHLEVEL=${'$'}(grep -E '^PATCHLEVEL[[:space:]]*=' "${'$'}MAKEFILE_PATH" | awk '{print ${'$'}3}' | head -1)
          KERNEL_VER="${'$'}{VERSION}.${'$'}{PATCHLEVEL}"

          echo "✅ KERNEL_VER=${'$'}KERNEL_VER (de ${'$'}MAKEFILE_PATH)"

          # Mapeia kernel → android version
          case "${'$'}KERNEL_VER" in
            5.10) ANDROID_VER="android12" ;;
            5.15) ANDROID_VER="android13" ;;
            6.1)  ANDROID_VER="android14" ;;
            6.6)  ANDROID_VER="android15" ;;
            *)    ANDROID_VER="unknown" ;;
          esac

          SUSFS_BRANCH="gki-${'$'}{ANDROID_VER}-${'$'}{KERNEL_VER}"
          SUSFS_PATCH_FILENAME="50_add_susfs_in_${'$'}{SUSFS_BRANCH}.patch"

          echo "✅ ANDROID_VER=${'$'}ANDROID_VER"
          echo "✅ SUSFS_BRANCH=${'$'}SUSFS_BRANCH"
          echo "✅ SUSFS_PATCH_FILENAME=${'$'}SUSFS_PATCH_FILENAME"

          # ----------------------------------------------------------
          # Detecta build system
          # ----------------------------------------------------------
          BUILD_MODE="unknown"

          if [[ "${'$'}BRANCH" == SM-A055* || "${'$'}BRANCH" == SM-X926B* ]]; then
            BUILD_MODE="build_kernel_sh"
          elif [[ "${'$'}BRANCH" == SM-S926* ]]; then
            BUILD_MODE="bazel_projects_s5e9945"
          elif [[ "${'$'}BRANCH" == SM-A556* ]]; then
            BUILD_MODE="bazel_projects_s5e8845"
          elif [[ "${'$'}BRANCH" == SM-S938B* ]]; then
            BUILD_MODE="build_kernel_gki_with_sed"
          elif [[ "${'$'}KERNEL_VER" == 6.* ]]; then
            if [ -f "${'$'}CONFIG_DIR/build_kernel_GKI.sh" ]; then
              BUILD_MODE="build_kernel_gki"
            fi
          fi

          # Fallbacks
          if [ "${'$'}BUILD_MODE" = "unknown" ]; then
            if [ -f "${'$'}CONFIG_DIR/build_kernel_GKI.sh" ]; then
              BUILD_MODE="build_kernel_gki"
            elif [ -f "${'$'}CONFIG_DIR/build_kernel.sh" ]; then
              BUILD_MODE="build_kernel_sh"
            elif [ -d "${'$'}CONFIG_DIR/kernel_platform" ]; then
              BUILD_MODE="bazel_common"
            fi
          fi

          echo "✅ BUILD_MODE=${'$'}BUILD_MODE"

          if [ "${'$'}BUILD_MODE" = "unknown" ]; then
            echo "❌ Não foi possível detectar o build mode para ${'$'}BRANCH"
            ls -la "${'$'}CONFIG_DIR" | head -30
            exit 1
          fi

          # Persiste vars
          {
            echo "KERNEL_VER=${'$'}KERNEL_VER"
            echo "ANDROID_VER=${'$'}ANDROID_VER"
            echo "SUSFS_BRANCH=${'$'}SUSFS_BRANCH"
            echo "SUSFS_PATCH_FILENAME=${'$'}SUSFS_PATCH_FILENAME"
            echo "BUILD_MODE=${'$'}BUILD_MODE"
            echo "BRANCH=${'$'}BRANCH"
          } >> ${'$'}GITHUB_ENV

      # ==========================================================
      #  Garante KERNEL_ONLY=1 (só para build_kernel_GKI.sh)
      # ==========================================================
      - name: Ensure KERNEL_ONLY=1 (if applicable)
        run: |
          CONFIG_DIR="${'$'}GITHUB_WORKSPACE/${'$'}CONFIG_NAME"
          BUILD_SCRIPT="${'$'}CONFIG_DIR/build_kernel_GKI.sh"

          if [ -f "${'$'}BUILD_SCRIPT" ]; then
            echo "=== Garantindo KERNEL_ONLY=1 em ${'$'}BUILD_SCRIPT ==="
            if grep -q "^export KERNEL_ONLY=" "${'$'}BUILD_SCRIPT"; then
              sed -i 's/^export KERNEL_ONLY=0/export KERNEL_ONLY=1/' "${'$'}BUILD_SCRIPT"
              echo "✅ Forçado para 1"
            else
              echo "ℹ Sem definição explícita — padrão do WildKernels já é 1"
            fi
            grep -n "KERNEL_ONLY" "${'$'}BUILD_SCRIPT" || echo "  (nada encontrado)"
          else
            echo "ℹ build_kernel_GKI.sh não existe (BUILD_MODE=${'$'}BUILD_MODE) — pulando"
          fi

      # ==========================================================
      #  Extrai TODOS os .tar.xz (clangs, lld, libLLVM, LTO, etc.)
      # ==========================================================
      - name: Extract all tarballs (clangs, lld, libLLVM, etc.)
        run: |
          cd "${'$'}GITHUB_WORKSPACE/${'$'}CONFIG_NAME"
          echo "=== Procurando e extraindo todos os .tar.xz ==="
          COUNT=0
          while IFS= read -r -d '' file; do
            echo "  ▶ Extraindo ${'$'}file"
            tar -xf "${'$'}file" || true
            rm -f "${'$'}file"
            COUNT=$((COUNT + 1))
          done < <(find . -maxdepth 2 -type f -name '*.tar.xz' -print0)
          echo "✅ Extraídos ${'$'}COUNT tarballs"

          echo "=== Estrutura após extração ==="
          ls -la "${'$'}GITHUB_WORKSPACE/${'$'}CONFIG_NAME" | head -40
          find "${'$'}GITHUB_WORKSPACE/${'$'}CONFIG_NAME" -maxdepth 4 -type d -name 'clang-r*' 2>/dev/null | head -10

      # ==========================================================
      #  [OBSOLETO] Input osrc_url mantido por compatibilidade da UI
      # ==========================================================
      - name: Skip OSRC override (kept for UI compatibility)
        run: |
          echo "ℹ OSRC input ignorado — usando source do WildKernels como base."

      - name: Set Environment Variables
        run: |
          CONFIG="${'$'}CONFIG_NAME"
          COMMON="${'$'}GITHUB_WORKSPACE/${'$'}CONFIG/kernel_platform/common"
          echo "ROOT=${'$'}GITHUB_WORKSPACE" >> ${'$'}GITHUB_ENV
          echo "CONFIG=${'$'}CONFIG" >> ${'$'}GITHUB_ENV
          echo "COMMON=${'$'}COMMON" >> ${'$'}GITHUB_ENV
          echo "PLATFORM=${'$'}GITHUB_WORKSPACE/${'$'}CONFIG/kernel_platform" >> ${'$'}GITHUB_ENV
          echo "GKI_DEFCONFIG=${'$'}COMMON/arch/arm64/configs/gki_defconfig" >> ${'$'}GITHUB_ENV
          echo "KERNEL_PATCHES=${'$'}GITHUB_WORKSPACE/kernel_patches" >> ${'$'}GITHUB_ENV
          echo "ANYKERNEL3=${'$'}GITHUB_WORKSPACE/AnyKernel3" >> ${'$'}GITHUB_ENV

          chmod -R u+w "${'$'}GITHUB_WORKSPACE/${'$'}CONFIG/kernel_platform"

      - name: Clone dependencies (kernel_patches + AnyKernel3)
        run: |
          git clone --depth=1 https://github.com/WildKernels/kernel_patches.git \
            "${'$'}GITHUB_WORKSPACE/kernel_patches"
          git clone --depth=1 -b gki-2.0 https://github.com/WildKernels/AnyKernel3.git \
            "${'$'}GITHUB_WORKSPACE/AnyKernel3"
          echo "✅ kernel_patches + AnyKernel3 clonados"

      - name: Download boot.img
        run: |
          if [ -n "${'$'}{{ inputs.boot_img_url }}" ]; then
            curl -fSL --retry 3 -o boot.img.download "${'$'}{{ inputs.boot_img_url }}"
          elif [ -n "${'$'}{{ inputs.boot_img_b64 }}" ]; then
            echo "${'$'}{{ inputs.boot_img_b64 }}" | base64 -d > boot.img.download
          else
            echo "⚠ Nenhum boot.img fornecido — seguindo sem repack Odin"
            touch boot.img.placeholder
            exit 0
          fi
          ls -lh boot.img.download

          MAGIC=${'$'}(od -An -tx1 -N4 boot.img.download | tr -d ' \n')
          echo "Magic bytes: ${'$'}MAGIC"

          if [ "${'$'}MAGIC" = "04224d18" ]; then
            echo "▶ Arquivo é LZ4 — descomprimindo..."
            lz4 -d -f boot.img.download boot.img
          else
            echo "▶ Arquivo NÃO é LZ4 — usando direto..."
            mv boot.img.download boot.img
          fi

          file boot.img
          ls -lh boot.img

      # ==========================================================
      #  1. KernelSU-Next
      # ==========================================================
      - name: Setup KernelSU-Next
        run: |
          cd "${'$'}COMMON"
          if [ "${'$'}{{ inputs.enable_susfs }}" = "true" ]; then
            curl -LSs "https://raw.githubusercontent.com/pershoot/KernelSU-Next/dev-susfs/kernel/setup.sh" | bash -s dev-susfs
          else
            curl -LSs "https://raw.githubusercontent.com/KernelSU-Next/KernelSU-Next/next/kernel/setup.sh" | bash -s dev
          fi
          ls -la drivers/kernelsu 2>/dev/null || ls -la KernelSU 2>/dev/null || true

          cd "${'$'}COMMON/KernelSU-Next"
          git fetch origin dev 2>/dev/null || true
          KSU_GIT_VERSION=${'$'}(git rev-list --count refs/remotes/origin/dev 2>/dev/null)
          KSU_GIT_TAG=${'$'}(git describe --tags --abbrev=0 refs/remotes/origin/dev 2>/dev/null || echo "v0.0.1")
          [ -z "${'$'}KSU_GIT_TAG" ] && KSU_GIT_TAG="v0.0.1"

          KSU_COMMIT=${'$'}(git rev-parse --short refs/remotes/origin/dev 2>/dev/null)
          KSU_VERSION=${'$'}((30000 + KSU_GIT_VERSION))
          cd kernel
          sed -i "s/^KSU_VERSION_FALLBACK := 1${'$'}/KSU_VERSION_FALLBACK := ${'$'}{KSU_VERSION}/" Kbuild
          sed -i "s/^KSU_VERSION_TAG_FALLBACK := v0.0.1${'$'}/KSU_VERSION_TAG_FALLBACK := ${'$'}{KSU_GIT_TAG}/" Kbuild
          echo "KSU_VERSION=${'$'}KSU_VERSION" >> ${'$'}GITHUB_ENV

      # ==========================================================
      #  2. SUSFS + patches Samsung (usando SUSFS_BRANCH dinâmico)
      # ==========================================================
      - name: Setup SUSFS
        if: ${'$'}{{ inputs.enable_susfs == 'true' }}
        run: |
          cd "${'$'}GITHUB_WORKSPACE"
          echo "=== Clonando SUSFS branch ${'$'}SUSFS_BRANCH ==="
          git clone https://gitlab.com/simonpunk/susfs4ksu.git -b "${'$'}SUSFS_BRANCH" susfs4ksu
          cd susfs4ksu
          git fetch https://gitlab.com/pershoot/susfs4ksu.git "${'$'}{SUSFS_BRANCH}-dev" 2>/dev/null || true
          git log ..FETCH_HEAD --oneline -n 2 2>/dev/null | awk '{print ${'$'}1}' | tac | xargs git cherry-pick 2>/dev/null || true

          SUSFS4KSU="${'$'}GITHUB_WORKSPACE/susfs4ksu"
          cd "${'$'}COMMON"

          echo "=== Copiando arquivos SUSFS ==="
          cp "${'$'}SUSFS4KSU/kernel_patches/fs/"* ./fs/
          cp "${'$'}SUSFS4KSU/kernel_patches/include/linux/"* ./include/linux/
          cp "${'$'}SUSFS4KSU/kernel_patches/${'$'}{SUSFS_PATCH_FILENAME}" ./

          if [ -f "include/linux/susfs_def.h" ]; then
            if ! grep -q "include <linux/cred.h>" include/linux/susfs_def.h; then
              sed -i '0,/^#ifndef/s//#include <linux\/cred.h>\n\n#ifndef/' include/linux/susfs_def.h
              echo "✅ cred.h adicionado em susfs_def.h"
            fi
          fi

          echo "=== Preparando open.c e namespace.c ==="
          sed -i '/#ifdef CONFIG_SECURITY_DEFEX/,/^#endif/d' fs/open.c
          sed -i '/copy_flags = CL_COPY_UNBINDABLE | CL_EXPIRE;/,/#endif/ {
            /#ifdef CONFIG_KDP_NS/,/#endif/ {
              /#else/,/#endif/!d
              /#else/d
              /#endif/d
            }
          }' fs/namespace.c

          echo "=== Aplicando patch principal SUSFS ==="
          patch -p1 < "${'$'}{SUSFS_PATCH_FILENAME}" || true
          rm -f "${'$'}{SUSFS_PATCH_FILENAME}"

          echo "=== Aplicando patches Samsung específicos ==="
          shopt -s nullglob
          patches=("${'$'}KERNEL_PATCHES/samsung/${'$'}BRANCH"/*.patch)
          if [ ${'$'}{#patches[@]} -gt 0 ]; then
            cp "${'$'}{patches[@]}" ./
            for p in *.patch; do
              echo "  ▶ ${'$'}p"
              patch -p1 < "${'$'}p" || true
              rm -f "${'$'}p"
            done
          fi
          shopt -u nullglob

          echo "=== Adicionando #include <linux/susfs.h> ==="
          for f in fs/proc/base.c fs/namespace.c fs/open.c fs/read_write.c fs/stat.c fs/d_path.c fs/namei.c fs/exec.c fs/statfs.c; do
            if [ -f "${'$'}f" ] && grep -qi "susfs" "${'$'}f" 2>/dev/null; then
              if ! grep -q "include <linux/susfs.h>" "${'$'}f"; then
                echo "  ⚠ Adicionando susfs.h em ${'$'}f"
                sed -i '0,/^#include/s//#ifdef CONFIG_KSU_SUSFS\n#include <linux\/susfs.h>\n#endif\n#include/' "${'$'}f"
              fi
            fi
          done

          rm -rf "${'$'}GITHUB_WORKSPACE/susfs4ksu"
          echo "✅ SUSFS + patches Samsung finalizados"

      # ==========================================================
      #  3. ZeroMount
      # ==========================================================
      - name: Add Zeromount
        if: ${'$'}{{ inputs.enable_susfs == 'true' && inputs.enable_zeromount == 'true' }}
        uses: ./.github/actions/zeromount

      # ==========================================================
      #  4. NoMount
      # ==========================================================
      - name: Setup NoMount
        if: ${'$'}{{ inputs.enable_nomount == 'true' }}
        run: |
          cd "${'$'}COMMON"
          curl -fsSL --retry 5 --retry-delay 5 \
            "https://raw.githubusercontent.com/maxsteeel/nomount/refs/heads/dev/kernel/setup.sh" \
            -o /tmp/nomount_setup.sh
          bash /tmp/nomount_setup.sh dev 2>&1 | tail -10 || true
          rm -f /tmp/nomount_setup.sh
          [ -L "fs/nomount" ] && echo "  ✅ NoMount symlink criado" || echo "  ⚠ NoMount não aplicado"

      # ==========================================================
      #  5. BBG
      # ==========================================================
      - name: Setup Baseband Guard
        if: ${'$'}{{ inputs.enable_bbg == 'true' }}
        run: |
          cd "${'$'}COMMON"
          curl -fsSL --retry 5 --retry-delay 5 \
            "https://raw.githubusercontent.com/vc-teahouse/Baseband-guard/main/setup.sh" \
            -o /tmp/bbg_setup.sh
          bash /tmp/bbg_setup.sh 2>&1 | tail -10 || true
          rm -f /tmp/bbg_setup.sh

          BBG_DIR=""
          for d in security/baseband-guard security/baseband_guard; do
            [ -f "${'$'}d/Makefile" ] && BBG_DIR="${'$'}d" && break
          done

          if [ -n "${'$'}BBG_DIR" ]; then
            echo "  ✅ BBG instalado em ${'$'}BBG_DIR"
            if ! grep -q "baseband_guard" security/Kconfig; then
              sed -i 's/\(default[^,]*\)selinux\(.*\)/\1selinux,baseband_guard\2/' security/Kconfig 2>/dev/null || true
            fi
          else
            echo "  ❌ BBG não instalado — abortando"
            exit 1
          fi

      # ==========================================================
      #  6. Unicode Fix
      # ==========================================================
      - name: Apply Unicode Fix
        if: ${'$'}{{ inputs.enable_unicodefix == 'true' }}
        run: |
          cd "${'$'}COMMON"
          git clone --depth=1 https://github.com/WildKernels/kernel_patches.git /tmp/wk_patches
          if [ "${'$'}(printf '%s\n' "5.16" "${'$'}KERNEL_VER" | sort -V | head -n1)" = "${'$'}KERNEL_VER" ]; then
            PATCH="/tmp/wk_patches/common/unicode_bypass_fix_6.1-.patch"
          else
            PATCH="/tmp/wk_patches/common/unicode_bypass_fix_6.1+.patch"
          fi
          [ -f "${'$'}PATCH" ] && patch -p1 --forward -N < "${'$'}PATCH" 2>&1 | tail -5 || true
          rm -rf /tmp/wk_patches

      # ==========================================================
      #  7. Droidspaces
      # ==========================================================
      - name: Add droidspace support
        if: ${'$'}{{ inputs.enable_droidspace == 'true' }}
        uses: ./.github/actions/droidspaces

      # ==========================================================
      #  8. NTSync
      # ==========================================================
      - name: Add ntsync
        if: ${'$'}{{ inputs.enable_ntsync == 'true' }}
        uses: ./.github/actions/ntsync

      # ==========================================================
      #  9. BBRv3
      # ==========================================================
      - name: Add bbrv3
        if: ${'$'}{{ inputs.enable_bbrv3 == 'true' }}
        uses: ./.github/actions/bbrv3

      # ==========================================================
      #  10. IPv6 NAT
      # ==========================================================
      - name: Add IP-SET and IPv6_NAT support
        if: ${'$'}{{ inputs.enable_ipv6_nat == 'true' }}
        uses: ./.github/actions/IPv6_NAT

      # ==========================================================
      #  11. TTL
      # ==========================================================
      - name: Add TTL support
        if: ${'$'}{{ inputs.enable_ttl == 'true' }}
        uses: ./.github/actions/TTL

      # ==========================================================
      #  12. Optimization
      # ==========================================================
      - name: add optimization patches
        if: ${'$'}{{ inputs.enable_optimization == 'true' && !startsWith(inputs.device_branch, 'SM-S711') }}
        uses: ./.github/actions/optimization

      # ==========================================================
      #  13. Ajustes finais de config (só gki_defconfig)
      # ==========================================================
      - name: Apply Samsung config fragments (WildKernels-style)
        run: |
          PLATFORM="${'$'}PLATFORM"
          chmod -R u+w "${'$'}PLATFORM" 2>/dev/null || true

          GKI="${'$'}GKI_DEFCONFIG"

          echo "=== Aplicando configs no gki_defconfig (base) ==="

          if [ -f "${'$'}GKI" ]; then
            "${'$'}COMMON/scripts/config" --file "${'$'}GKI" -e CONFIG_OVERLAY_FS
            "${'$'}COMMON/scripts/config" --file "${'$'}GKI" -e CONFIG_TMPFS_XATTR
            "${'$'}COMMON/scripts/config" --file "${'$'}GKI" -e CONFIG_TMPFS_POSIX_ACL
            "${'$'}COMMON/scripts/config" --file "${'$'}GKI" -e CONFIG_KALLSYMS
            "${'$'}COMMON/scripts/config" --file "${'$'}GKI" -e CONFIG_KALLSYMS_ALL
            "${'$'}COMMON/scripts/config" --file "${'$'}GKI" -e CONFIG_KPM 2>/dev/null || true

            "${'$'}COMMON/scripts/config" --file "${'$'}GKI" -e CONFIG_KSU
            if [ "${'$'}{{ inputs.enable_susfs }}" = "true" ]; then
              "${'$'}COMMON/scripts/config" --file "${'$'}GKI" -e CONFIG_KSU_SUSFS
              "${'$'}COMMON/scripts/config" --file "${'$'}GKI" -e CONFIG_KSU_SUSFS_SUS_PATH
              "${'$'}COMMON/scripts/config" --file "${'$'}GKI" -e CONFIG_KSU_SUSFS_SUS_MOUNT
            fi

            if [ "${'$'}{{ inputs.enable_nomount }}" = "true" ]; then
              "${'$'}COMMON/scripts/config" --file "${'$'}GKI" -e CONFIG_NOMOUNT 2>/dev/null || true
            fi

            if [ "${'$'}{{ inputs.enable_bbg }}" = "true" ]; then
              "${'$'}COMMON/scripts/config" --file "${'$'}GKI" -e CONFIG_BBG
              LSM_VAL=${'$'}(grep '^CONFIG_LSM=' "${'$'}GKI" | head -1 | sed 's/^CONFIG_LSM="//; s/"${'$'}//')
              [ -z "${'$'}LSM_VAL" ] && LSM_VAL="landlock,lockdown,yama,loadpin,safesetid,integrity,selinux,smack,tomoyo,apparmor,bpf"
              if ! echo "${'$'}LSM_VAL" | grep -q "baseband_guard"; then
                LSM_VAL="${'$'}LSM_VAL,baseband_guard"
              fi
              "${'$'}COMMON/scripts/config" --file "${'$'}GKI" --set-str CONFIG_LSM "${'$'}LSM_VAL"
            fi

            if [ "${'$'}{{ inputs.enable_droidspace }}" = "true" ]; then
              for cfg in CONFIG_SYSVIPC CONFIG_POSIX_MQUEUE CONFIG_IPC_NS CONFIG_PID_NS \
                         CONFIG_USER_NS CONFIG_DEVTMPFS CONFIG_NETFILTER_XT_MATCH_ADDRTYPE \
                         CONFIG_NETFILTER_XT_TARGET_REJECT CONFIG_NETFILTER_XT_TARGET_LOG \
                         CONFIG_NETFILTER_XT_MATCH_RECENT CONFIG_IP_SET CONFIG_IP_SET_HASH_IP \
                         CONFIG_IP_SET_HASH_NET CONFIG_NETFILTER_XT_SET; do
                "${'$'}COMMON/scripts/config" --file "${'$'}GKI" -e "${'$'}cfg" 2>/dev/null || true
              done
            fi

            if [ "${'$'}{{ inputs.enable_ntsync }}" = "true" ]; then
              "${'$'}COMMON/scripts/config" --file "${'$'}GKI" -e CONFIG_NTSYNC 2>/dev/null || true
            fi

            if [ "${'$'}{{ inputs.enable_bbrv3 }}" = "true" ]; then
              for cfg in CONFIG_NET_SCH_FQ CONFIG_NET_SCH_FQ_CODEL CONFIG_NET_SCH_CAKE \
                         CONFIG_NET_SCH_PIE CONFIG_NET_SCH_FQ_PIE CONFIG_TCP_CONG_ADVANCED \
                         CONFIG_TCP_CONG_BBR CONFIG_TCP_CONG_BBR3 CONFIG_TCP_CONG_CUBIC; do
                "${'$'}COMMON/scripts/config" --file "${'$'}GKI" -e "${'$'}cfg" 2>/dev/null || true
              done
              for cfg in CONFIG_TCP_CONG_BIC CONFIG_TCP_CONG_WESTWOOD CONFIG_TCP_CONG_HTCP; do
                "${'$'}COMMON/scripts/config" --file "${'$'}GKI" -d "${'$'}cfg" 2>/dev/null || true
              done
            fi

            if [ "${'$'}{{ inputs.enable_ipv6_nat }}" = "true" ]; then
              for cfg in CONFIG_IP_SET CONFIG_IP_SET_BITMAP_IP CONFIG_IP_SET_BITMAP_IPMAC \
                         CONFIG_IP_SET_BITMAP_PORT CONFIG_IP_SET_HASH_IP CONFIG_IP_SET_HASH_IPMARK \
                         CONFIG_IP_SET_HASH_IPPORT CONFIG_IP_SET_HASH_IPPORTIP \
                         CONFIG_IP_SET_HASH_IPPORTNET CONFIG_IP_SET_HASH_IPMAC \
                         CONFIG_IP_SET_HASH_MAC CONFIG_IP_SET_HASH_NETPORTNET \
                         CONFIG_IP_SET_HASH_NET CONFIG_IP_SET_HASH_NETNET \
                         CONFIG_IP_SET_HASH_NETPORT CONFIG_IP_SET_HASH_NETIFACE \
                         CONFIG_IP_SET_LIST_SET CONFIG_NETFILTER_XT_MATCH_ADDRTYPE \
                         CONFIG_NETFILTER_XT_SET CONFIG_IP6_NF_NAT CONFIG_IP6_NF_TARGET_MASQUERADE; do
                "${'$'}COMMON/scripts/config" --file "${'$'}GKI" -e "${'$'}cfg" 2>/dev/null || true
              done
              "${'$'}COMMON/scripts/config" --file "${'$'}GKI" --set-val CONFIG_IP_SET_MAX 65534 2>/dev/null || true
            fi

            if [ "${'$'}{{ inputs.enable_ttl }}" = "true" ]; then
              for cfg in CONFIG_IP_NF_TARGET_TTL CONFIG_IP6_NF_TARGET_HL CONFIG_IP6_NF_MATCH_HL; do
                "${'$'}COMMON/scripts/config" --file "${'$'}GKI" -e "${'$'}cfg" 2>/dev/null || true
              done
            fi

            echo "Nuking Samsung Knox/Root protection in: ${'$'}GKI"
            for cfg in \
              CONFIG_UH CONFIG_UH_RKP CONFIG_UH_LKMAUTH CONFIG_UH_LKM_BLOCK \
              CONFIG_RKP CONFIG_RKP_CFP_JOPP CONFIG_RKP_CFP_ROPP CONFIG_RKP_CFP \
              CONFIG_SECURITY_DEFEX CONFIG_PROCA CONFIG_FIVE CONFIG_KDP CONFIG_KDP_NS ; do
              "${'$'}COMMON/scripts/config" --file "${'$'}GKI" --disable "${'$'}cfg" 2>/dev/null || true
            done

            echo "✅ Configs aplicadas no gki_defconfig (base)"
          fi

          cd "${'$'}COMMON"
          sed -i 's/check_defconfig//' ./build.config.gki 2>/dev/null || true
          [ -f "./build.config.gki.aarch64" ] && sed -i 's/check_defconfig//' ./build.config.gki.aarch64 2>/dev/null || true

      # ==========================================================
      #  14. Build kernel (BUILD_MODE dinâmico)
      # ==========================================================
      - name: Build kernel (auto-detected build mode)
        run: |
          export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:${'$'}JAVA_HOME/bin:${'$'}PATH"
          echo "PATH: ${'$'}PATH"
          which rm && which cp && which find && which grep

          CONFIG_DIR="${'$'}GITHUB_WORKSPACE/${'$'}CONFIG_NAME"
          PLATFORM="${'$'}CONFIG_DIR/kernel_platform"

          echo "JAVA_HOME atual: ${'$'}JAVA_HOME"
          java -version

          echo "=== BUILD_MODE=${'$'}BUILD_MODE | KERNEL_VER=${'$'}KERNEL_VER | ANDROID_VER=${'$'}ANDROID_VER ==="

          cd "${'$'}CONFIG_DIR"

          # Substitui JDK 11 embutido pelo JDK 21 (se existir)
          if [ -d "${'$'}PLATFORM" ]; then
            JDK11_DIR="${'$'}PLATFORM/prebuilts/jdk/jdk11/linux-x86"
            if [ -d "${'$'}JDK11_DIR" ] && [ ! -L "${'$'}JDK11_DIR" ]; then
              rm -rf "${'$'}JDK11_DIR"
            fi
            mkdir -p "${'$'}(dirname "${'$'}JDK11_DIR")"
            ln -sfn "${'$'}JAVA_HOME" "${'$'}JDK11_DIR"
            echo "✅ Symlink JDK: ${'$'}JDK11_DIR -> ${'$'}JAVA_HOME"

            rm -rf "${'$'}PLATFORM/out/bazel"

            echo "=== Verificando clang.real ==="
            CLANG_REAL="${'$'}PLATFORM/prebuilts/clang/host/linux-x86/clang-r487747c/bin/clang.real"
            if [ -f "${'$'}CLANG_REAL" ]; then
              echo "✅ clang.real: ${'$'}CLANG_REAL"
            else
              echo "⚠ clang.real não encontrado em clang-r487747c (pode ser outro path)"
              find "${'$'}PLATFORM/prebuilts/clang/host/linux-x86" -name "clang.real" 2>/dev/null | head -5
            fi

            cd "${'$'}PLATFORM"
            rm -f bazel-bin bazel-out bazel-testlogs bazel-kernel_platform
            export SKIP_ABI_CHECK=1
            cd "${'$'}CONFIG_DIR"

            mkdir -p "${'$'}CONFIG_DIR/out/target/product/e3q"
            mkdir -p "${'$'}PLATFORM/out"
            chmod -R u+w "${'$'}PLATFORM"
          fi

          # ----------------------------------------------------------
          # Executa o build conforme BUILD_MODE
          # ----------------------------------------------------------
          set +e
          case "${'$'}BUILD_MODE" in
            build_kernel_gki)
              echo "▶ Rodando build_kernel_GKI.sh"
              chmod +x "${'$'}CONFIG_DIR/build_kernel_GKI.sh"
              "${'$'}CONFIG_DIR/build_kernel_GKI.sh" 2>&1 | tee bazel_build.log
              ;;
            build_kernel_gki_with_sed)
              echo "▶ Rodando build_kernel_GKI.sh com sed em modules.bzl"
              sed -i '/drivers\/net\/usb\/smsc75xx\.ko/d;/drivers\/net\/usb\/smsc95xx\.ko/d' \
                "${'$'}PLATFORM/common/modules.bzl" 2>/dev/null || true
              chmod +x "${'$'}CONFIG_DIR/build_kernel_GKI.sh"
              "${'$'}CONFIG_DIR/build_kernel_GKI.sh" 2>&1 | tee bazel_build.log
              ;;
            build_kernel_sh)
              echo "▶ Rodando build_kernel.sh"
              sudo apt-get install -y libyaml-dev || true
              chmod +x "${'$'}CONFIG_DIR/build_kernel.sh"
              "${'$'}CONFIG_DIR/build_kernel.sh" 2>&1 | tee bazel_build.log
              ;;
            bazel_projects_s5e9945)
              echo "▶ Bazel //projects/s5e9945:s5e9945_user"
              cd "${'$'}CONFIG_DIR"
              "${'$'}CONFIG_DIR/tools/bazel" build --nocheck_bzl_visibility --config=stamp \
                --sandbox_debug --verbose_failures --debug_make_verbosity=I \
                //projects/s5e9945:s5e9945_user 2>&1 | tee bazel_build.log
              ;;
            bazel_projects_s5e8845)
              echo "▶ Bazel //projects/s5e8845:s5e8845_user"
              cd "${'$'}CONFIG_DIR"
              "${'$'}CONFIG_DIR/tools/bazel" build --nocheck_bzl_visibility --config=stamp \
                --sandbox_debug --verbose_failures --debug_make_verbosity=I \
                //projects/s5e8845:s5e8845_user 2>&1 | tee bazel_build.log
              ;;
            bazel_common)
              echo "▶ Bazel //common:kernel_aarch64"
              cd "${'$'}PLATFORM"
              "${'$'}PLATFORM/tools/bazel" build //common:kernel_aarch64 2>&1 | tee "${'$'}CONFIG_DIR/bazel_build.log"
              ;;
            *)
              echo "❌ BUILD_MODE desconhecido: ${'$'}BUILD_MODE"
              exit 1
              ;;
          esac
          EXIT=${'$'}?
          set -e

          echo "=== Últimas 60 linhas do log ==="
          tail -60 "${'$'}CONFIG_DIR/bazel_build.log" 2>/dev/null || tail -60 bazel_build.log

          if [ "${'$'}EXIT" -ne 0 ]; then
            echo "❌ Build falhou (exit ${'$'}EXIT)"
            grep -n "ERROR\|error:\|FAILED" "${'$'}CONFIG_DIR/bazel_build.log" 2>/dev/null | head -30 || true
            exit 1
          fi

          # ----------------------------------------------------------
          # Localiza a Image (procura em vários lugares possíveis)
          # ----------------------------------------------------------
          IMAGE_PATH=""
          for pattern in \
            "${'$'}PLATFORM/out/bazel/**/kbuild_mixed_tree/Image" \
            "${'$'}PLATFORM/out/bazel/**/dist/Image" \
            "${'$'}PLATFORM/bazel-bin/common/kernel_aarch64/Image" \
            "${'$'}CONFIG_DIR/out/**/dist/Image" \
            "${'$'}CONFIG_DIR/bazel-bin/**/Image" ; do
            IMAGE_PATH=${'$'}(shopt -s globstar; ls ${'$'}pattern 2>/dev/null | head -1)
            [ -n "${'$'}IMAGE_PATH" ] && break
          done

          if [ -z "${'$'}IMAGE_PATH" ]; then
            echo "❌ Image não encontrada via globs — busca recursiva:"
            IMAGE_PATH=${'$'}(find "${'$'}CONFIG_DIR" -type f -name "Image" 2>/dev/null | head -1)
          fi

          if [ -z "${'$'}IMAGE_PATH" ]; then
            echo "❌ Image não encontrada"
            exit 1
          fi

          echo "✅ Image encontrada: ${'$'}IMAGE_PATH"
          ls -lh "${'$'}IMAGE_PATH"

          mkdir -p "${'$'}GITHUB_WORKSPACE/build_output"
          cp "${'$'}IMAGE_PATH" "${'$'}GITHUB_WORKSPACE/build_output/Image"
          echo "BUILT_IMAGE=${'$'}GITHUB_WORKSPACE/build_output/Image" >> ${'$'}GITHUB_ENV

      - name: Upload build log
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: build-log-${'$'}{{ inputs.device_model }}
          path: ${'$'}GITHUB_WORKSPACE/${'$'}{{ env.CONFIG }}/bazel_build.log
          if-no-files-found: warn

      # ==========================================================
      #  15. SAÍDA 1: boot.tar.md5 (Odin) — opcional
      # ==========================================================
      - name: Build boot.tar.md5 (Odin)
        if: success() && (inputs.boot_img_url != '' || inputs.boot_img_b64 != '')
        run: |
          curl -fSL -o magiskboot \
            "https://raw.githubusercontent.com/Uevo001/magiskboot-linux/main/x86_64/magiskboot"
          chmod +x magiskboot

          ./magiskboot unpack boot.img
          cp "${'$'}{{ env.BUILT_IMAGE }}" kernel
          ./magiskboot repack boot.img boot_patched.img

          mv boot_patched.img boot.img
          mkdir -p odin_package
          cp boot.img odin_package/boot.img
          cd odin_package
          tar -cvf ../boot.tar boot.img
          cd ..
          md5sum -t boot.tar >> boot.tar
          mv boot.tar boot.tar.md5

          ls -lh boot.tar.md5

      # ==========================================================
      #  16. SAÍDA 2: AnyKernel3.zip
      # ==========================================================
      - name: Build AnyKernel3.zip
        if: success()
        run: |
          cp "${'$'}{{ env.BUILT_IMAGE }}" "${'$'}ANYKERNEL3/Image"

          sed -i "s/^kernel.string=.*/kernel.string=Custom Kernel ${'$'}{{ inputs.device_branch }}/" "${'$'}ANYKERNEL3/anykernel.sh" || true
          sed -i "s/^device.name1=.*/device.name1=${'$'}{{ inputs.device_model }}/" "${'$'}ANYKERNEL3/anykernel.sh" || true
          for n in 2 3 4 5; do
            sed -i "s/^device.name${'$'}n=.*/device.name${'$'}n=/" "${'$'}ANYKERNEL3/anykernel.sh" || true
          done

          cd "${'$'}ANYKERNEL3"
          zip -r9 "../AnyKernel3-${'$'}{{ inputs.device_model }}.zip" . -x "*.git*"
          cd ..

          ls -lh AnyKernel3-*.zip

      # ==========================================================
      #  17. Upload de artefatos
      # ==========================================================
      - name: Upload artifacts
        if: success()
        uses: actions/upload-artifact@v4
        with:
          name: custom-kernel-${'$'}{{ inputs.device_model }}
          path: |
            boot.tar.md5
            boot.img
            AnyKernel3-${'$'}{{ inputs.device_model }}.zip
          compression-level: 9
          if-no-files-found: warn
""".trimIndent()
}
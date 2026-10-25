#!/usr/bin/env bash
#
# Guitar Simulator — macOS / Linux installer
# --------------------------------------------
# Downloads guitar-simulator.jar to your Desktop and checks that Java is installed.
#
# One-liner to share with friends:
#
#   curl -L https://raw.githubusercontent.com/jmducut2014/guitar-simulator/main/install.sh | bash
#
# (Replace YOUR_GH_USER with your GitHub username after you've published the repo.)
#
set -euo pipefail

# ---- CONFIG: edit these two lines after you create the repo on GitHub ----
GH_USER="jmducut2014"
REPO="guitar-simulator"
# --------------------------------------------------------------------------

JAR_NAME="guitar-simulator.jar"
# GitHub's "latest release" redirect URL is the cleanest way to download
# a release asset without hardcoding a version number.
JAR_URL="https://github.com/${GH_USER}/${REPO}/releases/latest/download/${JAR_NAME}"

# Where to install. macOS and Linux both use ~/Desktop as the literal path
# even on systems where the Desktop's display name is localized.
DESKTOP_DIR="${HOME}/Desktop"

# ---- Java check ----
if ! command -v java >/dev/null 2>&1; then
    cat <<EOF

================================================================
  Java is not installed (or not on your PATH).
  Guitar Simulator needs Java 17 or newer to run.

  Install it from:    https://adoptium.net/temurin/releases/
  Or with Homebrew:    brew install openjdk@21
================================================================
EOF
    exit 1
fi

JAVA_MAJOR=$(java -version 2>&1 | awk -F[\".] '/version/ {print $2; exit}')
if [ -z "${JAVA_MAJOR:-}" ] || [ "${JAVA_MAJOR:-0}" -lt 17 ]; then
    cat <<EOF

================================================================
  Your Java is too old. Need Java 17+, you have ${JAVA_MAJOR:-unknown}.
  Update from:    https://adoptium.net/temurin/releases/
================================================================
EOF
    exit 1
fi

# ---- Download ----
echo "Downloading ${JAR_NAME}..."
mkdir -p "${DESKTOP_DIR}"
TMP_FILE="$(mktemp)"
trap 'rm -f "${TMP_FILE}"' EXIT

if ! curl -L --fail --silent --show-error "${JAR_URL}" -o "${TMP_FILE}"; then
    cat <<EOF

================================================================
  Download failed.

  Possible causes:
    * You haven't published a GitHub Release yet with the jar attached.
      (See the README's "Publish to GitHub" section.)
    * GH_USER / REPO at the top of this script are still the placeholders.
================================================================
EOF
    exit 1
fi

mv "${TMP_FILE}" "${DESKTOP_DIR}/${JAR_NAME}"
trap - EXIT

echo ""
echo "  Installed: ${DESKTOP_DIR}/${JAR_NAME}"
echo "  Run it:   java -jar ~/Desktop/${JAR_NAME}"
echo "            (or just double-click the jar in Finder/Files)"
echo ""

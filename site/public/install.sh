#!/bin/sh

install_azkar() (
    set -eu
    site=https://jamhour1g.github.io/Azkar-App
    releases=https://github.com/jamhour1g/Azkar-App/releases

    fail() {
        printf 'Azkar: %s\n' "$*" >&2
        exit 1
    }

    [ "$#" -le 1 ] || fail 'Usage: install.sh [stable|dev|--dev]'
    channel=${1-prompt}
    case "$channel" in
        prompt|stable|dev) ;;
        --dev) channel=dev ;;
        *) fail 'Usage: install.sh [stable|dev|--dev]' ;;
    esac

    choose_channel() {
        while :; do
            printf '%s ' "$1" >&3
            IFS= read -r choice <&3 || exit 0
            case "$choice" in
                [Ss])
                    if [ "$2" = stable ]; then
                        channel=stable
                        return
                    fi
                    ;;
                [Dd]) channel=dev; return ;;
                [Cc]|'') exit 0 ;;
            esac
            printf 'Choose one of the listed options.\n' >&3
        done
    }

    as_root() {
        if [ "$(id -u)" = 0 ]; then
            "$@"
        else
            sudo "$@"
        fi
    }

    fetch() {
        curl -fsSL --proto '=https' --proto-redir '=https' "$1" -o "$2"
    }

    repository_key() {
        fetch "$site/keys/azkar-repo.asc" "$tmp/key.asc"
        keys=$(gpg --batch --with-colons --show-keys "$tmp/key.asc")
        fingerprints=$(printf '%s\n' "$keys" | awk -F: '
            $1 == "pub" { primary = 1 }
            $1 == "fpr" && primary { print $10; primary = 0 }
        ')
        [ "$fingerprints" = 195D8FE24FAFCB15170BE26425796BA0347033EB ] ||
            fail 'Unexpected repository signing key.'
    }

    os=$(uname -s)
    arch=$(uname -m)
    case "$os:$arch" in
        Linux:x86_64)
            suffix='linux-x64[.]AppImage'
            installer=AppImage
            ;;
        Darwin:arm64)
            suffix='macos-arm64[.]dmg'
            installer=DMG
            ;;
        Darwin:x86_64)
            if [ "$(sysctl -n sysctl.proc_translated 2>/dev/null || true)" = 1 ]; then
                suffix='macos-arm64[.]dmg'
            else
                suffix='macos-x64[.]dmg'
            fi
            installer=DMG
            ;;
        *) fail 'This installer supports x86-64 Linux, Apple Silicon, and Intel Macs.' ;;
    esac

    if [ "$channel" = prompt ]; then
        # The installer may itself be arriving on standard input.
        ( : <>/dev/tty ) 2>/dev/null || fail 'Run in a terminal or specify stable or --dev.'
        exec 3<>/dev/tty
        choose_channel 'Install Azkar from [S] Stable or [D] Dev (prerelease), or [C] Cancel:' stable
        if [ "$channel" = stable ]; then
            latest_api=https://api.github.com/repos/jamhour1g/Azkar-App/releases/latest
            status=$(curl -sSL --proto '=https' --proto-redir '=https' -o /dev/null -w '%{http_code}' "$latest_api") ||
                fail 'Could not check for a Stable release.'
            case "$status" in
                200) ;;
                404) choose_channel 'No Stable release is published. [D] Install Dev or [C] Cancel:' dev ;;
                *) fail "Could not check for a Stable release (HTTP $status)." ;;
            esac
        fi
        exec 3>&-
    fi
    case "$channel" in
        stable) channel_name=Stable ;;
        dev) channel_name=Dev ;;
    esac

    tmp=$(mktemp -d)
    trap 'rm -rf "$tmp"' EXIT
    trap 'exit 1' HUP INT TERM

    install_linux_package() {
        ID=
        ID_LIKE=
        if [ -r /etc/os-release ]; then
            # shellcheck source=/dev/null
            . /etc/os-release
        elif [ -r /usr/lib/os-release ]; then
            # shellcheck source=/dev/null
            . /usr/lib/os-release
        fi

        case " $ID $ID_LIKE " in
            *' debian '*|*' ubuntu '*)
                if command -v apt-get >/dev/null && [ "$(dpkg --print-architecture)" = amd64 ]; then
                    repository_key
                    gpg --batch --dearmor --output "$tmp/key.gpg" "$tmp/key.asc"
                    printf 'deb [arch=amd64 signed-by=/usr/share/keyrings/azkar-repo.gpg] %s/apt %s main\n' "$site" "$channel" > "$tmp/azkar.list"
                    as_root install -m 0644 "$tmp/key.gpg" /usr/share/keyrings/azkar-repo.gpg
                    as_root install -m 0644 "$tmp/azkar.list" /etc/apt/sources.list.d/azkar.list
                    as_root apt-get update -o Dir::Etc::sourcelist=sources.list.d/azkar.list \
                        -o Dir::Etc::sourceparts=- -o APT::Update::Error-Mode=any
                    as_root apt-get install -y "azkar/$channel" </dev/null
                    exit
                fi
                ;;
        esac

        if [ "$ID" = fedora ] && command -v dnf >/dev/null; then
            repository_key
            cat > "$tmp/azkar.repo" <<REPO
[azkar-$channel]
name=Azkar $channel_name
baseurl=$site/rpm/$channel/
enabled=1
gpgcheck=0
repo_gpgcheck=1
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-azkar
REPO
            as_root install -m 0644 "$tmp/key.asc" /etc/pki/rpm-gpg/RPM-GPG-KEY-azkar
            as_root install -m 0644 "$tmp/azkar.repo" /etc/yum.repos.d/azkar.repo
            as_root dnf --refresh install -y azkar </dev/null
            exit
        fi
    }

    if [ "$os" = Linux ]; then
        install_linux_package
    fi

    download="$releases/latest/download"
    if [ "$channel" = dev ]; then
        fetch "$site/dev-version.txt" "$tmp/version"
        version=$(awk 'NR != 1 || $0 !~ /^[0-9]+[.][0-9]+[.][0-9]+-beta[.][0-9]+([.][0-9]+)*$/ { exit 1 }
            { print }' "$tmp/version") || fail 'Invalid Dev release version.'
        [ -n "$version" ] || fail 'No Dev release version found.'
        download="$releases/download/v$version"
    fi

    fetch "$download/SHA256SUMS" "$tmp/SHA256SUMS"
    entry=$(awk -v suffix="$suffix" 'NF == 2 && length($1) == 64 && $1 !~ /[^[:xdigit:]]/ &&
        $2 ~ ("^azkar-[0-9][0-9A-Za-z.+-]*-" suffix "$") { print $1 "  " $2 }' "$tmp/SHA256SUMS")
    case "$entry" in
        ''|*'
'*) fail "Expected one matching $installer in the $channel_name release." ;;
    esac
    asset=${entry#*"  "}
    fetch "$download/$asset" "$tmp/$asset"
    if [ "$os" = Darwin ]; then
        (cd "$tmp" && printf '%s\n' "$entry" | shasum -a 256 -c -)
        mkdir -p "$HOME/Downloads"
        folder=$(mktemp -d "$HOME/Downloads/Azkar.XXXXXX")
        disk_image="$folder/$asset"
        mv "$tmp/$asset" "$disk_image"
        printf 'Opening "%s". Drag Azkar to Applications to finish installation.\n' "$disk_image"
        open "$disk_image"
        exit
    fi

    (cd "$tmp" && printf '%s\n' "$entry" | sha256sum -c -)

    bin="$HOME/.local/bin"
    mkdir -p "$bin"
    staged=$(mktemp "$bin/.azkar.XXXXXX")
    trap 'rm -rf "$tmp"; rm -f "$staged"' EXIT
    install -m 0755 "$tmp/$asset" "$staged"
    mv -fT "$staged" "$bin/azkar"
    printf 'Azkar installed. Launch it with "%s/azkar".\n' "$bin"
)

install_azkar "$@"

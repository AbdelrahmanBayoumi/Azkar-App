# The package owns the files under /opt. GNOME discovers them through this link.
azkar_extension=azkar-prayers@azkar.app
azkar_extension_source='/opt/${sanitizedProductName}/lib/app/resources/gnome'/"$azkar_extension"
azkar_extension_link=/usr/share/gnome-shell/extensions/"$azkar_extension"

if [ -f "$azkar_extension_source/metadata.json" ] && [ ! -e "$azkar_extension_link" ] && [ ! -L "$azkar_extension_link" ]; then
    mkdir -p /usr/share/gnome-shell/extensions
    ln -s "$azkar_extension_source" "$azkar_extension_link"
fi

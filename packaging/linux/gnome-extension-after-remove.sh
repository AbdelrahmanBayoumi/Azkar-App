azkar_extension=azkar-prayers@azkar.app
azkar_extension_source='/opt/${sanitizedProductName}/lib/app/resources/gnome'/"$azkar_extension"
azkar_extension_link=/usr/share/gnome-shell/extensions/"$azkar_extension"

# An RPM upgrade can run the old remove script after the new files are already installed.
if [ ! -d "$azkar_extension_source" ] && [ -L "$azkar_extension_link" ] &&
   [ "$(readlink "$azkar_extension_link")" = "$azkar_extension_source" ]; then
    rm "$azkar_extension_link"
fi

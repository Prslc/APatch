#!/system/bin/sh
#
# APatch Boot Installation Script
# Handles flashing and A/B slot switching logic.
#

. ./util_functions.sh

SUPERKEY="$1"
IS_NEXT_SLOT="$2"
PATCH_DIR=$(pwd)

[ -z "$SUPERKEY" ] && { echo "! SuperKey empty"; exit 1; }

# Determine slot and find partition
if [ "$IS_NEXT_SLOT" = "true" ]; then
    get_next_slot
else
    get_current_slot
fi

find_boot_image
[ -z "$BOOTIMAGE" ] && { echo "! Partition not found"; exit 1; }

# Run the patcher
sh ./boot_patch.sh "$SUPERKEY" "$BOOTIMAGE" "true"
[ $? -ne 0 ] && exit 1

# Handle A/B slot switching and persistence
if [ "$IS_NEXT_SLOT" = "true" ] && [ -n "$SLOT" ]; then
    chmod 755 ./bootctl
    ./bootctl set-active-boot-slot "$SLOT"

    # Create post-fs-data trigger to prevent rollback
    mkdir -p /data/adb/post-fs-data.d
    cat <<EOF > /data/adb/post-fs-data.d/post_ota.sh
#!/system/bin/sh
chmod 0777 $PATCH_DIR/bootctl
chown root:root $PATCH_DIR/bootctl
$PATCH_DIR/bootctl mark-boot-successful
rm -rf $PATCH_DIR
rm -f /data/adb/post-fs-data.d/post_ota.sh
EOF

    chmod 0777 /data/adb/post-fs-data.d/post_ota.sh
    chown root:root /data/adb/post-fs-data.d/post_ota.sh
fi

echo "- Done"
exit 0
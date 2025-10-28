#!/bin/bash

assets_dir="./assets"

if [ ! -d "$assets_dir" ]; then
    echo "Error: assets directory not found at $assets_dir"
    exit 1
fi

for dir_path in "$assets_dir"/*; do
    [ ! -d "$dir_path" ] && continue

    dir=$(basename "$dir_path")
    
    [ "$dir" == "minecraft" ] && continue

    texture_path="$dir_path/textures/trims/entity/wings"

    if [ ! -d "$texture_path" ]; then
        echo "Warning: Texture path not found for $dir: $texture_path"
        continue
    fi

    output_dir="$assets_dir/../data/$dir/trim_pattern"
    mkdir -p "$output_dir"

    for file_path in "$texture_path"/*; do
            [ ! -f "$file_path" ] && continue

            file=$(basename "$file_path")
        file="${file%.*}"

            output_file="$output_dir/${file}.json"

            cat > "$output_file" << EOF
{
  "asset_id": "${dir}:${file}",
  "description": {
    "translate": "trim_pattern.${dir}.${file}"
  }
}
EOF

        echo "Created: $output_file"
    done
done
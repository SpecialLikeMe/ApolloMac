#!/usr/bin/env bash

set -euo pipefail

manifest_path=""
output_dir=""
preserve_path=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --manifest-path)
            manifest_path="${2-}"
            shift 2
            ;;
        --output-dir)
            output_dir="${2-}"
            shift 2
            ;;
        --preserve-path)
            preserve_path="${2-}"
            shift 2
            ;;
        *)
            echo "Unknown option: $1" >&2
            exit 1
            ;;
    esac
done

normalize_path() {
    local path_value=$1
    if [[ -z "$path_value" ]]; then
        return 1
    fi

    if [[ -d "$path_value" ]]; then
        (cd -- "$path_value" && pwd -P)
        return 0
    fi

    local parent_dir
    local base_name
    parent_dir=$(dirname -- "$path_value")
    base_name=$(basename -- "$path_value")
    if [[ ! -d "$parent_dir" ]]; then
        return 1
    fi

    (cd -- "$parent_dir" && printf '%s/%s\n' "$(pwd -P)" "$base_name")
}

remove_with_retries() {
    local target=$1
    local attempt

    for attempt in $(seq 1 15); do
        if [[ ! -e "$target" ]]; then
            return 0
        fi

        if rm -rf -- "$target" 2>/dev/null; then
            return 0
        fi

        sleep 0.2
    done
}

scratch_file=$(mktemp)
trap 'rm -f "$scratch_file"' EXIT

if [[ -n "$manifest_path" && -f "$manifest_path" ]]; then
    while IFS= read -r line; do
        [[ -z "$line" ]] && continue
        if normalized=$(normalize_path "$line" 2>/dev/null); then
            printf '%s\n' "$normalized" >> "$scratch_file"
        fi
    done < "$manifest_path"
fi

if [[ -n "$output_dir" && -d "$output_dir" ]]; then
    while IFS= read -r child_path; do
        if normalized=$(normalize_path "$child_path" 2>/dev/null); then
            printf '%s\n' "$normalized" >> "$scratch_file"
        fi
    done < <(find "$output_dir" -mindepth 1 -maxdepth 1 -print)
fi

normalized_preserve=""
if [[ -n "$preserve_path" ]]; then
    normalized_preserve=$(normalize_path "$preserve_path" 2>/dev/null || true)
fi

if [[ -s "$scratch_file" ]]; then
    while IFS= read -r target; do
        [[ -z "$target" ]] && continue
        if [[ -n "$normalized_preserve" && "$target" == "$normalized_preserve" ]]; then
            continue
        fi
        remove_with_retries "$target"
    done < <(sort -u "$scratch_file" | awk '{ print length, $0 }' | sort -rn | cut -d' ' -f2-)
fi

normalized_output_dir=""
if [[ -n "$output_dir" ]]; then
    normalized_output_dir=$(normalize_path "$output_dir" 2>/dev/null || true)
fi

if [[ -n "$normalized_output_dir" && -d "$normalized_output_dir" ]]; then
    if ! find "$normalized_output_dir" -mindepth 1 -print -quit | grep -q .; then
        remove_with_retries "$normalized_output_dir"
    fi
fi
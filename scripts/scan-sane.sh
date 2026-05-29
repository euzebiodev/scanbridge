#!/usr/bin/env bash
set -euo pipefail

OUTPUT_PATH=""
DEVICE_NAME=""
DPI="300"
COLOR_MODE="color"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --output)
      OUTPUT_PATH="$2"
      shift 2
      ;;
    --device)
      DEVICE_NAME="$2"
      shift 2
      ;;
    --dpi)
      DPI="$2"
      shift 2
      ;;
    --mode)
      COLOR_MODE="$2"
      shift 2
      ;;
    *)
      echo "Opcao desconhecida: $1" >&2
      exit 2
      ;;
  esac
done

if [[ -z "$OUTPUT_PATH" ]]; then
  echo "Informe --output" >&2
  exit 2
fi

case "$COLOR_MODE" in
  color)
    SANE_MODE="Color"
    ;;
  grayscale)
    SANE_MODE="Gray"
    ;;
  blackwhite)
    SANE_MODE="Lineart"
    ;;
  *)
    echo "Modo invalido: $COLOR_MODE" >&2
    exit 2
    ;;
esac

mkdir -p "$(dirname "$OUTPUT_PATH")"

COMMAND=(scanimage)
if [[ -n "$DEVICE_NAME" ]]; then
  COMMAND+=(-d "$DEVICE_NAME")
fi
COMMAND+=(--resolution "$DPI" --mode "$SANE_MODE" --format=jpeg --output-file "$OUTPUT_PATH")

"${COMMAND[@]}"
echo "$OUTPUT_PATH"

const stage = document.getElementById("crop-stage");
const image = document.getElementById("document-image");
const selection = document.getElementById("crop-selection");
const clearButton = document.getElementById("clear-selection");
const fields = {
    x: document.getElementById("crop-x"),
    y: document.getElementById("crop-y"),
    width: document.getElementById("crop-width"),
    height: document.getElementById("crop-height")
};

let dragStart = null;
let activePointerId = null;

function pointFromEvent(event) {
    const imageRect = image.getBoundingClientRect();
    const x = Math.max(0, Math.min(event.clientX - imageRect.left, imageRect.width));
    const y = Math.max(0, Math.min(event.clientY - imageRect.top, imageRect.height));
    return { x, y, imageRect };
}

function updateSelection(start, current) {
    const left = Math.min(start.x, current.x);
    const top = Math.min(start.y, current.y);
    const width = Math.abs(current.x - start.x);
    const height = Math.abs(current.y - start.y);

    selection.style.display = width > 4 && height > 4 ? "block" : "none";
    selection.style.left = `${left}px`;
    selection.style.top = `${top}px`;
    selection.style.width = `${width}px`;
    selection.style.height = `${height}px`;

    const scaleX = image.naturalWidth / current.imageRect.width;
    const scaleY = image.naturalHeight / current.imageRect.height;
    fields.x.value = Math.round(left * scaleX);
    fields.y.value = Math.round(top * scaleY);
    fields.width.value = Math.round(width * scaleX);
    fields.height.value = Math.round(height * scaleY);
}

function clearSelection() {
    selection.style.display = "none";
    selection.style.width = "0";
    selection.style.height = "0";
    fields.x.value = "";
    fields.y.value = "";
    fields.width.value = "";
    fields.height.value = "";
}

stage.addEventListener("pointerdown", (event) => {
    if (event.button !== 0) {
        return;
    }
    activePointerId = event.pointerId;
    dragStart = pointFromEvent(event);
    stage.setPointerCapture(activePointerId);
    updateSelection(dragStart, dragStart);
});

stage.addEventListener("pointermove", (event) => {
    if (dragStart === null || event.pointerId !== activePointerId) {
        return;
    }
    updateSelection(dragStart, pointFromEvent(event));
});

stage.addEventListener("pointerup", (event) => {
    if (event.pointerId !== activePointerId) {
        return;
    }
    const current = pointFromEvent(event);
    updateSelection(dragStart, current);
    if (!fields.width.value || Number(fields.width.value) < 5 || Number(fields.height.value) < 5) {
        clearSelection();
    }
    dragStart = null;
    activePointerId = null;
});

stage.addEventListener("pointercancel", clearSelection);
clearButton.addEventListener("click", clearSelection);

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

const handles = ["nw", "n", "ne", "e", "se", "s", "sw", "w"];
handles.forEach((handle) => {
    const element = document.createElement("span");
    element.className = `crop-handle crop-handle-${handle}`;
    element.dataset.handle = handle;
    selection.appendChild(element);
});

let crop = null;
let interaction = null;
let activePointerId = null;

function imageRect() {
    return image.getBoundingClientRect();
}

function pointFromEvent(event) {
    const rect = imageRect();
    return {
        x: Math.max(0, Math.min(event.clientX - rect.left, rect.width)),
        y: Math.max(0, Math.min(event.clientY - rect.top, rect.height))
    };
}

function normalizedCrop(cropValue) {
    const left = Math.min(cropValue.x1, cropValue.x2);
    const top = Math.min(cropValue.y1, cropValue.y2);
    const right = Math.max(cropValue.x1, cropValue.x2);
    const bottom = Math.max(cropValue.y1, cropValue.y2);
    return {
        left,
        top,
        width: right - left,
        height: bottom - top,
        right,
        bottom
    };
}

function renderCrop() {
    if (crop === null) {
        clearSelection();
        return;
    }

    const current = normalizedCrop(crop);
    if (current.width < 5 || current.height < 5) {
        selection.style.display = "none";
        updateFields(null);
        return;
    }

    selection.style.display = "block";
    selection.style.left = `${current.left}px`;
    selection.style.top = `${current.top}px`;
    selection.style.width = `${current.width}px`;
    selection.style.height = `${current.height}px`;
    updateFields(current);
}

function updateFields(current) {
    if (current === null) {
        fields.x.value = "";
        fields.y.value = "";
        fields.width.value = "";
        fields.height.value = "";
        return;
    }

    const rect = imageRect();
    const scaleX = image.naturalWidth / rect.width;
    const scaleY = image.naturalHeight / rect.height;
    fields.x.value = Math.round(current.left * scaleX);
    fields.y.value = Math.round(current.top * scaleY);
    fields.width.value = Math.round(current.width * scaleX);
    fields.height.value = Math.round(current.height * scaleY);
}

function clearSelection() {
    selection.style.display = "none";
    selection.style.width = "0";
    selection.style.height = "0";
    fields.x.value = "";
    fields.y.value = "";
    fields.width.value = "";
    fields.height.value = "";
    crop = null;
    interaction = null;
    activePointerId = null;
}

function startDraw(event) {
    const point = pointFromEvent(event);
    crop = { x1: point.x, y1: point.y, x2: point.x, y2: point.y };
    interaction = { type: "draw" };
}

function startMove(event) {
    const point = pointFromEvent(event);
    const current = normalizedCrop(crop);
    interaction = {
        type: "move",
        startPoint: point,
        startCrop: current
    };
}

function startResize(event, handle) {
    const current = normalizedCrop(crop);
    interaction = {
        type: "resize",
        handle,
        startCrop: current
    };
}

function moveCrop(event) {
    const point = pointFromEvent(event);
    const rect = imageRect();
    const deltaX = point.x - interaction.startPoint.x;
    const deltaY = point.y - interaction.startPoint.y;
    const width = interaction.startCrop.width;
    const height = interaction.startCrop.height;
    const left = Math.max(0, Math.min(interaction.startCrop.left + deltaX, rect.width - width));
    const top = Math.max(0, Math.min(interaction.startCrop.top + deltaY, rect.height - height));
    crop = { x1: left, y1: top, x2: left + width, y2: top + height };
}

function resizeCrop(event) {
    const point = pointFromEvent(event);
    const start = interaction.startCrop;
    let left = start.left;
    let top = start.top;
    let right = start.right;
    let bottom = start.bottom;

    if (interaction.handle.includes("w")) {
        left = point.x;
    }
    if (interaction.handle.includes("e")) {
        right = point.x;
    }
    if (interaction.handle.includes("n")) {
        top = point.y;
    }
    if (interaction.handle.includes("s")) {
        bottom = point.y;
    }

    crop = { x1: left, y1: top, x2: right, y2: bottom };
}

stage.addEventListener("pointerdown", (event) => {
    if (event.button !== 0) {
        return;
    }

    activePointerId = event.pointerId;
    stage.setPointerCapture(activePointerId);

    if (event.target.dataset.handle) {
        startResize(event, event.target.dataset.handle);
    } else if (event.target === selection && crop !== null) {
        startMove(event);
    } else {
        startDraw(event);
    }

    renderCrop();
});

stage.addEventListener("pointermove", (event) => {
    if (interaction === null || event.pointerId !== activePointerId) {
        return;
    }

    if (interaction.type === "draw") {
        const point = pointFromEvent(event);
        crop.x2 = point.x;
        crop.y2 = point.y;
    }
    if (interaction.type === "move") {
        moveCrop(event);
    }
    if (interaction.type === "resize") {
        resizeCrop(event);
    }

    renderCrop();
});

stage.addEventListener("pointerup", (event) => {
    if (event.pointerId !== activePointerId) {
        return;
    }

    renderCrop();
    const current = crop === null ? null : normalizedCrop(crop);
    if (current === null || current.width < 5 || current.height < 5) {
        clearSelection();
    } else {
        crop = { x1: current.left, y1: current.top, x2: current.right, y2: current.bottom };
        interaction = null;
        activePointerId = null;
        renderCrop();
    }
});

stage.addEventListener("pointercancel", () => {
    interaction = null;
    activePointerId = null;
    renderCrop();
});

clearButton.addEventListener("click", clearSelection);

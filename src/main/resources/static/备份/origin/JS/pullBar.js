 const dragBar = document.getElementById("dragBar");
    const left = document.querySelector(".left");
    const main = document.querySelector(".main");

    let isDragging = false;

    dragBar.addEventListener("mousedown", function () {
        isDragging = true;
        document.body.style.cursor = "col-resize";
        document.body.style.userSelect = "none";
    });

    document.addEventListener("mousemove", function (e) {
        if (!isDragging) return;

        const mainRect = main.getBoundingClientRect();

        let newLeftWidth = e.clientX - mainRect.left;

        // 最小最大限制
        if (newLeftWidth < 150) newLeftWidth = 150;
        if (newLeftWidth > mainRect.width - 200) {
            newLeftWidth = mainRect.width - 200;
        }

        left.style.width = newLeftWidth + "px";
    });

    document.addEventListener("mouseup", function () {
        isDragging = false;
        document.body.style.cursor = "default";
        document.body.style.userSelect = "auto";
    });
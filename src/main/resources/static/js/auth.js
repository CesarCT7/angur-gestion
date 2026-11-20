"use strict";
document.querySelectorAll("[data-password-toggle]").forEach(button => {
    button.addEventListener("click", () => {
        const input = document.getElementById(button.dataset.passwordToggle);
        if (!input) return;
        const visible = input.type === "password";
        input.type = visible ? "text" : "password";
        button.textContent = visible ? "Ocultar" : "Mostrar";
        button.setAttribute("aria-pressed", String(visible));
    });
});

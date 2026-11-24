try {
  const savedTheme = localStorage.getItem("azkar-download-theme");
  if (savedTheme === "light" || savedTheme === "dark")
    document.documentElement.dataset.theme = savedTheme;
} catch {
  // Keep the system theme when browser storage is unavailable.
}

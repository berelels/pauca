// Os blocos aparecem suavemente ao entrar na tela (o CSS ignora isso com "menos movimento").
const io = new IntersectionObserver(entries => {
  for (const e of entries) if (e.isIntersecting) { e.target.classList.add("in"); io.unobserve(e.target); }
}, { rootMargin: "0px 0px -10% 0px" });
document.querySelectorAll(".reveal").forEach(el => io.observe(el));

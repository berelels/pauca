// Página do Pauca: os toques de movimento. Sem bibliotecas e sem nada de fora (a página não fala com ninguém).
// Tudo é enfeite: com "menos movimento" nada disto anda, e sem JavaScript o conteúdo continua inteiro.
(() => {
  "use strict";
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => [...r.querySelectorAll(s)];
  const reduce = matchMedia("(prefers-reduced-motion: reduce)").matches;
  const mouse = matchMedia("(hover: hover) and (pointer: fine)").matches;
  const clamp = (v, a, b) => Math.min(b, Math.max(a, v));
  const root = document.documentElement;

  // A abertura (o olho se desenhando) passa uma vez por visita
  try { sessionStorage.setItem("pauca-veil", "1"); } catch (e) { /* sem armazenamento */ }

  // ---------- Aparecer ao rolar ----------
  // Os blocos inteiros, e os itens de listas e grades um depois do outro
  $$(".steps .step, .more > div, .checks li, .plans .plan, .cols p, .faq details").forEach((el) => {
    const i = $$(":scope > *", el.parentElement).indexOf(el);
    el.style.setProperty("--i", String(Math.min(i < 0 ? 0 : i, 6)));
    el.classList.add("rv");
  });
  const io = new IntersectionObserver((entries) => {
    for (const e of entries) if (e.isIntersecting) { e.target.classList.add("in"); io.unobserve(e.target); }
  }, { rootMargin: "0px 0px -10% 0px" });
  $$(".reveal, .rv, .wordmark").forEach((el) => io.observe(el));

  // ---------- Topo: barra de leitura, some ao descer, seção atual ----------
  const top = $(".top");
  const links = $$(".top nav a");
  const sections = links.map((a) => $(a.getAttribute("href"))).filter(Boolean);
  let lastY = scrollY;
  const onScroll = () => {
    const y = scrollY;
    const max = Math.max(1, document.documentElement.scrollHeight - innerHeight);
    top.style.setProperty("--p", clamp(y / max, 0, 1).toFixed(4));
    top.classList.toggle("lift", y > 8);
    if (y > 140 && y > lastY + 6) top.classList.add("hide");
    else if (y < lastY - 6 || y < 140) top.classList.remove("hide");
    lastY = y;
    let current = null;
    for (const s of sections) if (s.getBoundingClientRect().top < innerHeight * 0.4) current = s.id;
    links.forEach((a) => a.classList.toggle("on", a.getAttribute("href") === "#" + current));
  };

  // ---------- Paralaxe: os celulares andam um pouco diferente da rolagem ----------
  const stage = $(".stage");
  const visuals = $$(".feature .visual");
  const seen = new Set();
  if (!reduce) {
    const vio = new IntersectionObserver((entries) => {
      for (const e of entries) (e.isIntersecting ? seen.add(e.target) : seen.delete(e.target));
    }, { rootMargin: "20% 0px" });
    visuals.forEach((v) => vio.observe(v));
  }
  const parallax = () => {
    if (reduce) return;
    if (stage) stage.style.setProperty("--sy", String(clamp(scrollY, 0, 900)));
    seen.forEach((v) => {
      const r = v.getBoundingClientRect();
      v.style.setProperty("--vy", clamp((r.top + r.height / 2 - innerHeight / 2) / innerHeight, -1, 1).toFixed(3));
    });
  };

  let ticking = false;
  addEventListener("scroll", () => {
    if (ticking) return;
    ticking = true;
    requestAnimationFrame(() => { onScroll(); parallax(); ticking = false; });
  }, { passive: true });
  addEventListener("resize", () => { onScroll(); parallax(); });
  onScroll();
  parallax();

  // ---------- Amostras de cor: a página inteira troca de cor de destaque ----------
  $$(".sw").forEach((b) => b.addEventListener("click", () => {
    root.style.setProperty("--olive", b.dataset.color);
    $$(".sw").forEach((o) => o.setAttribute("aria-pressed", String(o === b)));
  }));

  // ---------- Perguntas: abrem e fecham devagar ----------
  $$(".faq details").forEach((d) => {
    const summary = $("summary", d);
    let anim = null;
    summary.addEventListener("click", (e) => {
      if (reduce) return;
      e.preventDefault();
      if (anim) anim.cancel();
      const opening = !d.open;
      const from = d.offsetHeight;
      if (opening) d.open = true;
      const to = opening ? d.offsetHeight : summary.offsetHeight;
      d.style.overflow = "hidden";
      anim = d.animate({ height: [from + "px", to + "px"] }, { duration: 420, easing: "cubic-bezier(.2,.7,.1,1)" });
      anim.onfinish = () => { if (!opening) d.open = false; d.style.overflow = ""; anim = null; };
      anim.oncancel = () => { d.style.overflow = ""; };
    });
  });

  // ---------- Assinatura do rodapé: as letras engrossam perto do cursor ----------
  const mark = $(".wordmark");
  if (mark) {
    const letters = $$("span", mark);
    if (mouse && !reduce) {
      let near = false;
      addEventListener("pointermove", (e) => {
        const r = mark.getBoundingClientRect();
        near = e.clientY > r.top - 140 && e.clientY < r.bottom + 140;
        letters.forEach((l) => {
          if (!near) { l.style.fontVariationSettings = ""; l.style.translate = ""; return; }
          const b = l.getBoundingClientRect();
          const d = Math.hypot(e.clientX - (b.left + b.width / 2), e.clientY - (b.top + b.height / 2));
          const k = clamp(1 - d / (b.width * 1.9), 0, 1);
          l.style.fontVariationSettings = `"wght" ${Math.round(300 + 500 * k)}`;
          l.style.translate = `0 ${(-k * 0.05).toFixed(3)}em`;
        });
      }, { passive: true });
    }
    // No toque: tocar numa letra faz ela pular
    letters.forEach((l) => l.addEventListener("pointerdown", () => {
      l.classList.remove("pop"); void l.offsetWidth; l.classList.add("pop");
    }));
  }

  if (!mouse || reduce) return;

  // ---------- Botões: puxam de leve para o cursor, e o preenchimento nasce de onde ele entra ----------
  $$(".btn").forEach((b) => {
    b.addEventListener("pointermove", (e) => {
      const r = b.getBoundingClientRect();
      b.style.setProperty("--x", e.clientX - r.left + "px");
      b.style.setProperty("--y", e.clientY - r.top + "px");
      b.style.translate = `${((e.clientX - r.left - r.width / 2) * 0.16).toFixed(1)}px ${((e.clientY - r.top - r.height / 2) * 0.26).toFixed(1)}px`;
    });
    b.addEventListener("pointerleave", () => { b.style.translate = ""; });
  });

  // ---------- Cartões: inclinam de leve e o brilho segue o cursor ----------
  $$(".step, .more > div, .plan").forEach((c) => {
    c.addEventListener("pointermove", (e) => {
      const r = c.getBoundingClientRect();
      const x = e.clientX - r.left, y = e.clientY - r.top;
      c.style.setProperty("--mx", x + "px");
      c.style.setProperty("--my", y + "px");
      c.style.setProperty("--ry", ((x / r.width - 0.5) * 6).toFixed(2) + "deg");
      c.style.setProperty("--rx", ((0.5 - y / r.height) * 6).toFixed(2) + "deg");
    });
    c.addEventListener("pointerleave", () => { c.style.setProperty("--rx", "0deg"); c.style.setProperty("--ry", "0deg"); });
  });

  // ---------- Cursor: um anel que segue com calma e cresce sobre o que dá para tocar ----------
  const ring = document.createElement("div");
  ring.className = "cursor";
  ring.setAttribute("aria-hidden", "true");
  document.body.append(ring);
  let tx = innerWidth / 2, ty = innerHeight / 2, x = tx, y = ty, raf = 0;
  const loop = () => {
    x += (tx - x) * 0.2; y += (ty - y) * 0.2;
    ring.style.transform = `translate3d(${x.toFixed(1)}px, ${y.toFixed(1)}px, 0)`;
    raf = Math.abs(tx - x) + Math.abs(ty - y) > 0.3 ? requestAnimationFrame(loop) : 0;
  };
  addEventListener("pointermove", (e) => {
    tx = e.clientX; ty = e.clientY;
    ring.classList.add("on");
    ring.classList.toggle("hot", !!e.target.closest("a, button, summary, .sw"));
    if (!raf) raf = requestAnimationFrame(loop);
  }, { passive: true });
  addEventListener("pointerdown", () => ring.classList.add("down"));
  addEventListener("pointerup", () => ring.classList.remove("down"));
  document.addEventListener("pointerleave", () => ring.classList.remove("on"));
})();

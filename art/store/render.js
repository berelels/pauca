// Renderiza slides.html em PNGs com o Electron.
// Uso: electron render.js <idioma> <pasta de saída> <id>[:larguraxaltura] ...
// No Linux a janela não passa da altura do monitor, então cada slide é capturado em faixas
// (o conteúdo é deslocado para cima a cada faixa) e as faixas são coladas no fim.
const { app, BrowserWindow, nativeImage } = require("electron");
const fs = require("fs");
const path = require("path");

const BAND = 640;
app.commandLine.appendSwitch("force-device-scale-factor", "1");
app.disableHardwareAcceleration();
// Sem isto o app fecha quando a primeira janela é destruída
app.on("window-all-closed", () => {});

app.whenReady().then(async () => {
  const [lang, outDir, ...ids] = process.argv.slice(2);
  fs.mkdirSync(outDir, { recursive: true });
  for (const spec of ids) {
    const [id, size = "1080x1920"] = spec.split(":");
    const [w, h] = size.split("x").map(Number);
    const band = Math.min(BAND, h);
    const win = new BrowserWindow({ width: w, height: band, show: false, useContentSize: true });
    const bands = [];
    for (let y = 0; y < h; y += band) {
      await win.loadFile(path.join(__dirname, "slides.html"), { query: { s: id, lang, w: String(w), h: String(h), y: String(y) } });
      await win.webContents.executeJavaScript(
        "document.fonts.ready.then(() => Promise.all([...document.images].map(i => i.decode().catch(() => {}))))");
      await new Promise(r => setTimeout(r, 300));
      const img = await win.webContents.capturePage();
      const size = img.getSize();
      if (size.width !== w || size.height !== band) throw new Error(`faixa com ${size.width}x${size.height}`);
      bands.push(img.toBitmap());
    }
    // Cola as faixas (BGRA) e corta no tamanho final
    const full = Buffer.concat(bands).subarray(0, w * h * 4);
    const out = nativeImage.createFromBitmap(full, { width: w, height: h });
    fs.writeFileSync(path.join(outDir, `${id}.png`), out.toPNG());
    console.error(id, out.getSize());
    win.destroy();
  }
  app.quit();
}).catch(e => { console.error(e); app.exit(1); });

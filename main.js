// Ice Client Launcher -- Electron main process.
// Frameless window + custom titlebar, Microsoft sign-in (msmc -- password only
// ever entered in Microsoft's own popup), and game launch (minecraft-launcher-core).
const { app, BrowserWindow, ipcMain } = require('electron');
const path = require('path');
const fs = require('fs');
const { Auth } = require('msmc');
const { Client } = require('minecraft-launcher-core');

let mainWindow = null;
let currentAuth = null; // mclc-compatible auth object for the signed-in account
const launcher = new Client();

const tokenPath = () => path.join(app.getPath('userData'), 'account.json');
const gameDir = () => path.join(app.getPath('userData'), 'minecraft');
const settingsPath = () => path.join(app.getPath('userData'), 'settings.json');

// ---------- settings ----------

const os = require('os');

// Total installed RAM in whole GB — the slider's upper bound.
const totalRamGb = () => Math.max(2, Math.floor(os.totalmem() / (1024 ** 3)));

// Leave headroom for Windows + the launcher itself; never offer the whole machine.
const maxRamGb = () => Math.max(2, totalRamGb() - 2);

const DEFAULT_SETTINGS = { ramGb: 4 };

function readSettings() {
  try {
    const s = JSON.parse(fs.readFileSync(settingsPath(), 'utf8'));
    return Object.assign({}, DEFAULT_SETTINGS, s);
  } catch (e) {
    return Object.assign({}, DEFAULT_SETTINGS);
  }
}

function writeSettings(s) {
  try { fs.writeFileSync(settingsPath(), JSON.stringify(s, null, 2)); } catch (e) { /* ignore */ }
}

function clampRam(gb) {
  const n = Math.round(Number(gb));
  if (!isFinite(n)) return DEFAULT_SETTINGS.ramGb;
  return Math.min(maxRamGb(), Math.max(2, n));
}

// The pre-built Forge 1.8.9 version we ship in game/forge (versions/ + libraries/).
const FORGE_ID = '1.8.9-forge1.8.9-11.15.1.2318-1.8.9';

// game/ (Forge + mods) is packed as extraResources in the installer, but sits
// next to the source in dev.
const bundleDir = () => app.isPackaged
  ? path.join(process.resourcesPath, 'game')
  : path.join(__dirname, 'game');

function copyDir(src, dst) {
  fs.mkdirSync(dst, { recursive: true });
  for (const entry of fs.readdirSync(src, { withFileTypes: true })) {
    const s = path.join(src, entry.name);
    const d = path.join(dst, entry.name);
    if (entry.isDirectory()) copyDir(s, d);
    else fs.copyFileSync(s, d); // always overwrite so fixes/updates land
  }
}

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1000,
    height: 640,
    minWidth: 900,
    minHeight: 580,
    frame: false,
    backgroundColor: '#05080e',
    show: false,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false
    }
  });
  mainWindow.loadFile('index.html');
  mainWindow.once('ready-to-show', () => mainWindow.show());
}

app.whenReady().then(() => {
  createWindow();
  app.on('activate', () => { if (BrowserWindow.getAllWindows().length === 0) createWindow(); });
});
app.on('window-all-closed', () => { if (process.platform !== 'darwin') app.quit(); });

ipcMain.on('win:minimize', (e) => BrowserWindow.fromWebContents(e.sender)?.minimize());
ipcMain.on('win:close', (e) => BrowserWindow.fromWebContents(e.sender)?.close());

// ---------- settings IPC ----------

ipcMain.handle('settings:get', async () => {
  const s = readSettings();
  s.ramGb = clampRam(s.ramGb);
  return { ...s, maxRamGb: maxRamGb(), totalRamGb: totalRamGb() };
});

ipcMain.handle('settings:set', async (e, patch) => {
  const s = readSettings();
  if (patch && patch.ramGb !== undefined) s.ramGb = clampRam(patch.ramGb);
  writeSettings(s);
  return { ...s, maxRamGb: maxRamGb(), totalRamGb: totalRamGb() };
});

// ---------- auth ----------

function profileOf(mc, xbox) {
  const refresh = (xbox && xbox.msToken && xbox.msToken.refresh_token)
    || (mc && mc.parent && mc.parent.msToken && mc.parent.msToken.refresh_token) || null;
  if (refresh) {
    try { fs.writeFileSync(tokenPath(), JSON.stringify({ refresh })); } catch (e) { /* ignore */ }
  }
  currentAuth = mc.mclc();
  return { name: mc.profile.name, id: mc.profile.id };
}

ipcMain.handle('auth:login', async () => {
  try {
    const authManager = new Auth('select_account');
    const xbox = await authManager.launch('electron');
    const mc = await xbox.getMinecraft();
    return { ok: true, profile: profileOf(mc, xbox) };
  } catch (e) {
    return { ok: false, error: String((e && e.message) || e) };
  }
});

ipcMain.handle('auth:restore', async () => {
  try {
    const saved = JSON.parse(fs.readFileSync(tokenPath(), 'utf8'));
    if (!saved.refresh) return { ok: false };
    const authManager = new Auth('select_account');
    const xbox = await authManager.refresh(saved.refresh);
    const mc = await xbox.getMinecraft();
    return { ok: true, profile: profileOf(mc, xbox) };
  } catch (e) {
    return { ok: false };
  }
});

ipcMain.handle('auth:logout', async () => {
  currentAuth = null;
  try { fs.unlinkSync(tokenPath()); } catch (e) { /* already gone */ }
  return { ok: true };
});

// ---------- game launch ----------

/** Best-effort search for a Java 8 executable on Windows (1.8.9 needs it). */
function findJava8() {
  const bases = [
    'C:/Program Files/Java',
    'C:/Program Files/Eclipse Adoptium',
    'C:/Program Files/Amazon Corretto',
    'C:/Program Files/Zulu',
    'C:/Program Files (x86)/Java'
  ];
  for (const base of bases) {
    try {
      for (const dir of fs.readdirSync(base)) {
        if (/(^|[^0-9])(1\.8|jdk-?8|jre-?8|8\.)/i.test(dir)) {
          const exe = path.join(base, dir, 'bin', 'java.exe');
          if (fs.existsSync(exe)) return exe;
        }
      }
    } catch (e) { /* base doesn't exist */ }
  }
  if (process.env.JAVA_HOME) {
    const exe = path.join(process.env.JAVA_HOME, 'bin', 'java.exe');
    if (fs.existsSync(exe)) return exe;
  }
  return null;
}

function status(msg) {
  if (mainWindow && !mainWindow.isDestroyed()) mainWindow.webContents.send('game:status', msg);
}

let lastDebug = '';
let sawGameData = false;

/**
 * Strips credentials before anything reaches the log. MCLC echoes the full
 * java command line, which contains --accessToken — a live session token that
 * grants account access on its own. The README asks users to share this file
 * when reporting bugs, so it must never contain one.
 */
function redact(s) {
  return String(s)
    .replace(/(--accessToken\s+)\S+/g, '$1<redacted>')
    .replace(/(--uuid\s+)\S+/g, '$1<redacted>')
    .replace(/(accessToken"?\s*[:=]\s*"?)[A-Za-z0-9._-]+/g, '$1<redacted>')
    .replace(/ey[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}/g, '<redacted-jwt>');
}

function logLine(tag, s) {
  const line = '[' + new Date().toISOString() + '] ' + tag + ' ' + redact(String(s).trim());
  console.log(line);
  try { fs.appendFileSync(path.join(app.getPath('userData'), 'launcher.log'), line + '\n'); } catch (e) { /* ignore */ }
}

launcher.on('progress', (e) => {
  const pct = e.total ? Math.round((e.task / e.total) * 100) : 0;
  status('Downloading ' + e.type + ' — ' + pct + '%');
});
launcher.on('arguments', () => status('Starting Minecraft…'));
launcher.on('debug', (line) => { lastDebug = String(line); logLine('DEBUG', line); });
launcher.on('data', (line) => { sawGameData = true; logLine('GAME', line); });
launcher.on('close', (code) => {
  logLine('CLOSE', 'code=' + code);
  if (sawGameData) { status('CLOSED'); }
  else { status('Error: ' + (lastDebug ? lastDebug.slice(0, 140) : 'game exited (code ' + code + ') — see launcher.log')); }
});

ipcMain.handle('game:launch', async () => {
  if (!currentAuth) return { ok: false, error: 'Sign in first' };

  const java = findJava8();
  if (!java) {
    return { ok: false, error: 'Java 8 not found — install it to run 1.8.9' };
  }

  try {
    const dir = gameDir();
    fs.mkdirSync(dir, { recursive: true });

    // Drop the bundled Ice Client + Schematica + LunatriusCore into the mods folder.
    const modsSrc = path.join(bundleDir(), 'mods');
    const modsDst = path.join(dir, 'mods');
    fs.mkdirSync(modsDst, { recursive: true });
    for (const f of fs.readdirSync(modsSrc)) {
      if (f.endsWith('.jar')) fs.copyFileSync(path.join(modsSrc, f), path.join(modsDst, f));
    }

    // Lay down our pre-built Forge 1.8.9 (version profile + its libraries) so MCLC
    // launches it as a custom version -- its own old-Forge installer path is broken.
    const forgeSrc = path.join(bundleDir(), 'forge');
    copyDir(path.join(forgeSrc, 'versions'), path.join(dir, 'versions'));
    copyDir(path.join(forgeSrc, 'libraries'), path.join(dir, 'libraries'));

    sawGameData = false;
    lastDebug = '';

    const ram = clampRam(readSettings().ramGb);
    // Give the JVM a floor of 1G, or the full allocation if the user picked a
    // small value — a min above max makes the JVM refuse to start.
    const minRam = Math.min(1, ram);

    logLine('LAUNCH', 'java=' + java + ' root=' + dir + ' forge=' + FORGE_ID + ' ram=' + ram + 'G');
    status('Preparing 1.8.9 + Forge + Ice Client (first launch takes a few minutes)…');

    const proc = await launcher.launch({
      authorization: currentAuth,
      root: dir,
      version: { number: '1.8.9', type: 'release', custom: FORGE_ID },
      memory: { max: ram + 'G', min: minRam + 'G' },
      javaPath: java
    });
    if (!proc) {
      return { ok: false, error: (lastDebug || 'launch failed').slice(0, 200) };
    }
    return { ok: true };
  } catch (e) {
    logLine('ERROR', (e && e.stack) || e);
    return { ok: false, error: String((e && e.message) || e) };
  }
});

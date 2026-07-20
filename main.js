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

// ---------- launcher self-update ----------
// Checks the VPS for a newer launcher build, downloads it in the background and
// installs it when the launcher is closed. Silent and offline-safe: if the
// server is unreachable it just logs and carries on.
const { autoUpdater } = require('electron-updater');
autoUpdater.autoDownload = true;
autoUpdater.autoInstallOnAppQuit = true;
autoUpdater.on('checking-for-update', () => logLine('SELFUPDATE', 'checking'));
autoUpdater.on('update-not-available', () => logLine('SELFUPDATE', 'launcher up to date'));
autoUpdater.on('update-available', (i) => {
  logLine('SELFUPDATE', 'downloading launcher ' + i.version);
  status('Downloading launcher update ' + i.version + '…');
});
autoUpdater.on('update-downloaded', (i) => {
  logLine('SELFUPDATE', 'ready ' + i.version);
  status('Launcher update ready — installs when you close it');
});
autoUpdater.on('error', (e) => logLine('SELFUPDATE', 'skipped: ' + ((e && e.message) || e)));

app.whenReady().then(() => {
  createWindow();
  // slight delay so the window is up before any status messages fire
  setTimeout(() => { try { autoUpdater.checkForUpdates(); } catch (e) { /* offline */ } }, 3000);
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

// ---------- versions ----------

// Launcher version comes from package.json; client version from whatever jar is
// actually installed (the update marker, else the bundled jar's filename).
ipcMain.handle('app:versions', async () => {
  let client = null;
  try { client = JSON.parse(fs.readFileSync(versionMarker(), 'utf8')).clientVersion; } catch (e) { /* not updated yet */ }
  if (!client) {
    try {
      const jar = fs.readdirSync(path.join(bundleDir(), 'mods')).find((n) => /^IceClient.*\.jar$/i.test(n));
      const m = jar && jar.match(/(\d+\.\d+\.\d+)/);
      if (m) client = m[1];
    } catch (e) { /* ignore */ }
  }
  return { launcher: app.getVersion(), client: client || '?' };
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
  // Prefer javaw.exe: that's what the official launcher uses, so the game shows
  // up as javaw.exe (what screenshare/anticheat tooling expects) and no console
  // window tags along. java.exe is the fallback.
  const names = ['javaw.exe', 'java.exe'];
  for (const base of bases) {
    try {
      for (const dir of fs.readdirSync(base)) {
        if (/(^|[^0-9])(1\.8|jdk-?8|jre-?8|8\.)/i.test(dir)) {
          for (const name of names) {
            const exe = path.join(base, dir, 'bin', name);
            if (fs.existsSync(exe)) return exe;
          }
        }
      }
    } catch (e) { /* base doesn't exist */ }
  }
  if (process.env.JAVA_HOME) {
    for (const name of names) {
      const exe = path.join(process.env.JAVA_HOME, 'bin', name);
      if (fs.existsSync(exe)) return exe;
    }
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
  // javaw.exe produces no console output, so a clean exit code counts as "ran fine"
  if (sawGameData || code === 0) { status('CLOSED'); }
  else { status('Error: ' + (lastDebug ? lastDebug.slice(0, 140) : 'game exited (code ' + code + ') — see launcher.log')); }
});

// ---------- auto-update ----------
// The VPS publishes a manifest; if it names a client build newer than what's
// installed we pull that jar into the mods folder before launching. Offline-safe:
// any failure is logged and skipped so the game still starts.
const http = require('http');
const https = require('https');

const UPDATE_MANIFEST = 'http://5.175.213.69/download/version.json';

function httpGet(url, cb) {
  return (url.startsWith('https') ? https : http).get(url, cb);
}

function fetchJson(url) {
  return new Promise((resolve, reject) => {
    httpGet(url, (r) => {
      if (r.statusCode !== 200) { r.resume(); return reject(new Error('HTTP ' + r.statusCode)); }
      let body = '';
      r.setEncoding('utf8');
      r.on('data', (c) => body += c);
      r.on('end', () => { try { resolve(JSON.parse(body)); } catch (e) { reject(e); } });
    }).on('error', reject);
  });
}

function downloadTo(url, dest) {
  return new Promise((resolve, reject) => {
    const file = fs.createWriteStream(dest);
    httpGet(url, (r) => {
      if (r.statusCode !== 200) { r.resume(); return reject(new Error('HTTP ' + r.statusCode)); }
      r.pipe(file);
      file.on('finish', () => file.close(() => resolve()));
    }).on('error', reject);
  });
}

const versionMarker = () => path.join(app.getPath('userData'), 'client-version.json');

async function syncClientUpdate(modsDst) {
  try {
    const m = await fetchJson(UPDATE_MANIFEST);
    if (!m || !m.clientVersion || !m.jar || !m.jar.url || !m.jar.name) return;

    const target = path.join(modsDst, m.jar.name);
    let installed = null;
    try { installed = JSON.parse(fs.readFileSync(versionMarker(), 'utf8')).clientVersion; } catch (e) { /* first run */ }

    const needsDownload = (installed !== m.clientVersion) || !fs.existsSync(target);
    if (needsDownload) {
      status('Updating Ice Client to ' + m.clientVersion + '…');
      const tmp = path.join(app.getPath('userData'), 'update.jar');
      await downloadTo(m.jar.url, tmp);
      fs.copyFileSync(tmp, target);
      fs.writeFileSync(versionMarker(), JSON.stringify({ clientVersion: m.clientVersion }));
      logLine('UPDATE', 'installed ' + m.jar.name + ' (' + m.clientVersion + ')');
    }

    // ALWAYS prune other client jars. The bundled one gets re-copied every launch,
    // and two copies of the mod makes Forge refuse to start (duplicate mod id).
    for (const f of fs.readdirSync(modsDst)) {
      if (/^IceClient.*\.jar$/i.test(f) && f !== m.jar.name) {
        fs.unlinkSync(path.join(modsDst, f));
        logLine('UPDATE', 'removed stale ' + f);
      }
    }
  } catch (e) {
    logLine('UPDATE', 'skipped: ' + ((e && e.message) || e));
  }
}

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

    // then pull a newer client build from the VPS, if one is published
    await syncClientUpdate(modsDst);

    // Safety net: Forge refuses to start if two copies of the mod are present.
    // This runs no matter what (even if the update check failed or we're offline):
    // keep only the newest IceClient jar, delete any others.
    try {
      const jars = fs.readdirSync(modsDst).filter((f) => /^IceClient.*\.jar$/i.test(f));
      if (jars.length > 1) {
        let newest = null;
        let newestTime = -1;
        for (const f of jars) {
          const t = fs.statSync(path.join(modsDst, f)).mtimeMs;
          if (t > newestTime) { newestTime = t; newest = f; }
        }
        for (const f of jars) {
          if (f !== newest) {
            fs.unlinkSync(path.join(modsDst, f));
            logLine('UPDATE', 'removed duplicate ' + f);
          }
        }
      }
    } catch (e) {
      logLine('UPDATE', 'dedupe failed: ' + ((e && e.message) || e));
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

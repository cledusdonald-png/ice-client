/**
 * Ice Client points + cosmetics service.
 *
 * Balances and unlocks live here rather than in each player's config, because a
 * config is a text file anyone can edit -- points kept client-side are worth
 * nothing and a giveaway would be theatre.
 *
 * Deliberately small: one JSON file, no database, no auth beyond an admin key.
 * That is the right size for a faction of five. It is NOT hardened -- the client
 * simply states its own name, so someone who wanted to could claim to be
 * another player. Fine for points; do not put money behind it without proper
 * session verification.
 *
 * Deploy:
 *   /opt/ice/ice-points.js      (this file)
 *   /opt/ice/points.json        (created automatically)
 *
 * Run behind the existing nginx /api/ proxy on port 3001.
 */

const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const PORT = 3001;
const DATA = path.join(__dirname, 'points.json');

// Change this. Anyone with it can mint points.
const ADMIN_KEY = process.env.ICE_ADMIN_KEY || 'change-me';

// Prices mirror CosmeticRegistry in the client. They are repeated here on
// purpose: the server must not trust a price the client sends it.
const PRICES = {
  cape_ice: 0,
  cape_frost: 500,
  cape_aurora: 1500,
  cape_obsidian: 1500,
  cape_founder: -1,      // granted only

  hat_beanie: 200,
  hat_halo: 800,
  hat_crown: 1200,

  wings_frost: 1800,
  wings_shadow: 3000,

  pet_snowman: 900,
  pet_polarbear: 1800,

  trail_frost: 300,
  trail_aurora: 1400,

  emote_wave: 0,
  emote_sit: 400,
  emote_floss: 1200,
};

// ---------------------------------------------------------------- storage

let db = { players: {}, codes: {} };

/**
 * Who is wearing what, in memory only.
 *
 * Deliberately not persisted: this is presence, and after a restart nobody is
 * online -- reloading it would resurrect stale state and show capes on players
 * who left hours ago. Entries expire on their own so a crashed client does not
 * linger forever.
 */
const worn = new Map();
const WORN_TTL_MS = 90000;

function load() {
  try {
    db = JSON.parse(fs.readFileSync(DATA, 'utf8'));
    db.players = db.players || {};
    db.codes = db.codes || {};
  } catch (e) {
    console.log('[ice] starting with an empty database');
  }
}

let saveTimer = null;
function save() {
  // Debounced: a burst of redeems should not mean a burst of disk writes.
  if (saveTimer) return;
  saveTimer = setTimeout(() => {
    saveTimer = null;
    try {
      fs.writeFileSync(DATA + '.tmp', JSON.stringify(db, null, 2));
      fs.renameSync(DATA + '.tmp', DATA);   // atomic: never a half-written file
    } catch (e) {
      console.error('[ice] save failed:', e.message);
    }
  }, 400);
}

function player(name) {
  const key = String(name || '').toLowerCase();
  if (!/^[a-z0-9_]{3,16}$/.test(key)) return null;

  if (!db.players[key]) {
    db.players[key] = { name: name, balance: 0, owned: [], redeemed: [] };
  }
  return db.players[key];
}

function state(p) {
  return { balance: p.balance, owned: p.owned };
}

// ---------------------------------------------------------------- helpers

function send(res, code, obj) {
  const body = JSON.stringify(obj);
  res.writeHead(code, {
    'Content-Type': 'application/json',
    'Content-Length': Buffer.byteLength(body),
  });
  res.end(body);
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let raw = '';
    req.on('data', (c) => {
      raw += c;
      if (raw.length > 8192) req.destroy();     // nobody needs to POST more
    });
    req.on('end', () => {
      try { resolve(raw ? JSON.parse(raw) : {}); } catch (e) { reject(e); }
    });
    req.on('error', reject);
  });
}

function newCode() {
  // Short, unambiguous, readable over voice comms: no O/0 or I/1.
  const alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  let out = '';
  for (let i = 0; i < 6; i++) {
    out += alphabet[crypto.randomInt(alphabet.length)];
  }
  return out;
}

// ---------------------------------------------------------------- routes

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, 'http://localhost');
  const route = url.pathname.replace(/^\/api/, '');

  try {
    // ---- GET /points?name=Steve
    if (req.method === 'GET' && route === '/points') {
      const p = player(url.searchParams.get('name'));
      if (!p) return send(res, 400, { error: 'Bad name.' });
      return send(res, 200, state(p));
    }

    // ---- POST /redeem { name, code }
    if (req.method === 'POST' && route === '/redeem') {
      const body = await readBody(req);
      const p = player(body.name);
      if (!p) return send(res, 400, { error: 'Bad name.' });

      const code = String(body.code || '').toUpperCase().trim();
      const c = db.codes[code];

      if (!c) return send(res, 404, { error: 'That code is not valid.' });
      if (c.expires && Date.now() > c.expires) {
        return send(res, 410, { error: 'That code has expired.' });
      }
      if (c.uses >= c.maxUses) {
        return send(res, 410, { error: 'That code has been fully claimed.' });
      }
      // One claim each, so a single person cannot drain a multi-use code.
      if (p.redeemed.includes(code)) {
        return send(res, 409, { error: "You've already used that code." });
      }

      c.uses += 1;
      p.redeemed.push(code);
      p.balance += c.amount;

      if (c.grants && !p.owned.includes(c.grants)) {
        p.owned.push(c.grants);
      }

      save();
      console.log(`[ice] ${p.name} redeemed ${code} (+${c.amount})`);
      return send(res, 200, Object.assign({ gained: c.amount }, state(p)));
    }

    // ---- POST /buy { name, item }
    if (req.method === 'POST' && route === '/buy') {
      const body = await readBody(req);
      const p = player(body.name);
      if (!p) return send(res, 400, { error: 'Bad name.' });

      const item = String(body.item || '');
      const price = PRICES[item];

      if (price === undefined) return send(res, 404, { error: "That item doesn't exist." });
      if (price < 0) return send(res, 403, { error: "That one can't be bought." });
      if (p.owned.includes(item)) return send(res, 409, { error: 'You already own that.' });
      if (price > 0 && p.balance < price) {
        return send(res, 402, { error: `Not enough — you need ${price - p.balance} more.` });
      }

      p.balance -= price;
      p.owned.push(item);
      save();
      console.log(`[ice] ${p.name} bought ${item} (-${price})`);
      return send(res, 200, state(p));
    }

    // ---- POST /worn { name, server, equipped:{SLOT:id} }
    // Says what you are wearing, and returns what everyone else on your server
    // is wearing. This is what makes cosmetics visible to other people at all.
    //
    // Held in memory rather than saved: it is presence, not ownership, and a
    // restart should forget who was online rather than resurrect stale state.
    if (req.method === 'POST' && route === '/worn') {
      const body = await readBody(req);
      const p = player(body.name);
      if (!p) return send(res, 400, { error: 'Bad name.' });

      const key = String(body.name).toLowerCase();
      const srv = String(body.server || 'unknown').toLowerCase().slice(0, 80);

      // Only report cosmetics the player actually owns, so editing a config
      // cannot put a legendary cape on someone who never earned it.
      const claimed = body.equipped && typeof body.equipped === 'object' ? body.equipped : {};
      const equipped = {};
      for (const slot of Object.keys(claimed)) {
        const id = String(claimed[slot] || '');
        if (!id) continue;
        const free = PRICES[id] === 0;
        if (free || p.owned.includes(id)) equipped[slot] = id;
      }

      worn.set(key, { name: p.name, server: srv, equipped, seen: Date.now() });

      // Everyone else seen on the same server recently.
      const cutoff = Date.now() - WORN_TTL_MS;
      const others = {};
      for (const [k, v] of worn) {
        if (v.seen < cutoff) { worn.delete(k); continue; }
        if (k === key || v.server !== srv) continue;
        others[v.name] = v.equipped;
      }

      return send(res, 200, { players: others });
    }

    // ---- POST /admin/code { key, amount, uses, days, grants }
    // Creates a giveaway code. This is the giveaway button.
    if (req.method === 'POST' && route === '/admin/code') {
      const body = await readBody(req);
      if (body.key !== ADMIN_KEY) return send(res, 403, { error: 'Nope.' });

      const code = (body.code || newCode()).toUpperCase();
      db.codes[code] = {
        amount: Math.max(0, parseInt(body.amount, 10) || 0),
        maxUses: Math.max(1, parseInt(body.uses, 10) || 1),
        uses: 0,
        grants: body.grants || null,
        expires: body.days ? Date.now() + body.days * 86400000 : null,
      };

      save();
      console.log(`[ice] created code ${code}`);
      return send(res, 200, { code, ...db.codes[code] });
    }

    // ---- POST /admin/give { key, name, amount }
    // Straight grant to one player, for when you just want to hand someone points.
    if (req.method === 'POST' && route === '/admin/give') {
      const body = await readBody(req);
      if (body.key !== ADMIN_KEY) return send(res, 403, { error: 'Nope.' });

      const p = player(body.name);
      if (!p) return send(res, 400, { error: 'Bad name.' });

      p.balance = Math.max(0, p.balance + (parseInt(body.amount, 10) || 0));
      if (body.grants && !p.owned.includes(body.grants)) p.owned.push(body.grants);

      save();
      console.log(`[ice] gave ${p.name} ${body.amount}`);
      return send(res, 200, state(p));
    }

    // ---- GET /admin/list?key=...
    if (req.method === 'GET' && route === '/admin/list') {
      if (url.searchParams.get('key') !== ADMIN_KEY) return send(res, 403, { error: 'Nope.' });
      return send(res, 200, { players: db.players, codes: db.codes });
    }

    send(res, 404, { error: 'No such endpoint.' });
  } catch (e) {
    console.error('[ice]', e);
    send(res, 500, { error: 'Server error.' });
  }
});

load();
server.listen(PORT, '127.0.0.1', () => {
  console.log(`[ice] points service on 127.0.0.1:${PORT}`);
  if (ADMIN_KEY === 'change-me') {
    console.warn('[ice] WARNING: ADMIN_KEY is still the default. Set ICE_ADMIN_KEY.');
  }
});

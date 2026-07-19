// Safe bridge between the UI and the main process. Only exposes the two
// window-control calls the titlebar needs -- no Node access leaks to the page.
const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('win', {
  minimize: () => ipcRenderer.send('win:minimize'),
  close: () => ipcRenderer.send('win:close')
});

contextBridge.exposeInMainWorld('auth', {
  login: () => ipcRenderer.invoke('auth:login'),
  restore: () => ipcRenderer.invoke('auth:restore'),
  logout: () => ipcRenderer.invoke('auth:logout')
});

contextBridge.exposeInMainWorld('game', {
  launch: () => ipcRenderer.invoke('game:launch'),
  onStatus: (cb) => ipcRenderer.on('game:status', (e, msg) => cb(msg))
});

contextBridge.exposeInMainWorld('settings', {
  get: () => ipcRenderer.invoke('settings:get'),
  set: (patch) => ipcRenderer.invoke('settings:set', patch)
});

/**
 * MV3 后台服务 worker（仅事件驱动）。
 *
 * 管理后台界面复杂，不适合塞进浏览器右上角的小 popup，
 * 因此点击工具栏图标时在新标签页打开完整控制台（与网页版同一套前端构建）。
 */
chrome.action.onClicked.addListener(() => {
  chrome.tabs.create({ url: chrome.runtime.getURL('index.html') })
})

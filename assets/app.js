/* JCM Yanyang 脚本文档 — 主题切换 / 语言切换 / 侧栏开合 / 轻量代码高亮（无外部依赖） */
(function () {
  "use strict";

  /* ---- 语言清单（与站点目录一致；名称以各语言原文显示，无需翻译） ---- */
  var LOCALES = [
    { code: "zh-CN", name: "简体中文", region: "中国大陆", prefix: "zh" },
    { code: "zh-HK", name: "繁體中文", region: "香港特別行政區", prefix: "zh" },
    { code: "zh-TW", name: "繁體中文", region: "台灣", prefix: "zh" },
    { code: "lzh", name: "文言", region: "華夏", prefix: "zh" },
    { code: "en-US", name: "English", region: "United States", prefix: "en" },
    { code: "en-GB", name: "English", region: "United Kingdom", prefix: "en" },
    { code: "fr-FR", name: "Français", region: "France", prefix: "fr" }
  ];

  /* ---- 输入方式追踪（桌面触摸屏 / 二合一设备） ----
     触摸会话 → <html data-input="touch">：CSS 压制 hover 表现，避免“粘滞悬停”；
     鼠标/触控笔再次使用 → 恢复 fine。初始值按指针能力推断。 */
  var root = document.documentElement;

  function setInput(type) {
    if (root.getAttribute("data-input") !== type) root.setAttribute("data-input", type);
  }

  try {
    setInput(window.matchMedia("(any-pointer: coarse)").matches ? "touch" : "fine");
  } catch (e) { setInput("fine"); }

  if (window.PointerEvent) {
    window.addEventListener("pointerdown", function (e) {
      setInput(e.pointerType === "touch" ? "touch" : "fine");
    }, true);
    window.addEventListener("pointermove", function (e) {
      if (e.pointerType === "mouse" && root.getAttribute("data-input") === "touch") {
        setInput("fine");
      }
    }, true);
  }

  /* ---- 表格滚动容器 ----
     为每张表包一层 .table-wrap（overflow-x: auto），表格恢复标准表格布局：
     width:100% 铺满优先，列宽地板之下不压缩；整体最小内容超宽时由外层滚动。
     无 JS 时回退为 table 自身 display:block + overflow（样式表中保留）。 */
  document.querySelectorAll("table").forEach(function (t) {
    var wrap = document.createElement("div");
    wrap.className = "table-wrap";
    t.parentNode.insertBefore(wrap, t);
    wrap.appendChild(t);
  });

  /* ---- 浅色 / 深色主题 ----
     首次访问跟随系统偏好；手动切换后写入 localStorage("jcm-theme")。
     防闪烁的内联脚本已在各页 <head> 中先行设置 data-theme。 */
  function currentTheme() {
    return document.documentElement.getAttribute("data-theme") === "dark" ? "dark" : "light";
  }

  function applyTheme(t) {
    document.documentElement.setAttribute("data-theme", t);
    try { localStorage.setItem("jcm-theme", t); } catch (e) { /* 私密模式等 */ }
    updateThemeButtons();
  }

  function updateThemeButtons() {
    var dark = currentTheme() === "dark";
    document.querySelectorAll(".theme-toggle").forEach(function (btn) {
      var icon = btn.querySelector(".theme-icon");
      var label = btn.querySelector(".theme-label");
      if (icon) icon.textContent = dark ? "☀️" : "🌙";
      if (label) {
        label.textContent = dark
          ? (label.getAttribute("data-light") || "Light")
          : (label.getAttribute("data-dark") || "Dark");
      }
      btn.setAttribute("aria-label", dark ? "Switch to light theme" : "Switch to dark theme");
    });
  }

  document.querySelectorAll(".theme-toggle").forEach(function (btn) {
    btn.addEventListener("click", function () {
      applyTheme(currentTheme() === "dark" ? "light" : "dark");
    });
  });
  updateThemeButtons();

  /* ---- 语言 ----
     记住当前语言；下拉框切换到同页面的其它语言版本。 */
  var locale = document.documentElement.getAttribute("data-locale") || "";
  if (locale) {
    try { localStorage.setItem("jcm-lang", locale); } catch (e) { /* 忽略 */ }
  }

  var select = document.querySelector(".lang-select");
  if (select) {
    LOCALES.forEach(function (l) {
      var opt = document.createElement("option");
      opt.value = l.code;
      opt.textContent = l.name + "（" + l.region + "）";
      if (l.code === locale) opt.selected = true;
      select.appendChild(opt);
    });
    select.addEventListener("change", function () {
      var page = location.pathname.split("/").pop() || "index.html";
      location.href = "../" + select.value + "/" + page;
    });
  }

  /* 语言选择页：按浏览器语言标注「推荐」卡片 */
  var cards = document.querySelectorAll(".lang-card");
  if (cards.length) {
    var prefs = [];
    try {
      prefs = (navigator.languages && navigator.languages.length)
        ? navigator.languages.slice()
        : [navigator.language || ""];
    } catch (e) { /* 忽略 */ }

    var suggested = null;
    for (var i = 0; i < prefs.length && !suggested; i++) {
      var p = (prefs[i] || "").toLowerCase();
      if (!p) continue;
      for (var j = 0; j < cards.length; j++) {
        var code = cards[j].getAttribute("data-lang") || "";
        if (code.toLowerCase() === p) { suggested = cards[j]; break; }
      }
      if (!suggested) {
        /* 无地区后缀时按主语言挑选：zh → zh-CN，en → en-US，fr → fr-FR */
        var primary = p.split("-")[0];
        var fallback = { zh: "zh-CN", en: "en-US", fr: "fr-FR", lzh: "lzh" }[primary];
        if (fallback) {
          for (var k = 0; k < cards.length; k++) {
            if (cards[k].getAttribute("data-lang") === fallback) { suggested = cards[k]; break; }
          }
        }
      }
    }
    if (suggested) suggested.classList.add("suggested");
  }

  /* ---- 移动端抽屉侧栏：开合 / 遮罩 / 滚动锁定 / 滑动关闭 ---- */
  var sidebar = document.querySelector(".sidebar");
  var btn = document.querySelector(".topbar .menu-btn");
  if (sidebar && btn) {
    btn.setAttribute("aria-expanded", "false");

    function drawerIsOpen() {
      return sidebar.classList.contains("open");
    }

    function openDrawer() {
      sidebar.classList.add("open");
      document.body.classList.add("nav-open");
      btn.setAttribute("aria-expanded", "true");
    }

    function closeDrawer() {
      sidebar.classList.remove("open");
      document.body.classList.remove("nav-open");
      btn.setAttribute("aria-expanded", "false");
    }

    btn.addEventListener("click", function () {
      drawerIsOpen() ? closeDrawer() : openDrawer();
    });

    /* 点击抽屉外任意处（含遮罩）关闭 */
    document.addEventListener("click", function (e) {
      if (drawerIsOpen() && !sidebar.contains(e.target) && !btn.contains(e.target)) {
        closeDrawer();
      }
    });

    /* Esc 关闭并把焦点还给菜单按钮 */
    document.addEventListener("keydown", function (e) {
      if (e.key === "Escape" && drawerIsOpen()) {
        closeDrawer();
        btn.focus();
      }
    });

    /* 触屏：向左滑动关闭抽屉（跟手位移，超过 70px 关闭，纵向滚动不干扰） */
    var sx = null, sy = 0, dx = 0, dragging = false;

    sidebar.addEventListener("touchstart", function (e) {
      if (!drawerIsOpen()) return;
      sx = e.touches[0].clientX;
      sy = e.touches[0].clientY;
      dx = 0;
      dragging = false;
    }, { passive: true });

    sidebar.addEventListener("touchmove", function (e) {
      if (sx === null) return;
      var mx = e.touches[0].clientX - sx;
      var my = e.touches[0].clientY - sy;
      if (!dragging) {
        if (Math.abs(mx) > 10 && Math.abs(mx) > Math.abs(my)) {
          dragging = true;
          sidebar.style.transition = "none";
        } else if (Math.abs(my) > 10) {
          sx = null; /* 纵向滚动：让给页面 */
          return;
        }
      }
      if (dragging) {
        dx = Math.min(0, mx);
        sidebar.style.transform = "translateX(" + dx + "px)";
      }
    }, { passive: true });

    function endSwipe() {
      if (!dragging) { sx = null; return; }
      sidebar.style.transition = "";
      sidebar.style.transform = "";
      dragging = false;
      sx = null;
      if (dx < -70) closeDrawer();
    }

    sidebar.addEventListener("touchend", endSwipe, { passive: true });
    sidebar.addEventListener("touchcancel", endSwipe, { passive: true });

    /* 跨断点（如旋转平板）时清理抽屉状态，避免 ≥ 961px 留下遮罩/滚动锁 */
    var mql = window.matchMedia("(max-width: 960px)");
    var onMq = function (e) {
      if (!e.matches && drawerIsOpen()) closeDrawer();
    };
    if (mql.addEventListener) mql.addEventListener("change", onMq);
    else if (mql.addListener) mql.addListener(onMq);
  }

  /* ---- 代码块复制按钮 ----
     由 JS 注入（免改 HTML）：pre 外套一层 .code-block-wrap，按钮悬于右上角，
     不随代码横向滚动移动；纯触屏常显，鼠标悬停代码块时显示。
     文案按页面语言本地化。 */
  var COPY_LABELS = {
    "zh-CN": { copy: "复制", ok: "已复制", fail: "复制失败" },
    "zh-HK": { copy: "複製", ok: "已複製", fail: "複製失敗" },
    "zh-TW": { copy: "複製", ok: "已複製", fail: "複製失敗" },
    "lzh": { copy: "錄", ok: "已錄", fail: "弗克錄" },
    "en-US": { copy: "Copy", ok: "Copied!", fail: "Copy failed" },
    "en-GB": { copy: "Copy", ok: "Copied!", fail: "Copy failed" },
    "fr-FR": { copy: "Copier", ok: "Copié !", fail: "Échec de la copie" }
  };
  var ICON_COPY = '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>';
  var ICON_OK = '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M20 6 9 17l-5-5"/></svg>';
  var ICON_ERR = '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M18 6 6 18M6 6l12 12"/></svg>';

  function legacyCopy(text) {
    try {
      var ta = document.createElement("textarea");
      ta.value = text;
      ta.setAttribute("readonly", "");
      ta.style.position = "fixed";
      ta.style.top = "-1000px";
      ta.style.opacity = "0";
      document.body.appendChild(ta);
      ta.focus();
      ta.select();
      ta.setSelectionRange(0, text.length);
      var ok = document.execCommand("copy");
      document.body.removeChild(ta);
      return ok;
    } catch (e) {
      return false;
    }
  }

  function copyText(text, done, fail) {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(text).then(done, function () {
        legacyCopy(text) ? done() : fail();
      });
    } else if (legacyCopy(text)) {
      done();
    } else {
      fail();
    }
  }

  var COPY_L = COPY_LABELS[locale] || COPY_LABELS["en-US"];

  document.querySelectorAll("pre").forEach(function (pre) {
    var code = null;
    for (var ci = 0; ci < pre.children.length; ci++) {
      if (pre.children[ci].tagName === "CODE") { code = pre.children[ci]; break; }
    }
    if (!code) return;

    var wrap = document.createElement("div");
    wrap.className = "code-block-wrap";
    pre.parentNode.insertBefore(wrap, pre);
    wrap.appendChild(pre);

    var copyBtn = document.createElement("button");
    copyBtn.type = "button";
    copyBtn.className = "copy-btn";
    copyBtn.setAttribute("aria-label", COPY_L.copy);
    copyBtn.innerHTML = ICON_COPY;

    var revertTimer = null;
    function setBtnState(state) {
      copyBtn.classList.remove("ok", "err");
      if (state === "ok") {
        copyBtn.classList.add("ok");
        copyBtn.innerHTML = ICON_OK + '<span class="copy-txt">' + esc(COPY_L.ok) + "</span>";
        copyBtn.setAttribute("aria-label", COPY_L.ok);
      } else if (state === "err") {
        copyBtn.classList.add("err");
        copyBtn.innerHTML = ICON_ERR + '<span class="copy-txt">' + esc(COPY_L.fail) + "</span>";
        copyBtn.setAttribute("aria-label", COPY_L.fail);
      } else {
        copyBtn.innerHTML = ICON_COPY;
        copyBtn.setAttribute("aria-label", COPY_L.copy);
      }
    }

    copyBtn.addEventListener("click", function () {
      copyText(code.textContent,
        function () {
          setBtnState("ok");
          if (revertTimer) clearTimeout(revertTimer);
          revertTimer = setTimeout(function () { setBtnState("idle"); }, 1800);
        },
        function () {
          setBtnState("err");
          if (revertTimer) clearTimeout(revertTimer);
          revertTimer = setTimeout(function () { setBtnState("idle"); }, 2400);
        });
    });

    wrap.appendChild(copyBtn);
  });

  /* ---- 轻量语法高亮 ----
     对 <pre><code class="lang-cpp|json|bash|cmake"> 做保守的词法着色：
     注释、字符串、数字、常见关键字。失败或未标记的代码块原样显示。 */
  var CPP_KW = new Set(("alignas alignof and auto bool break case catch char char8_t char16_t "
    + "char32_t class const consteval constexpr constinit const_cast continue decltype default "
    + "delete do double dynamic_cast else enum explicit export extern false float for friend "
    + "goto if inline int long mutable namespace new noexcept not nullptr operator or override "
    + "private protected public register reinterpret_cast requires return short signed sizeof "
    + "static static_assert static_cast struct switch template this thread_local throw true try "
    + "typedef typeid typename union unsigned using virtual void volatile while"
    + " include define ifndef endif pragma").split(/\s+/));

  var JS_KW = new Set(("var let const function return if else for while new class extends true "
    + "false null undefined this typeof instanceof of in break continue").split(/\s+/));

  function esc(s) {
    return s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
  }

  function highlightCpp(src, kw) {
    var out = "";
    var i = 0, n = src.length;
    while (i < n) {
      var c = src[i];
      // 行注释与块注释
      if (c === "/" && src[i + 1] === "/") {
        var j = src.indexOf("\n", i);
        if (j < 0) j = n;
        out += '<span class="tok-c">' + esc(src.slice(i, j)) + "</span>";
        i = j;
        continue;
      }
      if (c === "/" && src[i + 1] === "*") {
        var k = src.indexOf("*/", i + 2);
        if (k < 0) k = n - 2;
        out += '<span class="tok-c">' + esc(src.slice(i, k + 2)) + "</span>";
        i = k + 2;
        continue;
      }
      // 字符串与字符
      if (c === '"' || c === "'") {
        var q = c, m = i + 1;
        while (m < n && src[m] !== q) {
          if (src[m] === "\\") m++;
          m++;
        }
        m = Math.min(m + 1, n);
        out += '<span class="tok-s">' + esc(src.slice(i, m)) + "</span>";
        i = m;
        continue;
      }
      // 数字
      if (/[0-9]/.test(c) && (i === 0 || !/[\w.]/.test(src[i - 1]))) {
        var m2 = i;
        while (m2 < n && /[0-9a-fA-FxXulL.fF+-]/.test(src[m2]) &&
               (/[0-9a-fA-FxXulL.fF]/.test(src[m2]) ||
                ((src[m2] === "+" || src[m2] === "-") && /[eE]/.test(src[m2 - 1])))) m2++;
        out += '<span class="tok-n">' + esc(src.slice(i, m2)) + "</span>";
        i = m2;
        continue;
      }
      // 标识符 / 关键字 / 调用
      if (/[A-Za-z_]/.test(c)) {
        var m3 = i;
        while (m3 < n && /[A-Za-z0-9_]/.test(src[m3])) m3++;
        var word = src.slice(i, m3);
        var after = src.slice(m3).match(/^\s*\(/);
        if (kw.has(word)) {
          out += '<span class="tok-k">' + word + "</span>";
        } else if (/^(mtr|MTR|Jcm)[_A-Z]/.test(word) || /^[A-Z][A-Za-z0-9_]*$/.test(word)) {
          out += '<span class="tok-t">' + word + "</span>";
        } else if (after) {
          out += '<span class="tok-f">' + word + "</span>";
        } else {
          out += word;
        }
        i = m3;
        continue;
      }
      out += esc(c);
      i++;
    }
    return out;
  }

  function highlightJson(src) {
    var out = "";
    var i = 0, n = src.length;
    while (i < n) {
      var c = src[i];
      if (c === '"') {
        var m = i + 1;
        while (m < n && src[m] !== '"') { if (src[m] === "\\") m++; m++; }
        m = Math.min(m + 1, n);
        var seg = src.slice(i, m);
        // 键（后跟冒号）与普通字符串
        var rest = src.slice(m);
        out += '<span class="' + (/^\s*:/.test(rest) ? "tok-f" : "tok-s") + '">' +
               esc(seg) + "</span>";
        i = m;
        continue;
      }
      if (c === "/" && src[i + 1] === "/") {
        var j2 = src.indexOf("\n", i);
        if (j2 < 0) j2 = n;
        out += '<span class="tok-c">' + esc(src.slice(i, j2)) + "</span>";
        i = j2;
        continue;
      }
      if (/[0-9-]/.test(c) && /[0-9]/.test(src[i + 1] || "")) {
        var m4 = i + 1;
        while (m4 < n && /[0-9.eE+-]/.test(src[m4])) m4++;
        out += '<span class="tok-n">' + esc(src.slice(i, m4)) + "</span>";
        i = m4;
        continue;
      }
      out += esc(c);
      i++;
    }
    return out;
  }

  function highlightBash(src) {
    var out = "";
    src.split("\n").forEach(function (line, idx, arr) {
      var c = line.indexOf("#");
      var code = c >= 0 ? line.slice(0, c) : line;
      var comment = c >= 0 ? line.slice(c) : "";
      out += esc(code);
      if (comment) out += '<span class="tok-c">' + esc(comment) + "</span>";
      if (idx < arr.length - 1) out += "\n";
    });
    return out;
  }

  document.querySelectorAll("pre > code").forEach(function (block) {
    var cls = (block.className || "").toLowerCase();
    var lang = null;
    if (cls.indexOf("cpp") >= 0 || cls.indexOf("c++") >= 0) lang = "cpp";
    else if (cls.indexOf("json") >= 0) lang = "json";
    else if (cls.indexOf("bash") >= 0 || cls.indexOf("sh") >= 0) lang = "bash";
    else if (cls.indexOf("cmake") >= 0) lang = "cmake";
    if (!lang) return;
    try {
      var src = block.textContent;
      if (lang === "cpp" || lang === "cmake") {
        block.innerHTML = highlightCpp(src, lang === "cmake" ? new Set(["if", "else", "endif", "set", "function", "endfunction", "foreach", "endforeach", "include", "option", "add_library", "target_link_libraries", "add_executable"]) : CPP_KW);
      } else if (lang === "json") {
        block.innerHTML = highlightJson(src);
      } else {
        block.innerHTML = highlightBash(src);
      }
    } catch (e) { /* 保持原样 */ }
  });

  /* ---- 宽表格横向滚动：边缘渐隐提示 ----
     CSS 为表格列设了保底宽度，窄屏下不再压缩比例；仍放不下的表格
     依赖 .table-wrap 横向滚动。此处依滚动位置切换 can-l / can-r，
     CSS 用 mask 在对应边缘渐隐，提示尚有内容可滑。 */
  function syncTableEdges() {
    document.querySelectorAll(".table-wrap").forEach(function (w) {
      var max = w.scrollWidth - w.clientWidth;
      if (max <= 4) {
        w.classList.remove("can-l", "can-r");
        return;
      }
      w.classList.toggle("can-r", w.scrollLeft < max - 4);
      w.classList.toggle("can-l", w.scrollLeft > 4);
    });
  }

  document.querySelectorAll(".table-wrap").forEach(function (w) {
    w.addEventListener("scroll", syncTableEdges, { passive: true });
  });
  window.addEventListener("resize", syncTableEdges);
  window.addEventListener("orientationchange", syncTableEdges);
  if (document.fonts && document.fonts.ready) {
    document.fonts.ready.then(syncTableEdges).catch(function () { /* 忽略 */ });
  }
  syncTableEdges();
})();

/* JCM Yanyang 脚本文档 — 侧栏开合 + 轻量代码高亮（无外部依赖） */
(function () {
  "use strict";

  /* ---- 移动端侧栏开合 ---- */
  var sidebar = document.querySelector(".sidebar");
  var btn = document.querySelector(".topbar .menu-btn");
  if (sidebar && btn) {
    btn.addEventListener("click", function () {
      sidebar.classList.toggle("open");
    });
    document.addEventListener("click", function (e) {
      if (sidebar.classList.contains("open") &&
          !sidebar.contains(e.target) && e.target !== btn) {
        sidebar.classList.remove("open");
      }
    });
  }

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
})();

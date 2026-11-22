</main>
<#if guildId?? && (tabbar!false)>
<nav class="tabbar" aria-label="メイン">
  <a href="/g/${guildId?c}/events"<#if (nav!"") == "events"> aria-current="page"</#if>>
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M16 3v4M8 3v4M3 10h18"/></svg>
    <span>イベント</span>
  </a>
  <a href="/g/${guildId?c}/me"<#if (nav!"") == "me"> aria-current="page"</#if>>
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/></svg>
    <span>マイページ</span>
  </a>
  <#if isAdmin?? && isAdmin>
    <a href="/g/${guildId?c}/settings"<#if (nav!"") == "settings"> aria-current="page"</#if>>
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 7h9M17 7h3M4 17h3M11 17h9"/><circle cx="15" cy="7" r="2"/><circle cx="9" cy="17" r="2"/></svg>
      <span>設定</span>
    </a>
  </#if>
</nav>
</#if>
<script>
  (function () {
    // hx-boost は body の innerHTML を差し替えるだけで body 自体は再利用されるため、
    // この script はナビゲーションの度に再実行される。リスナーの多重登録を防ぐため
    // 初回のみ document.body に束縛し、要素参照はイベント発火のたびに取り直す
    if (window.__uiBound) return;
    window.__uiBound = true;
    var hideTimer = null;
    document.body.addEventListener("htmx:beforeRequest", function () {
      var bar = document.getElementById("progress-bar");
      if (!bar) return;
      clearTimeout(hideTimer);
      bar.style.transition = "none";
      bar.style.width = "0";
      bar.offsetHeight;
      bar.style.transition = "";
      bar.classList.add("active");
      bar.style.width = "80%";
    });
    document.body.addEventListener("htmx:afterRequest", function () {
      var bar = document.getElementById("progress-bar");
      if (!bar) return;
      bar.style.width = "100%";
      hideTimer = setTimeout(function () {
        bar.classList.remove("active");
        bar.style.width = "0";
      }, 250);
    });

    function setManageOpen(open) {
      var manage = document.querySelector("[data-manage]");
      if (!manage) return;
      manage.classList.toggle("is-open", open);
      document.querySelectorAll("[data-manage-toggle]").forEach(function (b) {
        b.setAttribute("aria-expanded", open ? "true" : "false");
      });
    }

    document.body.addEventListener("click", function (e) {
      // スマホ幅のイベント詳細: 「お店 / 参加者」タブの切り替え
      var tab = e.target.closest("[data-tab-target]");
      if (tab) {
        var container = tab.closest("[data-tabs]");
        container.dataset.tab = tab.dataset.tabTarget;
        container.querySelectorAll("[data-tab-target]").forEach(function (t) {
          t.setAttribute("aria-selected", t === tab ? "true" : "false");
        });
        return;
      }
      // スマホ幅のイベント詳細: 「⋯」で管理メニューを開閉する
      if (e.target.closest("[data-manage-toggle]")) {
        var manage = document.querySelector("[data-manage]");
        setManageOpen(!(manage && manage.classList.contains("is-open")));
        return;
      }
      if (e.target.closest("[data-manage-close]")) {
        setManageOpen(false);
        return;
      }
      // 出欠フォーム: 全員を出席にする
      var markAll = e.target.closest("[data-mark-all-present]");
      if (markAll) {
        markAll.closest("form").querySelectorAll('input[type=radio][value=PRESENT]').forEach(function (r) { r.checked = true; });
        return;
      }
      // 出欠フォーム: 会計金額を出席者で割り(100円単位で切り上げ)、出席者の受領金額欄に入力する
      var fillEven = e.target.closest("[data-fill-even]");
      if (fillEven) {
        var rows = Array.prototype.slice.call(fillEven.closest("form").querySelectorAll("[data-att-row]"))
          .filter(function (row) { return row.querySelector('input[type=radio][value=PRESENT]:checked'); });
        if (rows.length === 0) {
          alert("出席の人がいません。先に出欠を選んでください。");
          return;
        }
        var perPerson = Math.ceil(Number(fillEven.dataset.total) / rows.length / 100) * 100;
        rows.forEach(function (row) { row.querySelector("[data-amount]").value = perPerson; });
      }
    });

    document.addEventListener("keydown", function (e) {
      if (e.key === "Escape") setManageOpen(false);
    });
  })();
</script>
</body>
</html>

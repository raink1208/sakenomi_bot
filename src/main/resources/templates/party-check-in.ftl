<#include "_header.ftl">
<section class="check-in">
  <a class="back-link" href="/g/${guildId?c}/events/${event.id?c}#party-${party.id?c}">← 第${event.number}回 ${event.name} に戻る</a>
  <div class="page-head">
    <h1>出席登録</h1>
    <p class="page-sub">第${event.number}回 ${event.name} · ${event.date}</p>
  </div>

  <div class="tabs check-in-tabs">
    <#list parties as p>
      <a class="tab<#if p.id == party.id> active</#if>" href="/g/${guildId?c}/parties/${p.id?c}/check-in">${p.orderNo}軒目 ${p.shopName}</a>
    </#list>
  </div>

  <#if saved>
    <p class="success check-in-saved">出席を保存しました。</p>
  </#if>

  <#if people?size == 0>
    <p class="empty">参加者がいません。</p>
  <#else>
    <form method="post" action="/g/${guildId?c}/parties/${party.id?c}/check-in" id="check-in-form">
      <div class="check-in-panel">
        <div class="check-in-tally">
          <div>
            <div class="check-in-tally-label">出席</div>
            <div class="check-in-tally-main"><span data-tally="present">${presentCount}</span><small> / ${people?size}人</small></div>
          </div>
          <div class="check-in-breakdown">
            <span><i class="check-in-dot present"></i>出席 <b data-tally="present">${presentCount}</b></span>
            <span><i class="check-in-dot"></i>未登録 <b data-tally="unregistered">${people?size - presentCount}</b></span>
          </div>
        </div>
        <div class="check-in-meter" aria-hidden="true"><span id="check-in-meter-bar" style="width: ${(presentCount * 100 / people?size)?c}%"></span></div>
        <div class="check-in-tools">
          <label class="check-in-search" for="check-in-search">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></svg>
            <input type="search" id="check-in-search" placeholder="名前で絞り込み" autocomplete="off" aria-label="名前で絞り込み">
          </label>
          <div class="check-in-chips" role="group" aria-label="表示の絞り込み">
            <button type="button" class="check-in-chip" data-filter="all" aria-pressed="true">すべて ${people?size}</button>
            <button type="button" class="check-in-chip" data-filter="unregistered" aria-pressed="false">未登録 <span data-tally="unregistered">${people?size - presentCount}</span></button>
            <button type="button" class="check-in-chip" data-filter="present" aria-pressed="false">出席 <span data-tally="present">${presentCount}</span></button>
          </div>
        </div>
      </div>

      <p class="check-in-list-note">タップで出席 / 未登録を切り替え、最後に「保存」を押してください。幹事 → メンバー → ゲストの名前順です。</p>

      <div class="check-in-grid">
        <#list people as p>
          <#if p.present><input type="hidden" name="wasPresent" value="${p.id?c}"></#if>
          <label class="check-in-tile" data-name="${p.name}">
            <input type="checkbox" name="present" value="${p.id?c}"<#if p.present> checked</#if>>
            <span class="check-in-tile-body">
              <span class="check-in-name">${p.name}</span>
              <#if p.isOrganizer || p.isGuest>
                <span class="check-in-tags">
                  <#if p.isOrganizer><span class="check-in-tag organizer">幹事</span></#if>
                  <#if p.isGuest><span class="check-in-tag">ゲスト</span></#if>
                </span>
              </#if>
            </span>
          </label>
        </#list>
        <p class="empty check-in-no-hit" id="check-in-no-hit" hidden>該当する参加者はいません。</p>
      </div>

      <div class="check-in-bar">
        <div class="check-in-bar-inner">
          <span class="check-in-save-state" id="check-in-save-state">変更はありません</span>
          <div class="spacer"></div>
          <button type="button" class="button secondary" id="check-in-all">未登録 <span data-tally="unregistered">${people?size - presentCount}</span>人を出席にする</button>
          <button type="submit" class="button">保存</button>
        </div>
      </div>
    </form>

    <script>
      (function () {
        var form = document.getElementById("check-in-form");
        var tiles = Array.prototype.slice.call(form.querySelectorAll(".check-in-tile"));
        var search = document.getElementById("check-in-search");
        var filter = "all";

        // ひらがな・カタカナ、全角・半角、大文字・小文字、空白の違いを無視して検索する
        function normalize(text) {
          return text.normalize("NFKC").toLowerCase().replace(/\s/g, "")
            .replace(/[ァ-ヶ]/g, function (c) { return String.fromCharCode(c.charCodeAt(0) - 0x60); });
        }

        function update() {
          var total = tiles.length;
          var present = 0;
          var changed = 0;
          var query = normalize(search.value);
          var visible = 0;
          tiles.forEach(function (tile) {
            var box = tile.querySelector("input");
            if (box.checked) present++;
            var isChanged = box.checked !== box.defaultChecked;
            if (isChanged) changed++;
            tile.classList.toggle("is-changed", isChanged);
            var matches = (filter === "all" || (filter === "present") === box.checked) &&
              (!query || normalize(tile.dataset.name).indexOf(query) !== -1);
            tile.hidden = !matches;
            if (matches) visible++;
          });
          form.querySelectorAll('[data-tally="present"]').forEach(function (el) { el.textContent = present; });
          form.querySelectorAll('[data-tally="unregistered"]').forEach(function (el) { el.textContent = total - present; });
          document.getElementById("check-in-meter-bar").style.width = (present / total * 100) + "%";
          document.getElementById("check-in-no-hit").hidden = visible > 0;
          document.getElementById("check-in-all").disabled = present === total;
          var state = document.getElementById("check-in-save-state");
          state.textContent = changed > 0 ? "未保存の変更 " + changed + "件" : "変更はありません";
          state.classList.toggle("dirty", changed > 0);
          form.dataset.dirty = changed > 0 ? "1" : "0";
        }

        form.addEventListener("change", update);
        search.addEventListener("input", update);
        form.querySelectorAll(".check-in-chip").forEach(function (chip) {
          chip.addEventListener("click", function () {
            filter = chip.dataset.filter;
            form.querySelectorAll(".check-in-chip").forEach(function (c) { c.setAttribute("aria-pressed", c === chip ? "true" : "false"); });
            update();
          });
        });
        document.getElementById("check-in-all").addEventListener("click", function () {
          tiles.forEach(function (tile) { tile.querySelector("input").checked = true; });
          update();
        });
        form.addEventListener("submit", function () { form.dataset.dirty = "0"; });

        // 未保存の変更があるままページを離れようとしたら確認する。
        // hx-boost のリンク遷移は beforeunload が発火しないため、リンクのクリックをキャプチャして止める。
        // .topbar と main はナビゲーションの度に差し替わるので、リスナーが積み重なることはない
        function guardLeave(e) {
          if (form.dataset.dirty !== "1" || !e.target.closest("a[href]")) return;
          if (!confirm("保存していない変更があります。破棄して移動しますか?")) {
            e.preventDefault();
            e.stopPropagation();
          }
        }
        document.querySelector(".topbar").addEventListener("click", guardLeave, true);
        document.querySelector("main").addEventListener("click", guardLeave, true);
        window.onbeforeunload = function (e) {
          var f = document.getElementById("check-in-form");
          if (f && f.dataset.dirty === "1") {
            e.preventDefault();
            e.returnValue = "";
          }
        };

        update();
      })();
    </script>
  </#if>
</section>
<#include "_footer.ftl">
